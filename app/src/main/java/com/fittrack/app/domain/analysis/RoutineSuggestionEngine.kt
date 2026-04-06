package com.fittrack.app.domain.analysis

import com.fittrack.app.data.entity.Exercise
import com.fittrack.app.data.entity.MuscleGroup
import com.fittrack.app.data.repository.ExerciseRepository
import com.fittrack.app.data.repository.RoutineRepository
import com.fittrack.app.data.relation.RoutineWithExercises
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class RoutineSuggestionEngine(
    private val exerciseRepository: ExerciseRepository,
    private val routineRepository: RoutineRepository
) {
    fun getRoutinesForMuscleGroup(muscleGroup: MuscleGroup): Flow<List<RoutineWithExercises>> {
        return routineRepository.getRoutinesForMuscleGroup(muscleGroup.name)
    }

    fun getAllRoutines(): Flow<List<RoutineWithExercises>> {
        return routineRepository.getAllRoutines()
    }

    fun getRoutineById(id: Long): Flow<RoutineWithExercises?> {
        return routineRepository.getRoutineById(id)
    }

    suspend fun getExercisesForMuscleGroup(muscleGroup: MuscleGroup): List<Exercise> {
        return exerciseRepository.getByMuscleGroup(muscleGroup).first()
    }

    suspend fun suggestExercisesForMuscleGroup(
        muscleGroup: MuscleGroup,
        count: Int = 5
    ): List<ExerciseSuggestion> {
        val exercises = exerciseRepository.getByMuscleGroup(muscleGroup).first()
        return exercises.take(count).mapIndexed { index, exercise ->
            ExerciseSuggestion(
                exercise = exercise,
                suggestedSets = when {
                    index == 0 -> 4 // Primary compound: more sets
                    exercise.secondaryMuscleGroup != null -> 3 // Compound: moderate sets
                    else -> 3 // Isolation: moderate sets
                },
                suggestedReps = when {
                    index == 0 -> "6-8" // Primary: heavier
                    exercise.secondaryMuscleGroup != null -> "8-12" // Compound: moderate
                    else -> "10-15" // Isolation: higher reps
                },
                reason = when {
                    index == 0 -> "Primary compound movement for ${muscleGroup.displayName}"
                    exercise.secondaryMuscleGroup != null -> "Compound movement also targeting ${exercise.secondaryMuscleGroup!!.displayName}"
                    else -> "Isolation exercise to target ${muscleGroup.displayName} directly"
                }
            )
        }
    }
}

data class ExerciseSuggestion(
    val exercise: Exercise,
    val suggestedSets: Int,
    val suggestedReps: String,
    val reason: String
)
