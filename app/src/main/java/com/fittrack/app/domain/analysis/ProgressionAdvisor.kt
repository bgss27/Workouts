package com.fittrack.app.domain.analysis

import com.fittrack.app.data.WeightUnits
import com.fittrack.app.data.entity.Equipment
import com.fittrack.app.data.entity.Exercise
import com.fittrack.app.data.entity.MuscleGroup
import com.fittrack.app.data.entity.TrackingMode
import com.fittrack.app.data.entity.WorkoutSet
import kotlin.math.roundToInt

/**
 * "What should I do today?" for one weight × reps exercise, from its last
 * few sessions. Pure function of the inputs; mirrors the iOS advisor so both
 * apps give the same target. Spec: tier2-spec §2.
 */
object ProgressionAdvisor {
    enum class Kind(val glyph: String) {
        INCREASE_WEIGHT("↑"), ADD_REPS("+"), HOLD("="), REDUCE_WEIGHT("↓")
    }

    data class RepTarget(val lo: Int, val hi: Int) {
        companion object {
            val DEFAULT = RepTarget(8, 12)

            /** "8-12" / "8–12" / "5" → a target; anything else → 8–12. */
            fun parse(text: String?): RepTarget {
                val t = text?.trim()?.replace('–', '-')?.replace('—', '-') ?: return DEFAULT
                val range = Regex("""^(\d{1,3})\s*-\s*(\d{1,3})$""").find(t)
                if (range != null) {
                    val lo = range.groupValues[1].toInt()
                    val hi = range.groupValues[2].toInt()
                    return if (lo in 1..hi) RepTarget(lo, hi) else DEFAULT
                }
                val single = t.toIntOrNull()
                return if (single != null && single > 0) RepTarget(single, single) else DEFAULT
            }
        }
    }

    data class Suggestion(
        val kind: Kind,
        /** 0 for bodyweight progressions. */
        val weightKg: Double,
        val reps: Int,
        val previousWeightKg: Double,
        val previousMinReps: Int,
        val reason: String,
    ) {
        /** "Target 62.5 kg × 8 ↑" — weight omitted when 0. */
        fun label(weightUnit: String): String {
            val w = if (weightKg > 0) WeightUnits.format(weightKg, weightUnit) + " × " else ""
            return "Target $w$reps ${kind.glyph}"
        }
    }

