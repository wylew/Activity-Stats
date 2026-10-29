package com.wylew.activitystats.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

enum class RollupPeriod { DAILY, WEEKLY, MONTHLY, YEARLY }

data class Bucket(
    val label: String,
    val distanceM: Double,
    val count: Int,
    val isCurrent: Boolean,
    val isFuture: Boolean,
)

data class Rollup(
    val period: RollupPeriod,
    val filter: ActivityFilter,
    val start: LocalDate,
    /** Exclusive. */
    val end: LocalDate,
    val count: Int,
    val distanceM: Double,
    val movingSec: Long,
    val elevationM: Double,
    /** Moving-time-weighted average across activities that recorded heart rate. */
    val avgHr: Double?,
    /** Bars for the chart. Empty for DAILY, which shows [items] instead. */
    val buckets: List<Bucket>,
    /** Matching activities, newest first. */
    val items: List<Activity>,
)

object RollupCalculator {

    fun range(period: RollupPeriod, today: LocalDate, weekStart: DayOfWeek = DayOfWeek.MONDAY): Pair<LocalDate, LocalDate> =
        when (period) {
            RollupPeriod.DAILY -> today to today.plusDays(1)
            RollupPeriod.WEEKLY -> {
                val s = today.with(TemporalAdjusters.previousOrSame(weekStart))
                s to s.plusDays(7)
            }
            RollupPeriod.MONTHLY -> {
                val s = today.withDayOfMonth(1)
                s to s.plusMonths(1)
            }
            RollupPeriod.YEARLY -> {
                val s = today.withDayOfYear(1)
                s to s.plusYears(1)
            }
        }

    fun compute(
        all: List<Activity>,
        filter: ActivityFilter,
        period: RollupPeriod,
        today: LocalDate,
        zone: ZoneId,
        weekStart: DayOfWeek = DayOfWeek.MONDAY,
    ): Rollup {
        val (start, end) = range(period, today, weekStart)
        return build(all, filter, period, start, end, today, zone)
    }

    /**
     * The previous period up to the same point, for "vs last week" deltas.
     * On a Thursday this compares Mon–Thu against last Mon–Thu.
     */
    fun previousToDate(
        all: List<Activity>,
        filter: ActivityFilter,
        period: RollupPeriod,
        today: LocalDate,
        zone: ZoneId,
        weekStart: DayOfWeek = DayOfWeek.MONDAY,
    ): Rollup {
        val (start, _) = range(period, today, weekStart)
        val prevStart = when (period) {
            RollupPeriod.DAILY -> start.minusDays(1)
            RollupPeriod.WEEKLY -> start.minusWeeks(1)
            RollupPeriod.MONTHLY -> start.minusMonths(1)
            RollupPeriod.YEARLY -> start.minusYears(1)
        }
        val elapsedDays = java.time.temporal.ChronoUnit.DAYS.between(start, today) + 1
        val prevEnd = prevStart.plusDays(elapsedDays)
        val prevToday = prevEnd.minusDays(1)
        return build(all, filter, period, prevStart, prevEnd, prevToday, zone)
    }

    private fun build(
        all: List<Activity>,
        filter: ActivityFilter,
        period: RollupPeriod,
        start: LocalDate,
        end: LocalDate,
        today: LocalDate,
        zone: ZoneId,
    ): Rollup {
        val items = all
            .filter { filter.matches(it.kind) }
            .filter {
                val d = it.localDate(zone)
                !d.isBefore(start) && d.isBefore(end)
            }
            .sortedByDescending { it.startEpochSec }

        val hrItems = items.filter { it.avgHr != null && it.movingSec > 0 }
        val hrWeight = hrItems.sumOf { it.movingSec.toLong() }
        val avgHr = if (hrWeight > 0) hrItems.sumOf { it.avgHr!! * it.movingSec } / hrWeight else null

        return Rollup(
            period = period,
            filter = filter,
            start = start,
            end = end,
            count = items.size,
            distanceM = items.sumOf { it.distanceM },
            movingSec = items.sumOf { it.movingSec.toLong() },
            elevationM = items.sumOf { it.elevationM },
            avgHr = avgHr,
            buckets = buckets(items, period, start, end, today, zone),
            items = items,
        )
    }

    private fun buckets(
        items: List<Activity>,
        period: RollupPeriod,
        start: LocalDate,
        end: LocalDate,
        today: LocalDate,
        zone: ZoneId,
    ): List<Bucket> {
        val locale = Locale.getDefault()
        val ranges: List<Triple<String, LocalDate, LocalDate>> = when (period) {
            RollupPeriod.DAILY -> emptyList()
            RollupPeriod.WEEKLY -> (0 until 7).map {
                val d = start.plusDays(it.toLong())
                Triple(d.dayOfWeek.getDisplayName(TextStyle.NARROW, locale), d, d.plusDays(1))
            }
            RollupPeriod.MONTHLY -> generateSequence(start) { it.plusDays(7) }
                .takeWhile { it.isBefore(end) }
                .map { s -> Triple(s.dayOfMonth.toString(), s, minOf(s.plusDays(7), end)) }
                .toList()
            RollupPeriod.YEARLY -> (0 until 12).map {
                val s = start.plusMonths(it.toLong())
                Triple(s.month.getDisplayName(TextStyle.NARROW, locale), s, s.plusMonths(1))
            }
        }
        return ranges.map { (label, s, e) ->
            val inBucket = items.filter {
                val d = it.localDate(zone)
                !d.isBefore(s) && d.isBefore(e)
            }
            Bucket(
                label = label,
                distanceM = inBucket.sumOf { it.distanceM },
                count = inBucket.size,
                isCurrent = !today.isBefore(s) && today.isBefore(e),
                isFuture = s.isAfter(today),
            )
        }
    }
}

fun Activity.localDate(zone: ZoneId): LocalDate =
    Instant.ofEpochSecond(startEpochSec).atZone(zone).toLocalDate()

/** Hour of day as a fraction, 6.7 for 6:42 AM. */
fun Activity.hourOfDay(zone: ZoneId): Float {
    val t = Instant.ofEpochSecond(startEpochSec).atZone(zone).toLocalTime()
    return t.hour + t.minute / 60f
}
