package com.fittrack.app.domain.analysis

import com.fittrack.app.data.db.dao.ExerciseSetRow
import com.fittrack.app.data.entity.Exercise
import com.fittrack.app.data.entity.MuscleGroup
import com.fittrack.app.data.entity.TrackingMode
import com.fittrack.app.data.repository.ExerciseRepository
import com.fittrack.app.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

/** Weekly hard-set range a muscle group should land in. */
data class VolumeBand(val low: Int, val high: Int) {
    companion object {
        val DEFAULT = VolumeBand(10, 20)
        val SMALL = VolumeBand(6, 12)

        fun forGroup(group: MuscleGroup): VolumeBand = when (group) {
            MuscleGroup.ABS, MuscleGroup.FOREARMS, MuscleGroup.CALVES -> SMALL
            else -> DEFAULT
        }
    }
}

enum class VolumeStatus(val label: String) { BELOW("Below"), IN_RANGE("In range"), ABOVE("Above") }

/** One muscle group's sets for a week against its band. */
data class WeeklyVolumeRow(
    val muscleGroup: MuscleGroup,
    val sets: Int,
    val band: VolumeBand,
    /** Mean sets/week over the last 28 days. */
    val fourWeekAverage: Double,
) {
    val status: VolumeStatus
        get() = when {
            sets < band.low -> VolumeStatus.BELOW
            sets > band.high -> VolumeStatus.ABOVE
            else -> VolumeStatus.IN_RANGE
        }
}

data class WeeklyVolumeReport(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val rows: List<WeeklyVolumeRow>,
    val advice: List<DeloadAdvice>,
) {
    val groupsInRange: Int get() = rows.count { it.status == VolumeStatus.IN_RANGE }
}

data class DeloadAdvice(
    /** null for the global "take a lighter week" advice. */
    val muscleGroup: MuscleGroup?,
    val title: String,
    val reason: String,
    val action: String = "Next week: half the sets, keep ~90% of the weight.",
)

/**
 * Weekly hard-set counts per muscle group and the deload rules built on
 * them (spec §3). Weeks follow the device locale's first weekday.
 */
