package com.wylew.activitystats.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * @param weeks consecutive weeks with at least one matching activity. This week counts if
 *   it's already active; an empty current week doesn't break the streak until it ends.
 * @param recent the last [Streaks.HISTORY] weeks, oldest first, ending with this week.
 */
data class Streak(val weeks: Int, val recent: List<Boolean>, val thisWeekActive: Boolean)

object Streaks {
    const val HISTORY = 12

    fun weekly(
        all: List<Activity>,
        filter: ActivityFilter,
        today: LocalDate,
        zone: ZoneId,
        weekStart: DayOfWeek = DayOfWeek.MONDAY,
    ): Streak {
        val adjust = TemporalAdjusters.previousOrSame(weekStart)
        val thisWeek = today.with(adjust)
        val active = all.asSequence()
            .filter { filter.matches(it.kind) }
            .map { it.localDate(zone).with(adjust) }
            .toSet()
        val recent = (HISTORY - 1 downTo 0).map { thisWeek.minusWeeks(it.toLong()) in active }
        var week = if (thisWeek in active) thisWeek else thisWeek.minusWeeks(1)
        var count = 0
        while (week in active) {
            count++
            week = week.minusWeeks(1)
        }
        return Streak(count, recent, thisWeek in active)
    }
}
