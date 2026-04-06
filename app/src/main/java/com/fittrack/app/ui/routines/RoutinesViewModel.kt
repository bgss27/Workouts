package com.fittrack.app.ui.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.entity.Exercise
import com.fittrack.app.data.entity.MuscleGroup
import com.fittrack.app.data.relation.RoutineWithExercises
import com.fittrack.app.data.repository.ExerciseRepository
import com.fittrack.app.domain.analysis.RoutineSuggestionEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RoutinesViewModel(
    private val routineSuggestionEngine: RoutineSuggestionEngine,
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val _selectedMuscleGroup = MutableStateFlow<MuscleGroup?>(null)
    val selectedMuscleGroup: StateFlow<MuscleGroup?> = _selectedMuscleGroup

    val routines: StateFlow<List<RoutineWithExercises>> = routineSuggestionEngine.getAllRoutines()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _filteredRoutines = MutableStateFlow<List<RoutineWithExercises>>(emptyList())
    val filteredRoutines: StateFlow<List<RoutineWithExercises>> = _filteredRoutines

    private val _routineDetail = MutableStateFlow<RoutineWithExercises?>(null)
    val routineDetail: StateFlow<RoutineWithExercises?> = _routineDetail

    private val _exerciseMap = MutableStateFlow<Map<Long, Exercise>>(emptyMap())
    val exerciseMap: StateFlow<Map<Long, Exercise>> = _exerciseMap

    init {
        viewModelScope.launch {
            routineSuggestionEngine.getAllRoutines().collect { _filteredRoutines.value = it }
        }
        viewModelScope.launch {
            exerciseRepository.getAllExercises().collect { list ->
                _exerciseMap.value = list.associateBy { it.id }
            }
        }
    }

    fun selectMuscleGroup(group: MuscleGroup?) {
        _selectedMuscleGroup.value = group
        viewModelScope.launch {
            if (group == null) {
                routineSuggestionEngine.getAllRoutines().first()
            } else {
                routineSuggestionEngine.getRoutinesForMuscleGroup(group).first()
            }.let { _filteredRoutines.value = it }
        }
    }

    fun loadRoutineDetail(routineId: Long) {
        viewModelScope.launch {
            routineSuggestionEngine.getRoutineById(routineId).collect {
                _routineDetail.value = it
            }
        }
    }

    class Factory(
        private val routineSuggestionEngine: RoutineSuggestionEngine,
        private val exerciseRepository: ExerciseRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RoutinesViewModel(routineSuggestionEngine, exerciseRepository) as T
        }
    }
}
