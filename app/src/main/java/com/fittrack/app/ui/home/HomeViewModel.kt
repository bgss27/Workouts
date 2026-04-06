package com.fittrack.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.entity.Exercise
import com.fittrack.app.data.relation.WorkoutWithExercises
import com.fittrack.app.data.repository.ExerciseRepository
import com.fittrack.app.data.repository.WorkoutRepository
import com.fittrack.app.domain.analysis.ProgressAnalyzer
import com.fittrack.app.domain.model.Suggestion
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val progressAnalyzer: ProgressAnalyzer
) : ViewModel() {

    val recentWorkouts: StateFlow<List<WorkoutWithExercises>> = workoutRepository.getAllWorkouts()
        .map { it.take(5) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedWorkoutCount: StateFlow<Int> = workoutRepository.getCompletedWorkoutCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _suggestions = MutableStateFlow<List<Suggestion>>(emptyList())
    val suggestions: StateFlow<List<Suggestion>> = _suggestions

    private val _exercises = MutableStateFlow<Map<Long, Exercise>>(emptyMap())
    val exercises: StateFlow<Map<Long, Exercise>> = _exercises

    init {
        viewModelScope.launch {
            _suggestions.value = progressAnalyzer.generateSuggestions().take(5)
        }
        viewModelScope.launch {
            exerciseRepository.getAllExercises().collect { list ->
                _exercises.value = list.associateBy { it.id }
            }
        }
    }

    class Factory(
        private val workoutRepository: WorkoutRepository,
        private val exerciseRepository: ExerciseRepository,
        private val progressAnalyzer: ProgressAnalyzer
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(workoutRepository, exerciseRepository, progressAnalyzer) as T
        }
    }
}
