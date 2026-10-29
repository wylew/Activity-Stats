package com.wylew.activitystats.data.strava

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.wylew.activitystats.BuildConfig
import com.wylew.activitystats.data.HttpException
import com.wylew.activitystats.data.SettingsStore
import com.wylew.activitystats.data.StravaTokens
import com.wylew.activitystats.data.fetch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Strava OAuth 2.0 for a single-athlete personal app.
 *
 * Setup (strava.com/settings/api): set "Authorization Callback Domain" to `localhost`,
 * then put the Client ID and Secret in local.properties (see README).
 *
 * The client secret ships inside the APK, which is acceptable for a build only you install.
 * For a public release, move the token exchange to a small server.
 */
class StravaAuth(
    private val settings: SettingsStore,
    private val http: OkHttpClient,
    private val json: Json,
) {
    private val refreshLock = Mutex()

    val isConfigured: Boolean get() = BuildConfig.STRAVA_CLIENT_ID.isNotBlank() && BuildConfig.STRAVA_CLIENT_SECRET.isNotBlank()

    /** Opens the Strava app's consent screen if installed, otherwise a Custom Tab. */
    fun launchAuthorize(context: Context) {
        val appUri = authorizeUri("strava://oauth/mobile/authorize")
        val webUri = authorizeUri("https://www.strava.com/oauth/mobile/authorize")
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, appUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            val tab = CustomTabsIntent.Builder().build()
            tab.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            tab.launchUrl(context, webUri)
        }
    }

    private fun authorizeUri(base: String): Uri = Uri.parse(base).buildUpon()
        .appendQueryParameter("client_id", BuildConfig.STRAVA_CLIENT_ID)
        .appendQueryParameter("redirect_uri", REDIRECT_URI)
        .appendQueryParameter("response_type", "code")
        .appendQueryParameter("approval_prompt", "auto")
        .appendQueryParameter("scope", SCOPES)
        .build()

    fun isRedirect(uri: Uri?): Boolean =
        uri != null && uri.scheme == "activitystats" && uri.host == "localhost" && uri.path == "/strava-callback"

    /** Handles activitystats://localhost/strava-callback?code=…&scope=… */
    suspend fun handleRedirect(uri: Uri): Result<Unit> = runCatching {
        uri.getQueryParameter("error")?.let { error("Strava sign-in was cancelled ($it)") }
        val code = uri.getQueryParameter("code") ?: error("Strava did not return a code")
        val granted = uri.getQueryParameter("scope").orEmpty()
        if (!granted.contains("activity:read")) {
            error("Activity access wasn't granted. Allow \"View data about your activities\" and try again.")
        }
        val token = tokenRequest(
            FormBody.Builder()
                .add("client_id", BuildConfig.STRAVA_CLIENT_ID)
                .add("client_secret", BuildConfig.STRAVA_CLIENT_SECRET)
                .add("code", code)
                .add("grant_type", "authorization_code")
                .build()
        )
        settings.saveTokens(StravaTokens(token.accessToken, token.refreshToken, token.expiresAt))
        settings.saveAthlete(token.athlete?.displayName)
    }

    /** A token good for at least five more minutes, refreshing if needed. Null when disconnected. */
    suspend fun validAccessToken(): String? {
        refreshLock.withLock {
            val t = settings.tokens.first() ?: return null
            return if (t.expiresAtSec - 300 > nowSec()) t.access else refresh(t).access
        }
    }

    /** After a 401: refresh even if the stored expiry looks fine. */
    suspend fun forceRefresh(): String? {
        refreshLock.withLock {
            val t = settings.tokens.first() ?: return null
            return refresh(t).access
        }
    }

    private suspend fun refresh(t: StravaTokens): StravaTokens {
        val token = try {
            tokenRequest(
                FormBody.Builder()
                    .add("client_id", BuildConfig.STRAVA_CLIENT_ID)
                    .add("client_secret", BuildConfig.STRAVA_CLIENT_SECRET)
                    .add("grant_type", "refresh_token")
                    .add("refresh_token", t.refresh)
                    .build()
            )
        } catch (e: HttpException) {
            // 400/401 means the refresh token was revoked (e.g. access removed on strava.com).
            if (e.code == 400 || e.code == 401) settings.clearAuth()
            throw e
        }
        // Strava may rotate the refresh token; always persist the latest one.
        return StravaTokens(token.accessToken, token.refreshToken, token.expiresAt).also { settings.saveTokens(it) }
    }

    suspend fun disconnect() {
        val access = settings.tokens.first()?.access
        if (access != null) {
            runCatching {
                http.fetch(
                    Request.Builder()
                        .url("https://www.strava.com/oauth/deauthorize")
                        .post(FormBody.Builder().add("access_token", access).build())
                        .build()
                )
            }
        }
        settings.clearAuth()
    }

    private suspend fun tokenRequest(form: FormBody): TokenResponse {
        val body = http.fetch(Request.Builder().url("https://www.strava.com/oauth/token").post(form).build())
        return json.decodeFromString(TokenResponse.serializer(), body)
    }

    private fun nowSec() = System.currentTimeMillis() / 1000

    companion object {
        const val REDIRECT_URI = "activitystats://localhost/strava-callback"
        const val SCOPES = "read,activity:read_all"
    }
}

@Serializable
internal data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_at") val expiresAt: Long,
    val athlete: AthleteDto? = null,
)

@Serializable
internal data class AthleteDto(
    val id: Long,
    val firstname: String? = null,
    val lastname: String? = null,
) {
    val displayName: String? get() = listOfNotNull(firstname, lastname).joinToString(" ").ifBlank { null }
}
