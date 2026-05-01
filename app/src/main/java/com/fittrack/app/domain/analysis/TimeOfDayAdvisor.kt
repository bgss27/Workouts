package com.fittrack.app.domain.analysis

import java.util.Calendar

enum class TimeOfDay(val label: String, val icon: String) {
    MORNING("Morning", "wb_sunny"),
    AFTERNOON("Afternoon", "wb_cloudy"),
    EVENING("Evening", "nights_stay");

    companion object {
        fun current(): TimeOfDay {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            return when (hour) {
                in 5..11 -> MORNING
                in 12..16 -> AFTERNOON
                else -> EVENING
            }
        }
    }
}

data class TimeBasedGuidance(
    val timeOfDay: TimeOfDay,
    val warmupAdvice: WarmupAdvice,
    val intensityModifier: Double, // 1.0 = normal, 0.9 = 10% lighter
    val tip: String,
    val detailedTips: List<String>
)

data class WarmupAdvice(
    val warmupSetsPerExercise: Int,
    val warmupReps: Int,
    val warmupWeightPercent: Double, // percentage of working weight
    val mobilityMinutes: Int,
    val description: String
)

object TimeOfDayAdvisor {

    fun getGuidance(): TimeBasedGuidance {
        return when (TimeOfDay.current()) {
            TimeOfDay.MORNING -> morningGuidance()
            TimeOfDay.AFTERNOON -> afternoonGuidance()
            TimeOfDay.EVENING -> eveningGuidance()
        }
    }

    fun getWarmupSets(timeOfDay: TimeOfDay, workingWeight: Double): List<WarmupSet> {
        return when (timeOfDay) {
            TimeOfDay.MORNING -> listOf(
                WarmupSet(reps = 15, weightPercent = 0.0, note = "Empty bar / bodyweight"),
                WarmupSet(reps = 12, weightPercent = 0.30, note = "30% working weight"),
                WarmupSet(reps = 10, weightPercent = 0.50, note = "50% working weight"),
                WarmupSet(reps = 6, weightPercent = 0.70, note = "70% working weight"),
                WarmupSet(reps = 3, weightPercent = 0.85, note = "85% working weight")
            )
            TimeOfDay.AFTERNOON -> listOf(
                WarmupSet(reps = 12, weightPercent = 0.40, note = "40% working weight"),
                WarmupSet(reps = 8, weightPercent = 0.60, note = "60% working weight"),
                WarmupSet(reps = 4, weightPercent = 0.80, note = "80% working weight")
            )
            TimeOfDay.EVENING -> listOf(
                WarmupSet(reps = 10, weightPercent = 0.50, note = "50% working weight"),
                WarmupSet(reps = 5, weightPercent = 0.70, note = "70% working weight")
            )
        }
    }

    fun suggestWeight(baseWeight: Double, timeOfDay: TimeOfDay): Double {
        val modifier = when (timeOfDay) {
            TimeOfDay.MORNING -> 0.90
            TimeOfDay.AFTERNOON -> 0.95
            TimeOfDay.EVENING -> 1.0
        }
        return (baseWeight * modifier * 4).toInt() / 4.0 // round to nearest 0.25
    }

    private fun morningGuidance() = TimeBasedGuidance(
        timeOfDay = TimeOfDay.MORNING,
        warmupAdvice = WarmupAdvice(
            warmupSetsPerExercise = 5,
            warmupReps = 12,
            warmupWeightPercent = 30.0,
            mobilityMinutes = 10,
            description = "Your body temperature is low and joints are stiff. Take extra time to warm up with 5 progressive sets and 10 min of mobility work."
        ),
        intensityModifier = 0.90,
        tip = "Morning workout: Start lighter, warm up thoroughly",
        detailedTips = listOf(
            "Spend 10-15 minutes on dynamic stretching and mobility",
            "Do 5 progressive warmup sets before your first working set",
            "Start with 90% of your usual working weight",
            "Focus on higher reps (8-12) rather than maximal loads",
            "Drink water and have a light snack 30 min before",
            "Body temperature peaks later in the day — save PR attempts for afternoon/evening"
        )
    )

    private fun afternoonGuidance() = TimeBasedGuidance(
        timeOfDay = TimeOfDay.AFTERNOON,
        warmupAdvice = WarmupAdvice(
            warmupSetsPerExercise = 3,
            warmupReps = 10,
            warmupWeightPercent = 40.0,
            mobilityMinutes = 5,
            description = "Body temperature is rising and joints are loosened up. A standard warmup is sufficient."
        ),
        intensityModifier = 0.95,
        tip = "Afternoon workout: Good balance of readiness and energy",
        detailedTips = listOf(
            "5 minutes of light cardio or dynamic stretching",
            "3 progressive warmup sets per exercise",
            "You can train at 95% of your peak capacity",
            "Great time for moderate-to-heavy training",
            "Reaction time and coordination are near peak levels"
        )
    )

    private fun eveningGuidance() = TimeBasedGuidance(
        timeOfDay = TimeOfDay.EVENING,
        warmupAdvice = WarmupAdvice(
            warmupSetsPerExercise = 2,
            warmupReps = 8,
            warmupWeightPercent = 50.0,
            mobilityMinutes = 3,
            description = "Peak performance window. Body temperature and flexibility are at their highest. Minimal warmup needed."
        ),
        intensityModifier = 1.0,
        tip = "Evening workout: Peak performance window — go heavy!",
        detailedTips = listOf(
            "Minimal warmup needed — 2 progressive sets is usually enough",
            "Body temperature and flexibility are at their peak",
            "Best time for PR attempts and heavy compound lifts",
            "Strength output is 5-10% higher than morning",
            "Avoid intense training within 2 hours of bedtime for better sleep"
        )
    )
}

data class WarmupSet(
    val reps: Int,
    val weightPercent: Double,
    val note: String
) {
    fun calculateWeight(workingWeight: Double): Double {
        return (workingWeight * weightPercent * 4).toInt() / 4.0
    }
}
