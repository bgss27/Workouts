package com.fittrack.app.domain.analysis

import com.fittrack.app.data.entity.Exercise
import com.fittrack.app.data.entity.MuscleGroup
import com.fittrack.app.data.entity.WorkoutSet
import com.fittrack.app.data.repository.ExerciseRepository
import com.fittrack.app.data.repository.WorkoutRepository
import com.fittrack.app.domain.model.ProgressDataPoint
import com.fittrack.app.domain.model.Suggestion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class ProgressAnalyzer(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository
) {
    companion object {
        private const val PLATEAU_THRESHOLD = 0.01 // 1% improvement threshold
        private const val MIN_SESSIONS_FOR_ANALYSIS = 3
        private const val WEIGHT_INCREMENT_UPPER = 2.5
        private const val WEIGHT_INCREMENT_LOWER = 5.0
    }

    fun getProgressForExercise(exerciseId: Long, startTime: Long, endTime: Long): Flow<List<ProgressDataPoint>> {
        return workoutRepository.getSetsForExerciseInRange(exerciseId, startTime, endTime).map { sets ->
            groupSetsIntoSessions(sets)
        }
    }

    fun getProgressForExerciseAllTime(exerciseId: Long): Flow<List<ProgressDataPoint>> {
        return workoutRepository.getAllSetsForExercise(exerciseId).map { sets ->
            groupSetsIntoSessions(sets)
        }
    }

    private fun groupSetsIntoSessions(sets: List<WorkoutSet>): List<ProgressDataPoint> {
        if (sets.isEmpty()) return emptyList()

        return sets.groupBy { it.workoutExerciseId }.map { (_, sessionSets) ->
            val workingSets = sessionSets.filter { !it.isWarmup }
            if (workingSets.isEmpty()) return@map null

            val maxSet = workingSets.maxByOrNull { estimateOneRepMax(it.weightKg, it.reps) }!!
            val estimated1RM = estimateOneRepMax(maxSet.weightKg, maxSet.reps)
            val totalVolume = workingSets.sumOf { it.weightKg * it.reps }
            val maxWeight = workingSets.maxOf { it.weightKg }

            ProgressDataPoint(
                date = System.currentTimeMillis(), // Will be replaced with actual workout date
                estimated1RM = estimated1RM,
                totalVolume = totalVolume,
                maxWeight = maxWeight,
                totalSets = workingSets.size,
                totalReps = workingSets.sumOf { it.reps }
            )
        }.filterNotNull()
    }

    suspend fun generateSuggestions(): List<Suggestion> {
        val suggestions = mutableListOf<Suggestion>()
        val exercises = exerciseRepository.getAllExercises().first()

        for (exercise in exercises) {
            val sets = workoutRepository.getAllSetsForExercise(exercise.id).first()
            if (sets.isEmpty()) continue

            val sessionSets = sets.groupBy { it.workoutExerciseId }
            if (sessionSets.size < MIN_SESSIONS_FOR_ANALYSIS) continue

            val sessions = sessionSets.values.toList()
            analyzeExerciseProgress(exercise, sessions, suggestions)
        }

        // Analyze muscle group frequency
        analyzeMuscleGroupFrequency(suggestions)

        return suggestions.sortedBy { it.priority }
    }

    suspend fun getSuggestionsForMuscleGroup(muscleGroup: MuscleGroup): List<Suggestion> {
        val suggestions = mutableListOf<Suggestion>()
        val exercises = exerciseRepository.getByMuscleGroup(muscleGroup).first()

        for (exercise in exercises) {
            val sets = workoutRepository.getAllSetsForExercise(exercise.id).first()
            if (sets.isEmpty()) continue

            val sessionSets = sets.groupBy { it.workoutExerciseId }
            if (sessionSets.size < MIN_SESSIONS_FOR_ANALYSIS) continue

            val sessions = sessionSets.values.toList()
            analyzeExerciseProgress(exercise, sessions, suggestions)
        }

        // Suggest new exercises for the muscle group
        suggestNewExercises(muscleGroup, exercises, suggestions)

        return suggestions.sortedBy { it.priority }
    }

    private fun analyzeExerciseProgress(
        exercise: Exercise,
        sessions: List<List<WorkoutSet>>,
        suggestions: MutableList<Suggestion>
    ) {
        val recentSessions = sessions.takeLast(4)
        val oneRMs = recentSessions.map { session ->
            val workingSets = session.filter { !it.isWarmup }
            if (workingSets.isEmpty()) 0.0
            else workingSets.maxOf { estimateOneRepMax(it.weightKg, it.reps) }
        }

        // Plateau detection
        if (oneRMs.size >= 3) {
            val improvement = if (oneRMs.first() > 0) {
                (oneRMs.last() - oneRMs.first()) / oneRMs.first()
            } else 0.0

            val lastSession = recentSessions.last().filter { !it.isWarmup }
            val avgRpe = lastSession.mapNotNull { it.rpe }.average().let {
                if (it.isNaN()) 7.0 else it
            }

            if (improvement < PLATEAU_THRESHOLD) {
                // Plateaued - check if it's high effort
                if (avgRpe >= 9.0) {
                    // Suggest deload
                    suggestions.add(Suggestion.Deload(
                        exerciseName = exercise.name,
                        suggestedWeightReduction = 10.0,
                        muscleGroup = exercise.muscleGroup
                    ))
                } else {
                    // Suggest volume increase
                    val avgSets = recentSessions.map { it.filter { s -> !s.isWarmup }.size }.average().toInt()
                    suggestions.add(Suggestion.IncreaseVolume(
                        exerciseName = exercise.name,
                        currentSets = avgSets,
                        suggestedSets = avgSets + 1,
                        muscleGroup = exercise.muscleGroup
                    ))
                }
            }
        }

        // Progressive overload - check if hitting rep targets consistently
        val lastSession = recentSessions.lastOrNull()?.filter { !it.isWarmup } ?: return
        val maxRepsInLastSession = lastSession.maxOfOrNull { it.reps } ?: return
        val lastWeight = lastSession.maxOfOrNull { it.weightKg } ?: return

        if (maxRepsInLastSession >= 12 && lastSession.all { it.reps >= 8 }) {
            val increment = if (exercise.muscleGroup in listOf(MuscleGroup.LEGS, MuscleGroup.GLUTES, MuscleGroup.BACK)) {
                WEIGHT_INCREMENT_LOWER
            } else {
                WEIGHT_INCREMENT_UPPER
            }
            suggestions.add(Suggestion.IncreaseWeight(
                exerciseName = exercise.name,
                currentWeight = lastWeight,
                suggestedWeight = lastWeight + increment,
                muscleGroup = exercise.muscleGroup
            ))
        }
    }

    private suspend fun analyzeMuscleGroupFrequency(suggestions: MutableList<Suggestion>) {
        val fourWeeksAgo = System.currentTimeMillis() - (28L * 24 * 60 * 60 * 1000)
        val workouts = workoutRepository.getWorkoutsInRange(fourWeeksAgo, System.currentTimeMillis()).first()

        val muscleGroupFrequency = mutableMapOf<MuscleGroup, Int>()
        for (workout in workouts) {
            for (workoutExercise in workout.exercises) {
                val exercise = exerciseRepository.getById(workoutExercise.exerciseId) ?: continue
                muscleGroupFrequency[exercise.muscleGroup] =
                    (muscleGroupFrequency[exercise.muscleGroup] ?: 0) + 1
            }
        }

        val weeksTracked = 4
        for ((group, totalSessions) in muscleGroupFrequency) {
            val freqPerWeek = totalSessions / weeksTracked
            if (freqPerWeek < 2 && group != MuscleGroup.CARDIO) {
                suggestions.add(Suggestion.IncreaseFrequency(
                    muscleGroup = group,
                    currentFreqPerWeek = freqPerWeek,
                    suggestedFreqPerWeek = 2
                ))
            }
        }
    }

    private suspend fun suggestNewExercises(
        muscleGroup: MuscleGroup,
        currentExercises: List<Exercise>,
        suggestions: MutableList<Suggestion>
    ) {
        val allExercises = exerciseRepository.getByMuscleGroup(muscleGroup).first()
        val usedExerciseIds = currentExercises.filter { exercise ->
            workoutRepository.getAllSetsForExercise(exercise.id).first().isNotEmpty()
        }.map { it.id }.toSet()

        val unusedExercises = allExercises.filter { it.id !in usedExerciseIds }
        unusedExercises.take(2).forEach { exercise ->
            suggestions.add(Suggestion.TryExercise(
                exerciseName = exercise.name,
                reason = "Add variety to your ${muscleGroup.displayName} training. ${exercise.name} targets the muscle from a different angle.",
                muscleGroup = muscleGroup
            ))
        }
    }

    fun estimateOneRepMax(weight: Double, reps: Int): Double {
        if (reps <= 0 || weight <= 0) return 0.0
        if (reps == 1) return weight
        // Epley formula
        return weight * (1 + reps / 30.0)
    }
}
