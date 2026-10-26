package com.fittrack.app.domain.analysis

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Weekly training streak: consecutive ISO weeks with at least one completed workout. */
data class Streak(
    /** Weeks in a row ending at this week (or last week if this week is still empty). */
    val currentWeeks: Int,
    val longestWeeks: Int,
    val workoutsThisWeek: Int,
)

object StreakCalculator {
    /**
     * @param completedStartTimes start times (epoch ms) of completed workouts, any order.
     *
     * The current week counts as part of the streak once it has a workout; an
     * empty current week doesn't break the streak until the week is over, so
     * a Monday-morning Home visit still shows last week's run.
     */
    fun compute(
        completedStartTimes: List<Long>,
        zone: ZoneId = ZoneId.systemDefault(),
        today: LocalDate = LocalDate.now(zone),
    ): Streak {
        if (completedStartTimes.isEmpty()) return Streak(0, 0, 0)
        val weekStarts = completedStartTimes
            .map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate().with(DayOfWeek.MONDAY) }
            .toSortedSet()
        val thisWeek = today.with(DayOfWeek.MONDAY)
        val workoutsThisWeek = completedStartTimes.count {
            Instant.ofEpochMilli(it).atZone(zone).toLocalDate().with(DayOfWeek.MONDAY) == thisWeek
        }

        // Current streak: walk back from this week (or last week if this one is empty).
        var cursor = if (thisWeek in weekStarts) thisWeek else thisWeek.minusWeeks(1)
        var current = 0
        while (cursor in weekStarts) {
            current++
            cursor = cursor.minusWeeks(1)
        }

        // Longest: scan the sorted distinct weeks for the longest consecutive run.
        var longest = 0
        var run = 0
        var prev: LocalDate? = null
        for (w in weekStarts) {
            run = if (prev != null && ChronoUnit.WEEKS.between(prev, w) == 1L) run + 1 else 1
            if (run > longest) longest = run
            prev = w
        }
        return Streak(currentWeeks = current, longestWeeks = longest, workoutsThisWeek = workoutsThisWeek)
    }
}
