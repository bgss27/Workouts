package com.fittrack.app.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.entity.Exercise
import com.fittrack.app.data.relation.WorkoutWithExercises
import com.fittrack.app.data.repository.ExerciseRepository
import com.fittrack.app.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.*

class WorkoutHistoryViewModel(
    workoutRepository: WorkoutRepository,
    exerciseRepository: ExerciseRepository
) : ViewModel() {

    val workouts: StateFlow<List<WorkoutWithExercises>> = workoutRepository.getAllWorkouts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exercises: StateFlow<Map<Long, Exercise>> = exerciseRepository.getAllExercises()
        .map { list -> list.associateBy { it.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    class Factory(
        private val workoutRepository: WorkoutRepository,
        private val exerciseRepository: ExerciseRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WorkoutHistoryViewModel(workoutRepository, exerciseRepository) as T
        }
    }
}
