package com.fittrack.app.ui.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.entity.Exercise
import com.fittrack.app.data.entity.MuscleGroup
import com.fittrack.app.data.repository.ExerciseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ExercisesViewModel(
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val _selectedMuscleGroup = MutableStateFlow<MuscleGroup?>(null)
    val selectedMuscleGroup: StateFlow<MuscleGroup?> = _selectedMuscleGroup

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises

    val groupedExercises: StateFlow<Map<MuscleGroup, List<Exercise>>> = _exercises
        .map { list -> list.groupBy { it.muscleGroup } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        viewModelScope.launch {
            exerciseRepository.getAllExercises().collect { _exercises.value = it }
        }
    }

    fun selectMuscleGroup(group: MuscleGroup?) {
        _selectedMuscleGroup.value = group
        viewModelScope.launch {
            val list = if (group == null) {
                exerciseRepository.getAllExercises().first()
            } else {
                exerciseRepository.getByMuscleGroup(group).first()
            }
            applySearch(list)
        }
    }

    fun search(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            val base = if (_selectedMuscleGroup.value == null) {
                exerciseRepository.getAllExercises().first()
            } else {
                exerciseRepository.getByMuscleGroup(_selectedMuscleGroup.value!!).first()
            }
            applySearch(base)
        }
    }

    private fun applySearch(list: List<Exercise>) {
        val q = _searchQuery.value
        _exercises.value = if (q.isBlank()) list
        else list.filter { it.name.contains(q, ignoreCase = true) }
    }

    class Factory(
        private val exerciseRepository: ExerciseRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ExercisesViewModel(exerciseRepository) as T
        }
    }
}
