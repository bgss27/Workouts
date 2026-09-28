package com.fittrack.app.domain.analysis

import com.fittrack.app.data.entity.Exercise
import com.fittrack.app.data.entity.MuscleGroup
import com.fittrack.app.data.entity.WorkoutSet
import com.fittrack.app.data.repository.ExerciseRepository
import com.fittrack.app.data.repository.RoutineRepository
import com.fittrack.app.data.repository.UserPlanRepository
import com.fittrack.app.data.repository.WorkoutRepository
import com.fittrack.app.domain.model.ProgressDataPoint
import com.fittrack.app.domain.model.Suggestion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class ProgressAnalyzer(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val userPlanRepository: UserPlanRepository,
    private val routineRepository: RoutineRepository,
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

        // Count DISTINCT workout days per muscle group (not exercises). A
        // workout with 5 back exercises still counts as one back day —
        // that's what "frequency per week" actually means.
        val muscleGroupDays = mutableMapOf<MuscleGroup, Int>()
        for (workout in workouts) {
            val groupsHit = mutableSetOf<MuscleGroup>()
            for (workoutExercise in workout.exercises) {
                val exercise = exerciseRepository.getById(workoutExercise.exerciseId) ?: continue
                groupsHit.add(exercise.muscleGroup)
            }
            for (g in groupsHit) {
                muscleGroupDays[g] = (muscleGroupDays[g] ?: 0) + 1
            }
        }

        // Pull the user's plan-prescribed frequency. If they're on a 5-day
        // bro split that only schedules Back once a week, hitting Back 1x/week
        // means they're ON TRACK — don't tell them to do more. Fall back to
        // the generic 2x/week rule only when no plan is set.
        val planned = computePlannedFrequencyPerWeek()

        // Skip frequency suggestions until the user has at least 2 weeks of
        // history. Before that any "you only did Shoulders 1 time" alert is
        // premature noise — they may just be 3 days into the app.
        val firstWorkoutMs = workouts.minOfOrNull { it.workout.startTime }
        if (firstWorkoutMs != null) {
            val daysSpan = (System.currentTimeMillis() - firstWorkoutMs) / (24L * 60 * 60 * 1000)
            if (daysSpan < 14) return
        }

        val weeksTracked = 4
        // Iterate over every muscle group the plan covers, not just the ones
        // the user has touched. Otherwise a never-trained muscle silently
        // skips the check.
        val groupsToCheck = (muscleGroupDays.keys + planned.keys).distinct()
        for (group in groupsToCheck) {
            if (group == MuscleGroup.CARDIO) continue
            val totalDays = muscleGroupDays[group] ?: 0
            val target = planned[group] ?: 2
            val expectedTotal = target * weeksTracked
            // Compare totals — int per-week division truncates 1/4, 2/4, 3/4
            // all to 0, which was the source of the "training shoulders 0
            // times" bug even when the user had been doing it.
            if (totalDays < expectedTotal) {
                suggestions.add(Suggestion.IncreaseFrequency(
                    muscleGroup = group,
                    sessionsLast4Weeks = totalDays,
                    suggestedFreqPerWeek = target,
                ))
            }
        }
    }

    /**
     * Days-per-week that each muscle group is scheduled by the user's current
     * plan. Each plan day's routine contributes one "day" for every muscle
     * group that has at least one exercise in that routine (primary muscle
     * only — secondary work is too noisy to count as a session). Returns
     * empty when there's no plan, signalling callers to fall back to a
     * generic threshold.
     */
    private suspend fun computePlannedFrequencyPerWeek(): Map<MuscleGroup, Int> {
        val plans = userPlanRepository.getAllPlans().first()
        if (plans.isEmpty()) return emptyMap()
        val planned = mutableMapOf<MuscleGroup, Int>()
        for (plan in plans) {
            val routine = routineRepository.getRoutineById(plan.routineId).first() ?: continue
            val groupsInRoutine = mutableSetOf<MuscleGroup>()
            for (re in routine.exercises) {
                val ex = exerciseRepository.getById(re.exerciseId) ?: continue
                groupsInRoutine.add(ex.muscleGroup)
            }
            for (g in groupsInRoutine) {
                planned[g] = (planned[g] ?: 0) + 1
            }
        }
        return planned
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

    /**
     * Detect 1RM personal records set in the given workout. For each exercise
     * performed, compares the best estimated 1RM in this workout against the
     * user's best across all OTHER completed workouts. Returns one [PRResult]
     * per exercise where the new best beats the previous by more than 0.5 kg.
     *
     * Returns an empty list when the user has no prior data for an exercise —
     * the very first time logging something isn't a "PR" celebration.
     */
    suspend fun detectPRs(workoutId: Long): List<PRResult> {
        val workoutWithExercises = workoutRepository.getWorkoutById(workoutId).first() ?: return emptyList()
        val results = mutableListOf<PRResult>()

        for (we in workoutWithExercises.exercises) {
            val exercise = exerciseRepository.getById(we.exerciseId) ?: continue
            // workoutExerciseIds for this exercise within the current workout —
            // used to exclude this workout's contributions when computing "previous best".
            val currentWeIds = workoutWithExercises.exercises
                .filter { it.exerciseId == we.exerciseId }
                .map { it.id }
                .toSet()

            val currentSets = workoutRepository
                .getSetsForWorkoutExercise(we.id).first()
                .filter { !it.isWarmup }
            if (currentSets.isEmpty()) continue
            val newBest = currentSets.maxOf { estimateOneRepMax(it.weightKg, it.reps) }

            val allHistoric = workoutRepository
                .getAllSetsForExercise(we.exerciseId).first()
            val previousSets = allHistoric.filter { it.workoutExerciseId !in currentWeIds }
            val previousBest = previousSets
                .maxOfOrNull { estimateOneRepMax(it.weightKg, it.reps) } ?: 0.0

            // Skip first-time exercises (no prior best to beat) and skip
            // sub-0.5kg improvements that are likely floating-point or unit-
            // conversion noise rather than a real PR.
            if (previousBest > 0 && newBest > previousBest + 0.5) {
                results += PRResult(
                    exerciseName = exercise.name,
                    newBest1RM = newBest,
                    previousBest1RM = previousBest,
                    delta = newBest - previousBest,
                )
            }
        }

        // Stable order by largest delta first so the headline PR shows on top.
        return results.sortedByDescending { it.delta }
    }
}

/** A 1RM personal record set in a single workout. Weights in kg. */
data class PRResult(
    val exerciseName: String,
    val newBest1RM: Double,
    val previousBest1RM: Double,
    val delta: Double,
)
