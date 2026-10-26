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

    /** Number of user-created exercises, for the free-tier cap. */
    val customCount: StateFlow<Int> = exerciseRepository.getCustomCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /** One-shot user feedback (e.g. delete refused). */
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    /** Insert or update a custom exercise, then refresh the visible list. */
    fun saveCustom(exercise: Exercise) {
        viewModelScope.launch {
            if (exercise.id == 0L) exerciseRepository.insert(exercise)
            else exerciseRepository.update(exercise)
            reload()
        }
    }

    fun deleteCustom(exercise: Exercise) {
        viewModelScope.launch {
            val removed = exerciseRepository.deleteCustom(exercise)
            if (removed) {
                reload()
            } else {
                _messages.emit("${exercise.name} is used in a workout or routine and can't be deleted.")
            }
        }
    }

    private suspend fun reload() {
        val group = _selectedMuscleGroup.value
        val base = if (group == null) exerciseRepository.getAllExercises().first()
        else exerciseRepository.getByMuscleGroup(group).first()
        applySearch(base)
    }

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
