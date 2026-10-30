package com.wylew.activitystats.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class StreaksTest {
    private val zone = ZoneId.of("America/Los_Angeles")
    private val today = LocalDate.of(2026, 10, 29) // Thursday

    private fun run(date: LocalDate, kind: ActivityKind = ActivityKind.RUN) = Activity(
        id = date.toString() + kind,
        source = Source.HEALTH_CONNECT,
        name = "Run",
        kind = kind,
        sportType = "Run",
        startEpochSec = date.atTime(7, 0).atZone(zone).toEpochSecond(),
        distanceM = 5000.0,
        movingSec = 1800,
        elevationM = 0.0,
        origin = "Garmin",
    )

    @Test
    fun countsBackFromThisWeek() {
        val acts = listOf(today, today.minusWeeks(1), today.minusWeeks(2), today.minusWeeks(4)).map { run(it) }
        val s = Streaks.weekly(acts, ActivityFilter.RUN, today, zone)
        assertEquals(3, s.weeks)
        assertTrue(s.thisWeekActive)
        assertEquals(12, s.recent.size)
        assertEquals(listOf(true, false, true, true, true), s.recent.takeLast(5))
    }

    @Test
    fun emptyCurrentWeekKeepsStreakFromLastWeek() {
        val acts = listOf(today.minusWeeks(1), today.minusWeeks(2)).map { run(it) }
        val s = Streaks.weekly(acts, ActivityFilter.RUN, today, zone)
        assertEquals(2, s.weeks)
        assertFalse(s.thisWeekActive)
    }

    @Test
    fun filterApplies() {
        val acts = listOf(run(today, ActivityKind.RIDE), run(today.minusWeeks(1)))
        assertEquals(1, Streaks.weekly(acts, ActivityFilter.RUN, today, zone).weeks)
        assertEquals(2, Streaks.weekly(acts, ActivityFilter.ALL, today, zone).weeks)
    }

    @Test
    fun goalsScaleByPeriod() {
        val g = Goals()
        assertEquals(g.runWeeklyM / 7, g.forPeriod(ActivityFilter.RUN, RollupPeriod.DAILY), 0.01)
        assertEquals(g.rideWeeklyM, g.forPeriod(ActivityFilter.RIDE, RollupPeriod.WEEKLY), 0.01)
    }

    @Test
    fun sourceChoiceFiltersOrigins() {
        val garmin = run(today).copy(id = "g", origin = "Garmin")
        val strava = run(today).copy(id = "s", origin = "Strava", startEpochSec = garmin.startEpochSec + 30)
        val both = ActivityView.apply(listOf(garmin, strava), SourcePrefs(SourceChoice.BOTH, PreferOrigin.STRAVA))
        assertEquals(listOf("s"), both.map { it.id })
        assertEquals(listOf("g"), ActivityView.apply(listOf(garmin, strava), SourcePrefs(SourceChoice.GARMIN)).map { it.id })
        assertEquals(1, ActivityView.mergedCount(listOf(garmin, strava), SourcePrefs()))
    }
}
