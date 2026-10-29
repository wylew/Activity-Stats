package com.wylew.activitystats.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class HttpException(val code: Int, message: String) : Exception(message)

/** Strava returned 429. [retryAfterSec] is a best guess at the next 15-minute window. */
class RateLimitedException(val retryAfterSec: Long) : Exception("Strava rate limit reached")

/** Runs the call off the main thread and returns the body, or throws [HttpException]. */
internal suspend fun OkHttpClient.fetch(request: Request): String = withContext(Dispatchers.IO) {
    newCall(request).execute().use { resp ->
        val body = resp.body?.string().orEmpty()
        when {
            resp.isSuccessful -> body
            resp.code == 429 -> throw RateLimitedException(retryAfterSec = 15 * 60)
            else -> throw HttpException(resp.code, "HTTP ${resp.code}: ${body.take(200)}")
        }
    }
}
