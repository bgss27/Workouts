package com.fittrack.app.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.entity.Exercise
import com.fittrack.app.data.entity.MuscleGroup
import com.fittrack.app.data.repository.ExerciseRepository
import com.fittrack.app.domain.analysis.ProgressAnalyzer
import com.fittrack.app.domain.model.ProgressDataPoint
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProgressViewModel(
    private val exerciseRepository: ExerciseRepository,
    private val progressAnalyzer: ProgressAnalyzer
) : ViewModel() {

    private val _selectedMuscleGroup = MutableStateFlow<MuscleGroup?>(null)
    val selectedMuscleGroup: StateFlow<MuscleGroup?> = _selectedMuscleGroup

    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises

    private val _selectedExercise = MutableStateFlow<Exercise?>(null)
    val selectedExercise: StateFlow<Exercise?> = _selectedExercise

    private val _progressData = MutableStateFlow<List<ProgressDataPoint>>(emptyList())
    val progressData: StateFlow<List<ProgressDataPoint>> = _progressData

    init {
        viewModelScope.launch {
            exerciseRepository.getAllExercises().collect { _exercises.value = it }
        }
    }

    fun selectMuscleGroup(group: MuscleGroup?) {
        _selectedMuscleGroup.value = group
        _selectedExercise.value = null
        _progressData.value = emptyList()
        viewModelScope.launch {
            _exercises.value = if (group == null) {
                exerciseRepository.getAllExercises().first()
            } else {
                exerciseRepository.getByMuscleGroup(group).first()
            }
        }
    }

    fun selectExercise(exercise: Exercise) {
        _selectedExercise.value = exercise
        viewModelScope.launch {
            progressAnalyzer.getProgressForExerciseAllTime(exercise.id).collect {
                _progressData.value = it
            }
        }
    }

    class Factory(
        private val exerciseRepository: ExerciseRepository,
        private val progressAnalyzer: ProgressAnalyzer
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProgressViewModel(exerciseRepository, progressAnalyzer) as T
        }
    }
}
