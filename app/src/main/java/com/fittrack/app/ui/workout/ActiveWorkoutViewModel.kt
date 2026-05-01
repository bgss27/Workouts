package com.fittrack.app.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.entity.Exercise
import com.fittrack.app.data.entity.MuscleGroup
import com.fittrack.app.data.repository.ExerciseRepository
import com.fittrack.app.data.repository.WorkoutRepository
import com.fittrack.app.domain.analysis.RoutineSuggestionEngine
import com.fittrack.app.domain.analysis.TimeBasedGuidance
import com.fittrack.app.domain.analysis.TimeOfDayAdvisor
import com.fittrack.app.ui.components.SetData
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class WorkoutExerciseUiState(
    val workoutExerciseId: Long = 0,
    val exercise: Exercise,
    val sets: List<SetData> = listOf(SetData())
)

data class ActiveWorkoutUiState(
    val workoutId: Long = 0,
    val exercises: List<WorkoutExerciseUiState> = emptyList(),
    val isActive: Boolean = false,
    val elapsedSeconds: Long = 0
)

class ActiveWorkoutViewModel(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val routineSuggestionEngine: RoutineSuggestionEngine,
    private val routineId: Long?
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState

    val timeGuidance: TimeBasedGuidance = TimeOfDayAdvisor.getGuidance()

    private val _availableExercises = MutableStateFlow<List<Exercise>>(emptyList())
    val availableExercises: StateFlow<List<Exercise>> = _availableExercises

    private val _selectedMuscleGroup = MutableStateFlow<MuscleGroup?>(null)
    val selectedMuscleGroup: StateFlow<MuscleGroup?> = _selectedMuscleGroup

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    init {
        viewModelScope.launch {
            // Start workout
            val workoutId = workoutRepository.startWorkout()
            _uiState.update { it.copy(workoutId = workoutId, isActive = true) }

            // If started from a routine, pre-populate exercises
            if (routineId != null) {
                routineSuggestionEngine.getRoutineById(routineId).first()?.let { routine ->
                    for (routineExercise in routine.exercises.sortedBy { it.orderIndex }) {
                        val exercise = exerciseRepository.getById(routineExercise.exerciseId) ?: continue
                        addExerciseInternal(workoutId, exercise)
                    }
                }
            }
        }

        // Load exercises
        viewModelScope.launch {
            exerciseRepository.getAllExercises().collect { _availableExercises.value = it }
        }
    }

    fun filterByMuscleGroup(group: MuscleGroup?) {
        _selectedMuscleGroup.value = group
        viewModelScope.launch {
            if (group == null) {
                exerciseRepository.getAllExercises().first().let { _availableExercises.value = it }
            } else {
                exerciseRepository.getByMuscleGroup(group).first().let { _availableExercises.value = it }
            }
        }
    }

    fun searchExercises(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            if (query.isBlank()) {
                val group = _selectedMuscleGroup.value
                if (group == null) exerciseRepository.getAllExercises().first()
                else exerciseRepository.getByMuscleGroup(group).first()
            } else {
                exerciseRepository.searchByName(query).first()
            }.let { _availableExercises.value = it }
        }
    }

    fun addExercise(exercise: Exercise) {
        viewModelScope.launch {
            addExerciseInternal(_uiState.value.workoutId, exercise)
        }
    }

    private suspend fun addExerciseInternal(workoutId: Long, exercise: Exercise) {
        val orderIndex = _uiState.value.exercises.size
        val workoutExerciseId = workoutRepository.addExerciseToWorkout(
            workoutId, exercise.id, orderIndex
        )
        _uiState.update { state ->
            state.copy(
                exercises = state.exercises + WorkoutExerciseUiState(
                    workoutExerciseId = workoutExerciseId,
                    exercise = exercise,
                    sets = listOf(SetData())
                )
            )
        }
    }

    fun removeExercise(index: Int) {
        viewModelScope.launch {
            val exercise = _uiState.value.exercises[index]
            workoutRepository.removeExerciseFromWorkout(exercise.workoutExerciseId)
            _uiState.update { state ->
                state.copy(exercises = state.exercises.toMutableList().also { it.removeAt(index) })
            }
        }
    }

    fun addSet(exerciseIndex: Int) {
        _uiState.update { state ->
            val exercises = state.exercises.toMutableList()
            val exercise = exercises[exerciseIndex]
            exercises[exerciseIndex] = exercise.copy(sets = exercise.sets + SetData())
            state.copy(exercises = exercises)
        }
    }

    fun updateSet(exerciseIndex: Int, setIndex: Int, setData: SetData) {
        _uiState.update { state ->
            val exercises = state.exercises.toMutableList()
            val exercise = exercises[exerciseIndex]
            val sets = exercise.sets.toMutableList()
            sets[setIndex] = setData
            exercises[exerciseIndex] = exercise.copy(sets = sets)
            state.copy(exercises = exercises)
        }
    }

    fun deleteSet(exerciseIndex: Int, setIndex: Int) {
        _uiState.update { state ->
            val exercises = state.exercises.toMutableList()
            val exercise = exercises[exerciseIndex]
            val sets = exercise.sets.toMutableList()
            if (sets.size > 1) {
                sets.removeAt(setIndex)
                exercises[exerciseIndex] = exercise.copy(sets = sets)
            }
            state.copy(exercises = exercises)
        }
    }

    fun finishWorkout() {
        viewModelScope.launch {
            // Save all sets to database
            for (exerciseState in _uiState.value.exercises) {
                var setNumber = 0
                for (set in exerciseState.sets) {
                    val reps = set.reps.toIntOrNull() ?: continue
                    val weight = set.weight.toDoubleOrNull() ?: continue
                    if (reps <= 0) continue
                    if (!set.isWarmup) setNumber++
                    workoutRepository.addSet(
                        workoutExerciseId = exerciseState.workoutExerciseId,
                        setNumber = if (set.isWarmup) 0 else setNumber,
                        reps = reps,
                        weightKg = weight,
                        isWarmup = set.isWarmup
                    )
                }
            }
            workoutRepository.finishWorkout(_uiState.value.workoutId)
            _uiState.update { it.copy(isActive = false) }
        }
    }

    fun discardWorkout() {
        viewModelScope.launch {
            workoutRepository.deleteWorkout(_uiState.value.workoutId)
            _uiState.update { it.copy(isActive = false) }
        }
    }

    class Factory(
        private val workoutRepository: WorkoutRepository,
        private val exerciseRepository: ExerciseRepository,
        private val routineSuggestionEngine: RoutineSuggestionEngine,
        private val routineId: Long?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ActiveWorkoutViewModel(workoutRepository, exerciseRepository, routineSuggestionEngine, routineId) as T
        }
    }
}