class VolumeAnalyzer(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
) {
    private val zone: ZoneId get() = ZoneId.systemDefault()
    private val firstDay: DayOfWeek get() = WeekFields.of(Locale.getDefault()).firstDayOfWeek

    private fun startOfWeek(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(firstDay))

    private fun LocalDate.toMillis(): Long = atStartOfDay(zone).toInstant().toEpochMilli()
    private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

    private val groups = MuscleGroup.entries.filter { it != MuscleGroup.CARDIO && it != MuscleGroup.FULL_BODY }

    /**
     * @param weekOffset 0 = the current week, 1 = last week.
     */
    suspend fun report(weekOffset: Int = 0): WeeklyVolumeReport {
        val today = LocalDate.now(zone)
        val thisWeekStart = startOfWeek(today)
        val weekStart = thisWeekStart.minusWeeks(weekOffset.toLong())
        val weekEnd = weekStart.plusDays(6)
        val exercises = exerciseRepository.getAllExercises().first().associateBy { it.id }
        // Eight weeks back covers the global deload rule and the 4-week averages.
        val since = thisWeekStart.minusWeeks(8).toMillis()
        val rows = workoutRepository.getNonWarmupSetRowsSince(since)

        val weekRows = rows.filter { it.workoutStartTime in weekStart.toMillis() until weekEnd.plusDays(1).toMillis() }
        val fourWeeksAgo = today.minusDays(28).toMillis()
        val recent = rows.filter { it.workoutStartTime >= fourWeeksAgo }

        val table = groups.map { group ->
            val sets = weekRows.count { exercises[it.exerciseId]?.muscleGroup == group }
            val avg = recent.count { exercises[it.exerciseId]?.muscleGroup == group } / 4.0
            WeeklyVolumeRow(group, sets, VolumeBand.forGroup(group), avg)
        }.sortedWith(compareByDescending<WeeklyVolumeRow> { it.sets }.thenBy { it.muscleGroup.displayName })

        return WeeklyVolumeReport(weekStart, weekEnd, table, deloadAdvice(rows, exercises, thisWeekStart))
    }

    /** Deload rules only — used by Home suggestions. */
    suspend fun deloadAdvice(): List<DeloadAdvice> {
        val exercises = exerciseRepository.getAllExercises().first().associateBy { it.id }
        val thisWeekStart = startOfWeek(LocalDate.now(zone))
        val rows = workoutRepository.getNonWarmupSetRowsSince(thisWeekStart.minusWeeks(8).toMillis())
        return deloadAdvice(rows, exercises, thisWeekStart)
    }

    private fun deloadAdvice(
        rows: List<ExerciseSetRow>,
        exercises: Map<Long, Exercise>,
        thisWeekStart: LocalDate,
    ): List<DeloadAdvice> {
        val out = mutableListOf<DeloadAdvice>()
        val now = System.currentTimeMillis()
        val day = 24L * 60 * 60 * 1000

        for (group in groups) {
            val groupRows = rows.filter { exercises[it.exerciseId]?.muscleGroup == group }
            if (groupRows.isEmpty()) continue

            // plateaued: ≥2 exercises (or all of them) with ≥3 sessions whose
            // best e1RM over the last 3 sessions moved < 1% vs the 3 before.
            val liftRows = groupRows.filter { exercises[it.exerciseId]?.trackingMode == TrackingMode.WEIGHT_REPS }
            val perExercise = liftRows.groupBy { it.exerciseId }.values.mapNotNull { exRows ->
                val sessions = exRows.groupBy { it.set.workoutExerciseId }.values
                    .map { s -> s.first().workoutStartTime to s.maxOf { epley(it.set.weightKg, it.set.reps) } }
                    .sortedBy { it.first }
                    .map { it.second }
                if (sessions.size < 3) return@mapNotNull null
                val recent = sessions.takeLast(3).max()
                val before = sessions.dropLast(3).takeLast(3).ifEmpty { listOf(sessions.takeLast(3).first()) }.max()
                before > 0 && (recent - before) / before < 0.01
            }
            val needed = minOf(2, perExercise.size)
            val plateaued = needed > 0 && perExercise.count { it } >= needed

            // rpeRising: last 14 days mean ≥ 8.5 and above the prior 14 days.
            val lastTwo = groupRows.filter { it.workoutStartTime >= now - 14 * day }.mapNotNull { it.set.rpe }
            val priorTwo = groupRows.filter { it.workoutStartTime in (now - 28 * day) until (now - 14 * day) }.mapNotNull { it.set.rpe }
            val lastMean = lastTwo.takeIf { it.isNotEmpty() }?.average()
            val priorMean = priorTwo.takeIf { it.isNotEmpty() }?.average()
            val rpeRising = lastMean != null && (
                (priorMean != null && lastMean >= 8.5 && lastMean > priorMean) ||
                    (priorMean == null && lastMean >= 9.0)
                )

            // overVolume: above the band top in each of the last 3 full weeks.
            val band = VolumeBand.forGroup(group)
            val overVolume = (1..3).all { back ->
                val ws = thisWeekStart.minusWeeks(back.toLong())
                val count = groupRows.count { it.workoutStartTime in ws.toMillis() until ws.plusWeeks(1).toMillis() }
                count > band.high
            }

            when {
                plateaued && rpeRising -> out += DeloadAdvice(
                    muscleGroup = group,
                    title = "Deload ${group.displayName}",
                    reason = "Strength flat for 3 sessions and effort rising (RPE ${"%.1f".format(lastMean)})",
                )
                overVolume -> out += DeloadAdvice(
                    muscleGroup = group,
                    title = "Deload ${group.displayName}",
                    reason = "3 weeks above ${band.high} sets",
                )
            }
        }

        // Global: six or more consecutive loaded weeks without a lighter one.
        val weekly = (1..8).map { back ->
            val ws = thisWeekStart.minusWeeks(back.toLong())
            rows.count { it.workoutStartTime in ws.toMillis() until ws.plusWeeks(1).toMillis() }
        } // index 0 = last complete week
        val mean = weekly.average()
        if (mean > 0) {
            val threshold = mean * 0.6
            var loaded = 0
            for (count in weekly) {
                if (count >= threshold) loaded++ else break
            }
            if (loaded >= 6) {
                out += DeloadAdvice(
                    muscleGroup = null,
                    title = "Time for a deload week",
                    reason = "$loaded weeks of full training without a lighter week",
                )
            }
        }
        return out
    }

    private fun epley(weight: Double, reps: Int): Double {
        if (reps <= 0 || weight <= 0) return 0.0
        if (reps == 1) return weight
        return weight * (1 + reps / 30.0)
    }
}
