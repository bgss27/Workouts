package com.fittrack.app.domain.ml

import com.fittrack.app.data.entity.MuscleGroup

/**
 * ML-generated insight report for a specific muscle group.
 * Combines multiple statistical analyses into actionable recommendations.
 */
data class MuscleInsight(
    val muscleGroup: MuscleGroup,
    val overallScore: Double, // 0-100, overall muscle development score
    val strengthTrend: TrendDirection,
    val volumeTrend: TrendDirection,
    val strengthRegression: LinearRegressionResult?,
    val volumeRegression: LinearRegressionResult?,
    val weeklyVolumeSets: Double, // average weekly sets
    val isPlateaued: Boolean,
    val plateauDurationSessions: Int, // how many sessions in plateau
    val fatigueScore: Double, // 0-100, higher = more fatigued
    val recoveryStatus: RecoveryStatus,
    val balanceScore: Double, // relative to other muscle groups, 0-100
    val trainingPhase: TrainingPhase,
    val recommendations: List<MlRecommendation>,
    val exerciseInsights: List<ExerciseInsight>
)

data class ExerciseInsight(
    val exerciseId: Long,
    val exerciseName: String,
    val strengthTrend: TrendDirection,
    val estimated1RM: Double,
    val predicted1RMNextSession: Double,
    val rSquared: Double, // confidence in prediction
    val isPlateaued: Boolean,
    val sessionCount: Int,
    val bestWeight: Double,
    val bestVolume: Double
)

data class MlRecommendation(
    val type: RecommendationType,
    val title: String,
    val description: String,
    val confidence: Double, // 0-1
    val priority: Int // 1 = highest
)

enum class RecommendationType {
    INCREASE_WEIGHT,
    INCREASE_VOLUME,
    INCREASE_FREQUENCY,
    DELOAD,
    CHANGE_EXERCISE,
    PERIODIZATION_SHIFT,
    MUSCLE_BALANCE,
    RECOVERY
}

enum class RecoveryStatus(val label: String) {
    FRESH("Well Recovered"),
    MODERATE("Moderately Recovered"),
    FATIGUED("Needs Recovery"),
    OVERTRAINED("Overtrained - Rest Needed")
}

enum class TrainingPhase(val label: String, val description: String) {
    BEGINNER("Beginner", "Focus on learning movement patterns and building base strength"),
    STRENGTH("Strength Phase", "Low reps, heavy weight - building maximal strength"),
    HYPERTROPHY("Hypertrophy Phase", "Moderate reps and weight - maximizing muscle growth"),
    ENDURANCE("Endurance Phase", "High reps, lower weight - building muscular endurance"),
    DELOAD("Deload Phase", "Reduced volume/intensity for recovery"),
    MAINTENANCE("Maintenance", "Stable training to maintain current fitness")
}
