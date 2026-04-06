package com.fittrack.app.domain.ml

import com.fittrack.app.data.entity.Exercise
import com.fittrack.app.data.entity.MuscleGroup
import com.fittrack.app.data.entity.WorkoutSet
import com.fittrack.app.data.repository.ExerciseRepository
import com.fittrack.app.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.first
import kotlin.math.abs

/**
 * ML Analysis Engine that uses statistical/ML techniques to generate
 * per-muscle-group insights and recommendations.
 *
 * Techniques used:
 * - Linear regression for trend detection and prediction
 * - EWMA (Exponentially Weighted Moving Average) for smoothed trends
 * - Coefficient of Variation for plateau detection
 * - Volume-load fatigue modeling
 * - Muscle balance scoring via relative volume analysis
 * - Training phase classification based on rep/intensity patterns
 */
class MlAnalysisEngine(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository
) {
    companion object {
        private const val MIN_SESSIONS = 3
        private const val PLATEAU_CV_THRESHOLD = 0.03
        private const val PLATEAU_WINDOW = 4
        private const val OPTIMAL_WEEKLY_SETS_MIN = 10.0
        private const val OPTIMAL_WEEKLY_SETS_MAX = 20.0
        private const val FATIGUE_VOLUME_INCREASE_THRESHOLD = 1.3 // 30% increase flags fatigue
    }

    /**
     * Generate a complete ML insight report for a specific muscle group.
     */
    suspend fun analyzeMuscleGroup(muscleGroup: MuscleGroup): MuscleInsight {
        val exercises = exerciseRepository.getByMuscleGroup(muscleGroup).first()
        val exerciseInsights = mutableListOf<ExerciseInsight>()
        val all1RMs = mutableListOf<Double>()
        val allVolumes = mutableListOf<Double>()
        val allReps = mutableListOf<Int>()
        val allWeights = mutableListOf<Double>()

        for (exercise in exercises) {
            val sets = workoutRepository.getAllSetsForExercise(exercise.id).first()
            if (sets.isEmpty()) continue

            val insight = analyzeExercise(exercise, sets)
            if (insight != null) {
                exerciseInsights.add(insight)
                // Collect data for muscle-group-level analysis
                val sessionData = extractSessionData(sets)
                all1RMs.addAll(sessionData.map { it.estimated1RM })
                allVolumes.addAll(sessionData.map { it.totalVolume })
                sessionData.forEach { s ->
                    allReps.addAll(s.reps)
                    allWeights.addAll(s.weights)
                }
            }
        }

        // Muscle-group-level regression
        val strengthRegression = if (all1RMs.size >= MIN_SESSIONS) {
            LinearRegression.fitIndexed(all1RMs)
        } else null

        val volumeRegression = if (allVolumes.size >= MIN_SESSIONS) {
            LinearRegression.fitIndexed(allVolumes)
        } else null

        val isPlateaued = all1RMs.size >= PLATEAU_WINDOW &&
            StatisticalUtils.isPlateaued(all1RMs, PLATEAU_WINDOW, PLATEAU_CV_THRESHOLD)

        val plateauDuration = if (isPlateaued) {
            StatisticalUtils.plateauStartIndex(all1RMs, PLATEAU_WINDOW, PLATEAU_CV_THRESHOLD)?.let {
                all1RMs.size - it
            } ?: 0
        } else 0

        // Weekly volume estimation
        val weeklyVolumeSets = estimateWeeklyVolumeSets(muscleGroup)

        // Fatigue scoring
        val fatigueScore = calculateFatigueScore(allVolumes)

        // Recovery status
        val recoveryStatus = classifyRecoveryStatus(fatigueScore)

        // Training phase detection
        val trainingPhase = detectTrainingPhase(allReps, allWeights, allVolumes)

        // Balance score
        val balanceScore = calculateBalanceScore(muscleGroup)

        // Overall score
        val overallScore = calculateOverallScore(
            strengthRegression, volumeRegression, isPlateaued,
            weeklyVolumeSets, fatigueScore, balanceScore, exerciseInsights.size
        )

        // Generate ML recommendations
        val recommendations = generateRecommendations(
            muscleGroup, strengthRegression, volumeRegression, isPlateaued,
            plateauDuration, fatigueScore, weeklyVolumeSets, balanceScore,
            trainingPhase, exerciseInsights
        )

        return MuscleInsight(
            muscleGroup = muscleGroup,
            overallScore = overallScore,
            strengthTrend = strengthRegression?.trend ?: TrendDirection.INSUFFICIENT_DATA,
            volumeTrend = volumeRegression?.trend ?: TrendDirection.INSUFFICIENT_DATA,
            strengthRegression = strengthRegression,
            volumeRegression = volumeRegression,
            weeklyVolumeSets = weeklyVolumeSets,
            isPlateaued = isPlateaued,
            plateauDurationSessions = plateauDuration,
            fatigueScore = fatigueScore,
            recoveryStatus = recoveryStatus,
            balanceScore = balanceScore,
            trainingPhase = trainingPhase,
            recommendations = recommendations.sortedBy { it.priority },
            exerciseInsights = exerciseInsights
        )
    }

    /**
     * Generate insights for all muscle groups the user has trained.
     */
    suspend fun analyzeAllMuscleGroups(): List<MuscleInsight> {
        return MuscleGroup.entries
            .filter { it != MuscleGroup.CARDIO && it != MuscleGroup.FULL_BODY }
            .map { analyzeMuscleGroup(it) }
            .filter { it.exerciseInsights.isNotEmpty() }
            .sortedBy { it.overallScore }
    }

    private fun analyzeExercise(exercise: Exercise, allSets: List<WorkoutSet>): ExerciseInsight? {
        val sessions = allSets.groupBy { it.workoutExerciseId }
        if (sessions.size < 2) return null

        val sessionData = sessions.values.map { sessionSets ->
            val workingSets = sessionSets.filter { !it.isWarmup }
            if (workingSets.isEmpty()) return@map null
            val maxSet = workingSets.maxByOrNull { estimateOneRepMax(it.weightKg, it.reps) }!!
            SessionSummary(
                estimated1RM = estimateOneRepMax(maxSet.weightKg, maxSet.reps),
                totalVolume = workingSets.sumOf { it.weightKg * it.reps },
                maxWeight = workingSets.maxOf { it.weightKg },
                reps = workingSets.map { it.reps },
                weights = workingSets.map { it.weightKg }
            )
        }.filterNotNull()

        if (sessionData.size < 2) return null

        val oneRMs = sessionData.map { it.estimated1RM }
        val regression = LinearRegression.fitIndexed(oneRMs)

        val isPlateaued = oneRMs.size >= PLATEAU_WINDOW &&
            StatisticalUtils.isPlateaued(oneRMs, PLATEAU_WINDOW, PLATEAU_CV_THRESHOLD)

        val predicted1RM = if (regression.rSquared > 0.2) {
            regression.predict(oneRMs.size.toDouble())
        } else {
            oneRMs.lastOrNull() ?: 0.0
        }

        return ExerciseInsight(
            exerciseId = exercise.id,
            exerciseName = exercise.name,
            strengthTrend = regression.trend,
            estimated1RM = oneRMs.lastOrNull() ?: 0.0,
            predicted1RMNextSession = predicted1RM,
            rSquared = regression.rSquared,
            isPlateaued = isPlateaued,
            sessionCount = sessionData.size,
            bestWeight = sessionData.maxOf { it.maxWeight },
            bestVolume = sessionData.maxOf { it.totalVolume }
        )
    }

    private data class SessionSummary(
        val estimated1RM: Double,
        val totalVolume: Double,
        val maxWeight: Double,
        val reps: List<Int>,
        val weights: List<Double>
    )

    private fun extractSessionData(allSets: List<WorkoutSet>): List<SessionSummary> {
        return allSets.groupBy { it.workoutExerciseId }.values.mapNotNull { sessionSets ->
            val workingSets = sessionSets.filter { !it.isWarmup }
            if (workingSets.isEmpty()) return@mapNotNull null
            val maxSet = workingSets.maxByOrNull { estimateOneRepMax(it.weightKg, it.reps) }!!
            SessionSummary(
                estimated1RM = estimateOneRepMax(maxSet.weightKg, maxSet.reps),
                totalVolume = workingSets.sumOf { it.weightKg * it.reps },
                maxWeight = workingSets.maxOf { it.weightKg },
                reps = workingSets.map { it.reps },
                weights = workingSets.map { it.weightKg }
            )
        }
    }

    private fun calculateFatigueScore(volumes: List<Double>): Double {
        if (volumes.size < 3) return 0.0

        // Compare recent volume trend to overall average
        val overallMean = StatisticalUtils.mean(volumes)
        val recentMean = StatisticalUtils.mean(volumes.takeLast(3))
        val ewma = StatisticalUtils.ewma(volumes, 0.4)
        val ewmaTrend = if (ewma.size >= 2) {
            (ewma.last() - ewma[ewma.size - 2]) / ewma[ewma.size - 2].coerceAtLeast(1.0)
        } else 0.0

        // High recent volume relative to average = accumulating fatigue
        val volumeRatio = if (overallMean > 0) recentMean / overallMean else 1.0

        // Rapidly increasing volume = higher fatigue risk
        val fatigue = when {
            volumeRatio > FATIGUE_VOLUME_INCREASE_THRESHOLD -> 70.0 + (volumeRatio - 1.3) * 100
            volumeRatio > 1.15 -> 40.0 + (volumeRatio - 1.15) * 200
            volumeRatio > 1.0 -> 20.0 + (volumeRatio - 1.0) * 133
            else -> volumeRatio * 20.0
        }

        // Add component for consistently increasing trend
        val trendComponent = if (ewmaTrend > 0.05) 15.0 else 0.0

        return (fatigue + trendComponent).coerceIn(0.0, 100.0)
    }

    private fun classifyRecoveryStatus(fatigueScore: Double): RecoveryStatus = when {
        fatigueScore < 25 -> RecoveryStatus.FRESH
        fatigueScore < 50 -> RecoveryStatus.MODERATE
        fatigueScore < 75 -> RecoveryStatus.FATIGUED
        else -> RecoveryStatus.OVERTRAINED
    }

    private fun detectTrainingPhase(
        allReps: List<Int>,
        allWeights: List<Double>,
        allVolumes: List<Double>
    ): TrainingPhase {
        if (allReps.size < 5) return TrainingPhase.BEGINNER

        val recentReps = allReps.takeLast(20)
        val avgReps = recentReps.average()
        val recentWeights = allWeights.takeLast(20)

        // Check volume trend for deload detection
        if (allVolumes.size >= 4) {
            val recentVol = StatisticalUtils.mean(allVolumes.takeLast(3))
            val priorVol = StatisticalUtils.mean(allVolumes.dropLast(3).takeLast(3))
            if (priorVol > 0 && recentVol / priorVol < 0.7) {
                return TrainingPhase.DELOAD
            }
        }

        // Classify based on rep ranges
        return when {
            avgReps <= 5 -> TrainingPhase.STRENGTH
            avgReps <= 12 -> TrainingPhase.HYPERTROPHY
            avgReps <= 20 -> TrainingPhase.ENDURANCE
            else -> TrainingPhase.ENDURANCE
        }
    }

    private suspend fun estimateWeeklyVolumeSets(muscleGroup: MuscleGroup): Double {
        val fourWeeksAgo = System.currentTimeMillis() - (28L * 24 * 60 * 60 * 1000)
        val workouts = workoutRepository.getWorkoutsInRange(fourWeeksAgo, System.currentTimeMillis()).first()

        var totalSets = 0
        for (workout in workouts) {
            for (we in workout.exercises) {
                val exercise = exerciseRepository.getById(we.exerciseId) ?: continue
                if (exercise.muscleGroup == muscleGroup || exercise.secondaryMuscleGroup == muscleGroup) {
                    val sets = workoutRepository.getSetsForWorkoutExercise(we.id).first()
                    totalSets += sets.count { !it.isWarmup }
                }
            }
        }
        return totalSets / 4.0
    }

    private suspend fun calculateBalanceScore(muscleGroup: MuscleGroup): Double {
        // Compare this muscle group's weekly volume to the average across all trained groups
        val allVolumes = mutableMapOf<MuscleGroup, Double>()
        for (group in MuscleGroup.entries) {
            if (group == MuscleGroup.CARDIO || group == MuscleGroup.FULL_BODY) continue
            allVolumes[group] = estimateWeeklyVolumeSets(group)
        }

        val trainedVolumes = allVolumes.values.filter { it > 0 }
        if (trainedVolumes.isEmpty()) return 50.0

        val avgVolume = trainedVolumes.average()
        val thisVolume = allVolumes[muscleGroup] ?: 0.0

        if (avgVolume == 0.0) return 50.0

        // Score: 100 = perfectly balanced, lower = undertrained relative to others
        val ratio = thisVolume / avgVolume
        return (ratio * 50.0).coerceIn(0.0, 100.0)
    }

    private fun calculateOverallScore(
        strengthReg: LinearRegressionResult?,
        volumeReg: LinearRegressionResult?,
        isPlateaued: Boolean,
        weeklyVolume: Double,
        fatigue: Double,
        balance: Double,
        exerciseCount: Int
    ): Double {
        var score = 50.0 // Start at 50

        // Strength trend component (0-25 points)
        strengthReg?.let { reg ->
            score += when (reg.trend) {
                TrendDirection.IMPROVING -> 25.0
                TrendDirection.SLIGHTLY_IMPROVING -> 15.0
                TrendDirection.FLAT -> 5.0
                TrendDirection.SLIGHTLY_DECLINING -> -5.0
                TrendDirection.DECLINING -> -15.0
                else -> 0.0
            }
        }

        // Volume adequacy component (0-15 points)
        score += when {
            weeklyVolume in OPTIMAL_WEEKLY_SETS_MIN..OPTIMAL_WEEKLY_SETS_MAX -> 15.0
            weeklyVolume > 0 -> 7.0
            else -> -10.0
        }

        // Plateau penalty (-10 points)
        if (isPlateaued) score -= 10.0

        // Fatigue penalty (0 to -10 points)
        score -= (fatigue / 100.0) * 10.0

        // Balance component (-5 to +5)
        score += (balance - 50.0) / 10.0

        // Exercise variety bonus (0-5)
        score += (exerciseCount.coerceAtMost(5) * 1.0)

        return score.coerceIn(0.0, 100.0)
    }

    private fun generateRecommendations(
        muscleGroup: MuscleGroup,
        strengthReg: LinearRegressionResult?,
        volumeReg: LinearRegressionResult?,
        isPlateaued: Boolean,
        plateauDuration: Int,
        fatigue: Double,
        weeklyVolume: Double,
        balance: Double,
        phase: TrainingPhase,
        exerciseInsights: List<ExerciseInsight>
    ): List<MlRecommendation> {
        val recommendations = mutableListOf<MlRecommendation>()

        // 1. Plateau-based recommendations
        if (isPlateaued && plateauDuration >= 3) {
            val confidence = (plateauDuration.coerceAtMost(8) / 8.0)

            if (fatigue > 60) {
                recommendations.add(MlRecommendation(
                    type = RecommendationType.DELOAD,
                    title = "Deload Recommended for ${muscleGroup.displayName}",
                    description = "Your ${muscleGroup.displayName} strength has plateaued for $plateauDuration sessions with high fatigue. " +
                        "Reduce volume by 40-50% for 1 week to allow supercompensation. " +
                        "ML analysis shows a ${(fatigue).toInt()}% fatigue score.",
                    confidence = confidence,
                    priority = 1
                ))
            } else {
                recommendations.add(MlRecommendation(
                    type = RecommendationType.INCREASE_VOLUME,
                    title = "Break ${muscleGroup.displayName} Plateau with More Volume",
                    description = "Strength has been flat for $plateauDuration sessions. " +
                        "Since fatigue is manageable (${fatigue.toInt()}%), try adding 2-3 more sets per week " +
                        "to provide a new stimulus for adaptation.",
                    confidence = confidence,
                    priority = 1
                ))

                // Also suggest exercise variation
                val plateauedExercises = exerciseInsights.filter { it.isPlateaued }
                if (plateauedExercises.isNotEmpty()) {
                    recommendations.add(MlRecommendation(
                        type = RecommendationType.CHANGE_EXERCISE,
                        title = "Swap Stalled ${muscleGroup.displayName} Exercises",
                        description = "${plateauedExercises.joinToString(", ") { it.exerciseName }} " +
                            "${if (plateauedExercises.size == 1) "has" else "have"} plateaued. " +
                            "Try a different variation to stimulate new muscle fibers and break through the plateau.",
                        confidence = confidence * 0.8,
                        priority = 2
                    ))
                }
            }
        }

        // 2. Strength trend recommendations
        strengthReg?.let { reg ->
            if (reg.trend == TrendDirection.DECLINING && reg.rSquared > 0.3) {
                recommendations.add(MlRecommendation(
                    type = RecommendationType.RECOVERY,
                    title = "${muscleGroup.displayName} Strength is Declining",
                    description = "ML regression shows a downward trend (R²=${String.format("%.2f", reg.rSquared)}, " +
                        "slope=${String.format("%.2f", reg.slope)}/session). " +
                        "This may indicate overtraining or insufficient recovery. " +
                        "Consider reducing intensity or adding an extra rest day.",
                    confidence = reg.rSquared,
                    priority = 1
                ))
            } else if (reg.trend == TrendDirection.IMPROVING && reg.rSquared > 0.5) {
                recommendations.add(MlRecommendation(
                    type = RecommendationType.INCREASE_WEIGHT,
                    title = "${muscleGroup.displayName} is Progressing Well",
                    description = "Strong upward trend detected (R²=${String.format("%.2f", reg.rSquared)}). " +
                        "Continue current approach. Predicted improvement: " +
                        "${String.format("%.1f", abs(reg.slope))}kg/session on key lifts.",
                    confidence = reg.rSquared,
                    priority = 5
                ))
            }
        }

        // 3. Volume recommendations
        if (weeklyVolume < OPTIMAL_WEEKLY_SETS_MIN && weeklyVolume > 0) {
            recommendations.add(MlRecommendation(
                type = RecommendationType.INCREASE_VOLUME,
                title = "Increase ${muscleGroup.displayName} Volume",
                description = "Currently doing ${String.format("%.0f", weeklyVolume)} sets/week for ${muscleGroup.displayName}. " +
                    "Research suggests ${OPTIMAL_WEEKLY_SETS_MIN.toInt()}-${OPTIMAL_WEEKLY_SETS_MAX.toInt()} sets/week is optimal for hypertrophy. " +
                    "Adding ${(OPTIMAL_WEEKLY_SETS_MIN - weeklyVolume).toInt()} more sets could accelerate growth.",
                confidence = 0.7,
                priority = 3
            ))
        } else if (weeklyVolume > OPTIMAL_WEEKLY_SETS_MAX) {
            recommendations.add(MlRecommendation(
                type = RecommendationType.RECOVERY,
                title = "High ${muscleGroup.displayName} Volume - Watch for Overtraining",
                description = "${String.format("%.0f", weeklyVolume)} sets/week exceeds the typical optimal range. " +
                    "Unless you're an advanced lifter, this may lead to diminishing returns. " +
                    "Consider redistributing volume across the week.",
                confidence = 0.6,
                priority = 3
            ))
        }

        // 4. Frequency recommendations
        if (weeklyVolume > 0 && weeklyVolume < 4) {
            recommendations.add(MlRecommendation(
                type = RecommendationType.INCREASE_FREQUENCY,
                title = "Train ${muscleGroup.displayName} More Often",
                description = "Very low training frequency detected. Training each muscle group 2-3x/week " +
                    "allows for better volume distribution and more frequent growth stimulus.",
                confidence = 0.75,
                priority = 2
            ))
        }

        // 5. Balance recommendations
        if (balance < 30) {
            recommendations.add(MlRecommendation(
                type = RecommendationType.MUSCLE_BALANCE,
                title = "${muscleGroup.displayName} is Undertrained",
                description = "Compared to your other muscle groups, ${muscleGroup.displayName} receives " +
                    "significantly less training volume (balance score: ${balance.toInt()}/100). " +
                    "This imbalance could lead to postural issues or injury risk.",
                confidence = 0.65,
                priority = 2
            ))
        }

        // 6. Periodization recommendations
        if (phase == TrainingPhase.HYPERTROPHY && isPlateaued) {
            recommendations.add(MlRecommendation(
                type = RecommendationType.PERIODIZATION_SHIFT,
                title = "Consider a Strength Block for ${muscleGroup.displayName}",
                description = "You've been training in the hypertrophy range but have plateaued. " +
                    "Switching to a 4-6 week strength block (3-5 reps at 85%+) can break the plateau " +
                    "by improving neural adaptations, then return to hypertrophy with heavier loads.",
                confidence = 0.6,
                priority = 3
            ))
        } else if (phase == TrainingPhase.STRENGTH && isPlateaued) {
            recommendations.add(MlRecommendation(
                type = RecommendationType.PERIODIZATION_SHIFT,
                title = "Switch to Hypertrophy for ${muscleGroup.displayName}",
                description = "Strength gains have plateaued. A hypertrophy block (8-12 reps) for 4-6 weeks " +
                    "can build more muscle mass, which provides a foundation for future strength gains.",
                confidence = 0.6,
                priority = 3
            ))
        }

        // 7. Per-exercise weight increase suggestions
        for (insight in exerciseInsights) {
            if (!insight.isPlateaued && insight.strengthTrend == TrendDirection.IMPROVING &&
                insight.predicted1RMNextSession > insight.estimated1RM * 1.02) {
                val increase = insight.predicted1RMNextSession - insight.estimated1RM
                recommendations.add(MlRecommendation(
                    type = RecommendationType.INCREASE_WEIGHT,
                    title = "Increase ${insight.exerciseName} Weight",
                    description = "ML predicts you're ready for +${String.format("%.1f", increase)}kg " +
                        "(confidence: ${(insight.rSquared * 100).toInt()}%). " +
                        "Current est. 1RM: ${String.format("%.1f", insight.estimated1RM)}kg, " +
                        "predicted: ${String.format("%.1f", insight.predicted1RMNextSession)}kg.",
                    confidence = insight.rSquared,
                    priority = 4
                ))
            }
        }

        return recommendations
    }

    private fun estimateOneRepMax(weight: Double, reps: Int): Double {
        if (reps <= 0 || weight <= 0) return 0.0
        if (reps == 1) return weight
        return weight * (1 + reps / 30.0)
    }
}
