package com.fittrack.app.domain.model

import com.fittrack.app.data.entity.MuscleGroup

sealed class Suggestion {
    abstract val title: String
    abstract val description: String
    abstract val priority: Int // 1 = highest

    data class IncreaseWeight(
        val exerciseName: String,
        val currentWeight: Double,
        val suggestedWeight: Double,
        val muscleGroup: MuscleGroup
    ) : Suggestion() {
        override val title = "Increase Weight: $exerciseName"
        override val description = "You've been consistently hitting your rep targets. Try increasing from ${currentWeight}kg to ${suggestedWeight}kg."
        override val priority = 1
    }

    data class IncreaseVolume(
        val exerciseName: String,
        val currentSets: Int,
        val suggestedSets: Int,
        val muscleGroup: MuscleGroup
    ) : Suggestion() {
        override val title = "Add More Sets: $exerciseName"
        override val description = "Your strength is progressing but volume has plateaued. Try adding ${suggestedSets - currentSets} more set(s) per session."
        override val priority = 2
    }

    data class IncreaseFrequency(
        val muscleGroup: MuscleGroup,
        val currentFreqPerWeek: Int,
        val suggestedFreqPerWeek: Int
    ) : Suggestion() {
        override val title = "Train ${muscleGroup.displayName} More Often"
        override val description = "You're only training ${muscleGroup.displayName} ${currentFreqPerWeek}x/week. Try increasing to ${suggestedFreqPerWeek}x for better results."
        override val priority = 3
    }

    data class Deload(
        val exerciseName: String,
        val suggestedWeightReduction: Double,
        val muscleGroup: MuscleGroup
    ) : Suggestion() {
        override val title = "Consider a Deload: $exerciseName"
        override val description = "Your progress has stalled and effort is very high. Try reducing weight by ${suggestedWeightReduction.toInt()}% for a week, then build back up."
        override val priority = 4
    }

    data class TryExercise(
        val exerciseName: String,
        val reason: String,
        val muscleGroup: MuscleGroup
    ) : Suggestion() {
        override val title = "Try: $exerciseName"
        override val description = reason
        override val priority = 5
    }

    data class MuscleGroupSummary(
        val muscleGroup: MuscleGroup,
        val weeklyVolume: Int,
        val trend: String // "improving", "plateaued", "declining"
    ) : Suggestion() {
        override val title = "${muscleGroup.displayName} Progress: ${trend.replaceFirstChar { it.uppercase() }}"
        override val description = when (trend) {
            "improving" -> "Great progress! Your ${muscleGroup.displayName} training volume is ${weeklyVolume} sets/week and trending up."
            "plateaued" -> "Your ${muscleGroup.displayName} progress has stalled at ${weeklyVolume} sets/week. Consider changing exercises or increasing volume."
            else -> "Your ${muscleGroup.displayName} volume has dropped to ${weeklyVolume} sets/week. Try to maintain consistency."
        }
        override val priority = if (trend == "improving") 6 else 2
    }
}