    /**
     * @param sessionsNewestFirst up to three completed sessions, newest first,
     *   each being that session's sets (warm-ups included; they are filtered here).
     */
    fun suggest(
        exercise: Exercise,
        sessionsNewestFirst: List<List<WorkoutSet>>,
        repTarget: RepTarget,
        weightUnit: String,
    ): Suggestion? {
        if (exercise.trackingMode != TrackingMode.WEIGHT_REPS) return null
        val last = sessionsNewestFirst.firstOrNull()?.let(::prSets) ?: return null
        if (last.isEmpty()) return null

        val weight = dominantWeight(last)
        val top = last.filter { it.weightKg == weight }
        val minReps = top.minOf { it.reps }
        val avgRpe = top.mapNotNull { it.rpe }.takeIf { it.isNotEmpty() }?.average()

        val small = if (weightUnit == WeightUnits.LBS) 5.0 else 2.5
        val heavyGroup = exercise.muscleGroup in setOf(MuscleGroup.LEGS, MuscleGroup.GLUTES, MuscleGroup.BACK)
        val heavyEquipment = exercise.equipment == Equipment.BARBELL || exercise.equipment == Equipment.MACHINE
        val increment = if (heavyGroup && heavyEquipment) small * 2 else small
        val bodyweight = weight <= 0.0

        val allAtTop = top.all { it.reps >= repTarget.hi }
        val allAtBottom = top.all { it.reps >= repTarget.lo }
        val veryHard = avgRpe != null && avgRpe >= 9.5

        // (d) back-off: hold conditions in both of the last two sessions.
        val previous = sessionsNewestFirst.getOrNull(1)?.let(::prSets)
        if (previous != null && previous.isNotEmpty() && holdTriggered(top, repTarget, avgRpe)) {
            val prevWeight = dominantWeight(previous)
            val prevTop = previous.filter { it.weightKg == prevWeight }
            val prevRpe = prevTop.mapNotNull { it.rpe }.takeIf { it.isNotEmpty() }?.average()
            val sameWeight = prevWeight == weight
            val prevHold = holdTriggered(prevTop, repTarget, prevRpe)
            val bothVeryHard = veryHard && prevRpe != null && prevRpe >= 9.5
            if ((sameWeight && prevHold) || bothVeryHard) {
                val reduced = if (bodyweight) 0.0 else roundToIncrement(weight * 0.9, small, weightUnit)
                return Suggestion(
                    kind = Kind.REDUCE_WEIGHT,
                    weightKg = reduced,
                    reps = repTarget.lo,
                    previousWeightKg = weight,
                    previousMinReps = minReps,
                    reason = "Missed the range twice; drop 10% and rebuild",
                )
            }
        }

        return when {
            allAtTop && (avgRpe == null || avgRpe <= 8.5) -> {
                if (bodyweight) Suggestion(
                    kind = Kind.ADD_REPS, weightKg = 0.0, reps = repTarget.hi + 1,
                    previousWeightKg = weight, previousMinReps = minReps, reason = "Add a rep",
                ) else Suggestion(
                    kind = Kind.INCREASE_WEIGHT,
                    weightKg = roundToIncrement(weight + toKg(increment, weightUnit), small, weightUnit),
                    reps = repTarget.lo,
                    previousWeightKg = weight,
                    previousMinReps = minReps,
                    reason = "Hit ${repTarget.hi} reps on every set last time",
                )
            }
            allAtBottom && !veryHard && !allAtTop -> Suggestion(
                kind = Kind.ADD_REPS,
                weightKg = weight,
                reps = minOf(repTarget.hi, minReps + 1),
                previousWeightKg = weight,
                previousMinReps = minReps,
                reason = "Same weight, one more rep",
            )
            allAtTop -> Suggestion( // at the top of the range but RPE too high to add load
                kind = Kind.HOLD, weightKg = weight, reps = repTarget.lo,
                previousWeightKg = weight, previousMinReps = minReps,
                reason = "Effort was very high (RPE ${"%.1f".format(avgRpe)})",
            )
            else -> Suggestion(
                kind = Kind.HOLD,
                weightKg = weight,
                reps = repTarget.lo,
                previousWeightKg = weight,
                previousMinReps = minReps,
                reason = if (veryHard) "Effort was very high (RPE ${"%.1f".format(avgRpe)})"
                else "Repeat last time's weight",
            )
        }
    }

    /** Rule (c): any set under the bottom of the range, or RPE ≥ 9.5. */
    private fun holdTriggered(top: List<WorkoutSet>, target: RepTarget, avgRpe: Double?): Boolean =
        top.any { it.reps < target.lo } || (avgRpe != null && avgRpe >= 9.5)

    private fun prSets(session: List<WorkoutSet>): List<WorkoutSet> =
        session.filter { it.setType.countsForPR && it.reps > 0 }

    /** Most frequent weight; ties go to the heavier one. */
    private fun dominantWeight(sets: List<WorkoutSet>): Double =
        sets.groupingBy { it.weightKg }.eachCount().entries
            .sortedWith(compareByDescending<Map.Entry<Double, Int>> { it.value }.thenByDescending { it.key })
            .first().key

    private fun toKg(displayValue: Double, unit: String): Double = WeightUnits.toKg(displayValue, unit)

    /** Round a kg weight to the nearest [stepDisplay] in the display unit, back in kg. */
    private fun roundToIncrement(kg: Double, stepDisplay: Double, unit: String): Double {
        val display = WeightUnits.fromKg(kg, unit)
        val snapped = (display / stepDisplay).roundToInt() * stepDisplay
        return WeightUnits.toKg(snapped, unit)
    }
}
