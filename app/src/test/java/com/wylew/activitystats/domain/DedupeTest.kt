package com.wylew.activitystats.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class DedupeTest {

    private fun a(
        id: String,
        origin: String?,
        start: Long,
        meters: Double,
        kind: ActivityKind = ActivityKind.RUN,
        name: String = "Run",
        hr: Double? = null,
    ) = Activity(
        id = id,
        source = if (origin == "Strava" && id.startsWith("strava")) Source.STRAVA else Source.HEALTH_CONNECT,
        name = name,
        kind = kind,
        sportType = "Run",
        startEpochSec = start,
        distanceM = meters,
        movingSec = 2000,
        elevationM = 0.0,
        avgHr = hr,
        origin = origin,
    )

    @Test
    fun garminAndStravaCopiesCollapseToGarmin() {
        val garmin = a("hc:g1", "Garmin", 1_000, 6_100.0, hr = 148.0)
        val strava = a("hc:s1", "Strava", 1_045, 6_080.0, name = "Easy Thursday shakeout")
        val out = Dedupe.apply(listOf(strava, garmin))
        assertEquals(1, out.size)
        assertEquals("hc:g1", out[0].id)
        assertEquals(6_100.0, out[0].distanceM, 0.0)
        assertEquals("Easy Thursday shakeout", out[0].name) // keeps the Strava title
        assertEquals(148.0, out[0].avgHr!!, 0.0)
    }

    @Test
    fun threeCopiesIncludingStravaApi() {
        val out = Dedupe.apply(
            listOf(
                a("hc:g1", "Garmin", 1_000, 6_100.0),
                a("hc:s1", "Strava", 1_030, 6_090.0),
                a("strava:99", "Strava", 1_030, 6_090.0),
            )
        )
        assertEquals(1, out.size)
        assertEquals("hc:g1", out[0].id)
    }

    @Test
    fun differentDistancesStaySeparate() {
        // Warm-up and main run started close together.
        val out = Dedupe.apply(listOf(a("1", "Garmin", 1_000, 1_600.0), a("2", "Garmin", 1_100, 8_000.0)))
        assertEquals(2, out.size)
    }

    @Test
    fun differentSportsStaySeparate() {
        val out = Dedupe.apply(
            listOf(a("1", "Garmin", 1_000, 5_000.0), a("2", "Strava", 1_000, 5_000.0, kind = ActivityKind.RIDE))
        )
        assertEquals(2, out.size)
    }

    @Test
    fun farApartInTimeStaySeparate() {
        val out = Dedupe.apply(listOf(a("1", "Garmin", 1_000, 5_000.0), a("2", "Strava", 1_000 + 600, 5_000.0)))
        assertEquals(2, out.size)
    }
}
