package com.fittrack.app.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.entity.WorkoutSet
import com.fittrack.app.data.relation.WorkoutWithExercises
import com.fittrack.app.data.repository.ExerciseRepository
import com.fittrack.app.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ExerciseDetail(
    val exerciseName: String,
    val muscleGroup: String,
    val sets: List<WorkoutSet>
)

class WorkoutDetailViewModel(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val workoutId: Long
) : ViewModel() {

    val workout: StateFlow<WorkoutWithExercises?> = workoutRepository.getWorkoutById(workoutId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _exerciseDetails = MutableStateFlow<List<ExerciseDetail>>(emptyList())
    val exerciseDetails: StateFlow<List<ExerciseDetail>> = _exerciseDetails

    init {
        viewModelScope.launch {
            workoutRepository.getWorkoutById(workoutId).collect { workoutWithExercises ->
                workoutWithExercises ?: return@collect
                val details = workoutWithExercises.exercises.sortedBy { it.orderIndex }.map { we ->
                    val exercise = exerciseRepository.getById(we.exerciseId)
                    val sets = workoutRepository.getSetsForWorkoutExercise(we.id).first()
                    ExerciseDetail(
                        exerciseName = exercise?.name ?: "Unknown",
                        muscleGroup = exercise?.muscleGroup?.displayName ?: "",
                        sets = sets
                    )
                }
                _exerciseDetails.value = details
            }
        }
    }

    class Factory(
        private val workoutRepository: WorkoutRepository,
        private val exerciseRepository: ExerciseRepository,
        private val workoutId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WorkoutDetailViewModel(workoutRepository, exerciseRepository, workoutId) as T
        }
    }
}
