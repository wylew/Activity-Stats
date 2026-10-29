package com.wylew.activitystats.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class RollupCalculatorTest {

    private val zone = ZoneId.of("America/Los_Angeles")
    private val today = LocalDate.of(2026, 10, 29) // Thursday

    private fun act(
        id: String,
        kind: ActivityKind,
        at: LocalDateTime,
        km: Double,
        minutes: Int,
        hr: Double? = null,
    ) = Activity(
        id = id,
        source = Source.STRAVA,
        name = id,
        kind = kind,
        sportType = kind.name,
        startEpochSec = at.atZone(zone).toEpochSecond(),
        distanceM = km * 1000,
        movingSec = minutes * 60,
        elevationM = 10.0,
        avgHr = hr,
    )

    private val data = listOf(
        act("mon-run", ActivityKind.RUN, LocalDateTime.of(2026, 10, 26, 6, 30), 6.8, 40, 140.0),
        act("tue-run", ActivityKind.RUN, LocalDateTime.of(2026, 10, 27, 6, 30), 8.4, 43, 160.0),
        act("wed-ride", ActivityKind.RIDE, LocalDateTime.of(2026, 10, 28, 17, 0), 38.8, 80),
        act("thu-run", ActivityKind.RUN, LocalDateTime.of(2026, 10, 29, 6, 42), 6.1, 34),
        act("thu-ride", ActivityKind.RIDE, LocalDateTime.of(2026, 10, 29, 17, 30), 28.0, 64),
        // Sunday of the previous week: must not count toward this week.
        act("prev-sun-run", ActivityKind.RUN, LocalDateTime.of(2026, 10, 25, 23, 30), 10.0, 60),
    )

    @Test
    fun weeklyRunsStartMonday() {
        val r = RollupCalculator.compute(data, ActivityFilter.RUN, RollupPeriod.WEEKLY, today, zone)
        assertEquals(LocalDate.of(2026, 10, 26), r.start)
        assertEquals(3, r.count)
        assertEquals(21_300.0, r.distanceM, 0.01)
        assertEquals(7, r.buckets.size)
        assertTrue(r.buckets[3].isCurrent)
        assertTrue(r.buckets[4].isFuture)
        assertEquals(0, r.buckets[2].count)
    }

    @Test
    fun dailyAllIncludesRunAndRide() {
        val r = RollupCalculator.compute(data, ActivityFilter.ALL, RollupPeriod.DAILY, today, zone)
        assertEquals(2, r.count)
        assertEquals("thu-ride", r.items.first().id) // newest first
        assertTrue(r.buckets.isEmpty())
    }

    @Test
    fun dailyRideOnly() {
        val r = RollupCalculator.compute(data, ActivityFilter.RIDE, RollupPeriod.DAILY, today, zone)
        assertEquals(1, r.count)
        assertEquals(28_000.0, r.distanceM, 0.01)
        assertNull(r.avgHr)
    }

    @Test
    fun heartRateIsTimeWeighted() {
        val r = RollupCalculator.compute(data, ActivityFilter.RUN, RollupPeriod.WEEKLY, today, zone)
        // (140*40 + 160*43) / 83
        assertEquals((140.0 * 40 + 160.0 * 43) / 83, r.avgHr!!, 0.001)
    }

    @Test
    fun previousWeekToDateComparesSameDays() {
        val prev = RollupCalculator.previousToDate(data, ActivityFilter.RUN, RollupPeriod.WEEKLY, today, zone)
        assertEquals(LocalDate.of(2026, 10, 19), prev.start)
        assertEquals(LocalDate.of(2026, 10, 23), prev.end) // Mon..Thu
        assertEquals(0, prev.count)
    }

    @Test
    fun yearlyHasTwelveMonths() {
        val r = RollupCalculator.compute(data, ActivityFilter.ALL, RollupPeriod.YEARLY, today, zone)
        assertEquals(12, r.buckets.size)
        assertTrue(r.buckets[9].isCurrent)
        assertEquals(6, r.count)
    }

    @Test
    fun stravaSportTypes() {
        assertEquals(ActivityKind.RUN, ActivityKind.fromStrava("TrailRun"))
        assertEquals(ActivityKind.RIDE, ActivityKind.fromStrava("GravelRide"))
        assertEquals(ActivityKind.OTHER, ActivityKind.fromStrava("Yoga"))
    }
}
