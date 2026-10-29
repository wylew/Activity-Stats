package com.wylew.activitystats.data.strava

import com.wylew.activitystats.data.HttpException
import com.wylew.activitystats.data.fetch
import com.wylew.activitystats.domain.Activity
import com.wylew.activitystats.domain.ActivityKind
import com.wylew.activitystats.domain.Source
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Instant

class NotConnectedException : Exception("Strava isn't connected")

/** The one Strava endpoint we need: GET /athlete/activities. */
class StravaApi(
    private val http: OkHttpClient,
    private val json: Json,
    private val auth: StravaAuth,
) {
    suspend fun athleteActivities(afterEpochSec: Long, page: Int, perPage: Int = PER_PAGE): List<StravaActivityDto> {
        val url = "https://www.strava.com/api/v3/athlete/activities".toHttpUrl().newBuilder()
            .addQueryParameter("after", afterEpochSec.toString())
            .addQueryParameter("page", page.toString())
            .addQueryParameter("per_page", perPage.toString())
            .build()
        val body = authorized { token ->
            http.fetch(Request.Builder().url(url).header("Authorization", "Bearer $token").build())
        }
        return json.decodeFromString(ListSerializer(StravaActivityDto.serializer()), body)
    }

    /** Runs [block] with a valid token; on a 401, refreshes once and retries. */
    private suspend fun <T> authorized(block: suspend (String) -> T): T {
        val token = auth.validAccessToken() ?: throw NotConnectedException()
        return try {
            block(token)
        } catch (e: HttpException) {
            if (e.code != 401) throw e
            val fresh = auth.forceRefresh() ?: throw NotConnectedException()
            block(fresh)
        }
    }

    companion object {
        const val PER_PAGE = 200
    }
}

@Serializable
data class StravaActivityDto(
    val id: Long,
    val name: String = "",
    @SerialName("sport_type") val sportType: String? = null,
    val type: String? = null,
    @SerialName("start_date") val startDate: String,
    val distance: Double = 0.0,
    @SerialName("moving_time") val movingTime: Int = 0,
    @SerialName("total_elevation_gain") val totalElevationGain: Double = 0.0,
    @SerialName("average_heartrate") val averageHeartrate: Double? = null,
) {
    fun toDomain(): Activity {
        val sport = sportType ?: type ?: "Workout"
        return Activity(
            id = "strava:$id",
            source = Source.STRAVA,
            name = name,
            kind = ActivityKind.fromStrava(sport),
            sportType = sport,
            startEpochSec = Instant.parse(startDate).epochSecond,
            distanceM = distance,
            movingSec = movingTime,
            elevationM = totalElevationGain,
            avgHr = averageHeartrate,
            origin = "Strava",
        )
    }
}
