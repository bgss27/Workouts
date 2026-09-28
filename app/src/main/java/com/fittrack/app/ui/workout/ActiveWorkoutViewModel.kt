package com.fittrack.app.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.entity.Exercise
import com.fittrack.app.data.entity.MuscleGroup
import com.fittrack.app.data.health.HealthConnectService
import com.fittrack.app.data.repository.ExerciseRepository
import com.fittrack.app.data.repository.WorkoutRepository
import com.fittrack.app.domain.analysis.PRResult
import com.fittrack.app.domain.analysis.ProgressAnalyzer
import com.fittrack.app.domain.analysis.RoutineSuggestionEngine
import com.fittrack.app.domain.analysis.TimeBasedGuidance
import com.fittrack.app.domain.analysis.TimeOfDayAdvisor
import com.fittrack.app.ui.components.SetData
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class WorkoutExerciseUiState(
    val workoutExerciseId: Long = 0,
    val exercise: Exercise,
    val sets: List<SetData> = listOf(SetData()),
    /** Non-null when this exercise is part of a superset; shared with peers. */
    val supersetGroup: Long? = null,
)

data class ActiveWorkoutUiState(
    val workoutId: Long = 0,
    val exercises: List<WorkoutExerciseUiState> = emptyList(),
    val isActive: Boolean = false,
    val elapsedSeconds: Long = 0,
    val startTimeMillis: Long = 0,
    /**
     * The weight unit the user typed values in. Snapshotted at workout
     * start — if they change it in Settings mid-session, the in-flight
     * workout keeps using the original unit so we don't reinterpret
     * already-entered numbers.
     */
    val weightUnit: String = "kg",
)

private const val KG_PER_LB = 0.45359237

/** Lifecycle of the finish-workout flow. Drives post-finish navigation. */
sealed class FinishState {
    object NotFinished : FinishState()
    object Saving : FinishState()
    /** Workout saved. [prs] is empty when no 1RM personal records were hit. */
    data class Done(val prs: List<PRResult>) : FinishState()
}

class ActiveWorkoutViewModel(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val routineSuggestionEngine: RoutineSuggestionEngine,
    private val healthConnectService: HealthConnectService,
    private val progressAnalyzer: ProgressAnalyzer,
    private val routineId: Long?,
    /**
     * Hand-picked exercise IDs to seed the workout with — used by the "train
     * at home" generator which creates an ad-hoc routine without persisting
     * one. Ignored when [routineId] is set; otherwise loaded in order.
     */
    private val initialExerciseIds: List<Long>,
    private val initialWeightUnit: String,
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

    private val _finishState = MutableStateFlow<FinishState>(FinishState.NotFinished)
    val finishState: StateFlow<FinishState> = _finishState

    init {
        viewModelScope.launch {
            // Start workout
            val startMillis = System.currentTimeMillis()
            val workoutId = workoutRepository.startWorkout()
            _uiState.update {
                it.copy(
                    workoutId = workoutId,
                    isActive = true,
                    startTimeMillis = startMillis,
                    weightUnit = initialWeightUnit,
                )
            }

            // If started from a routine, pre-populate exercises
            if (routineId != null) {
                routineSuggestionEngine.getRoutineById(routineId).first()?.let { routine ->
                    for (routineExercise in routine.exercises.sortedBy { it.orderIndex }) {
                        val exercise = exerciseRepository.getById(routineExercise.exerciseId) ?: continue
                        addExerciseInternal(workoutId, exercise)
                    }
                }
            } else if (initialExerciseIds.isNotEmpty()) {
                // Ad-hoc workout pre-loaded with generator output (e.g. the
                // "train at home" flow). Preserves the given order.
                for (exerciseId in initialExerciseIds) {
                    val exercise = exerciseRepository.getById(exerciseId) ?: continue
                    addExerciseInternal(workoutId, exercise)
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

    /**
     * Candidate exercises the user could swap the exercise at [forIndex] for:
     * same primary muscle group, minus anything already in this workout
     * (including the target itself).
     */
    fun getSwapCandidates(forIndex: Int): Flow<List<Exercise>> {
        val state = _uiState.value
        if (forIndex !in state.exercises.indices) return flowOf(emptyList())
        val target = state.exercises[forIndex]
        val targetGroup = target.exercise.muscleGroup
        val inWorkoutIds = state.exercises.map { it.exercise.id }.toSet()
        return exerciseRepository.getByMuscleGroup(targetGroup)
            .map { list -> list.filterNot { it.id in inWorkoutIds } }
    }

    /**
     * Replace the exercise at [index] with [newExercise]:
     * - Old workout_exercise row is deleted (sets are still in-memory only,
     *   nothing persisted to lose).
     * - New workout_exercise row inserted at the same orderIndex.
     * - Superset group membership is preserved.
     * - In-memory sets reset to a single empty entry (different movement,
     *   different working weight — keeping old reps/weights would mislead).
     */
    fun swapExercise(index: Int, newExercise: Exercise) {
        viewModelScope.launch {
            val state = _uiState.value
            if (index !in state.exercises.indices) return@launch
            val current = state.exercises[index]
            val workoutId = state.workoutId
            val supersetGroup = current.supersetGroup

            workoutRepository.removeExerciseFromWorkout(current.workoutExerciseId)
            val newId = workoutRepository.addExerciseToWorkout(
                workoutId = workoutId,
                exerciseId = newExercise.id,
                orderIndex = index,
            )
            if (supersetGroup != null) {
                workoutRepository.updateSupersetGroup(newId, supersetGroup)
            }

            _uiState.update { st ->
                val updated = st.exercises.toMutableList()
                updated[index] = WorkoutExerciseUiState(
                    workoutExerciseId = newId,
                    exercise = newExercise,
                    sets = listOf(SetData()),
                    supersetGroup = supersetGroup,
                )
                st.copy(exercises = updated)
            }
        }
    }

    fun removeExercise(index: Int) {
        viewModelScope.launch {
            val exercises = _uiState.value.exercises
            if (index !in exercises.indices) return@launch
            val target = exercises[index]
            val originalGroup = target.supersetGroup
            workoutRepository.removeExerciseFromWorkout(target.workoutExerciseId)

            val updated = exercises.toMutableList().also { it.removeAt(index) }

            // Don't leave an orphan single-member superset behind.
            if (originalGroup != null) {
                val peers = updated.withIndex()
                    .filter { (_, ex) -> ex.supersetGroup == originalGroup }
                if (peers.size == 1) {
                    val (peerIdx, peer) = peers.first()
                    workoutRepository.updateSupersetGroup(peer.workoutExerciseId, null)
                    updated[peerIdx] = peer.copy(supersetGroup = null)
                }
            }

            _uiState.update { state -> state.copy(exercises = updated) }
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

    /**
     * Toggle superset membership for the exercise at [exerciseIndex]:
     *
     * - Already in a group → remove from that group. If only one peer remains
     *   afterwards, ungroup the peer too (no orphan single-element groups).
     * - Not in a group → join (or start) a group with the previous exercise.
     *   No-op if this is the first exercise in the workout.
     *
     * Group IDs are arbitrary [Long] identifiers; we use the
     * [WorkoutExercise.id] of the first member as a stable, unique value.
     */
    fun toggleSuperset(exerciseIndex: Int) {
        viewModelScope.launch {
            val exercises = _uiState.value.exercises
            if (exerciseIndex !in exercises.indices) return@launch
            val current = exercises[exerciseIndex]
            val originalGroup = current.supersetGroup
            val updated = exercises.toMutableList()

            if (originalGroup != null) {
                // Ungroup current.
                workoutRepository.updateSupersetGroup(current.workoutExerciseId, null)
                updated[exerciseIndex] = current.copy(supersetGroup = null)

                // If exactly one peer remains, ungroup it too.
                val remainingPeers = updated.withIndex()
                    .filter { (_, ex) -> ex.supersetGroup == originalGroup }
                if (remainingPeers.size == 1) {
                    val (peerIdx, peer) = remainingPeers.first()
                    workoutRepository.updateSupersetGroup(peer.workoutExerciseId, null)
                    updated[peerIdx] = peer.copy(supersetGroup = null)
                }
            } else {
                if (exerciseIndex == 0) return@launch
                val previous = exercises[exerciseIndex - 1]
                val groupId = previous.supersetGroup ?: previous.workoutExerciseId
                if (previous.supersetGroup == null) {
                    workoutRepository.updateSupersetGroup(previous.workoutExerciseId, groupId)
                    updated[exerciseIndex - 1] = previous.copy(supersetGroup = groupId)
                }
                workoutRepository.updateSupersetGroup(current.workoutExerciseId, groupId)
                updated[exerciseIndex] = current.copy(supersetGroup = groupId)
            }

            _uiState.update { it.copy(exercises = updated) }
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
            _finishState.value = FinishState.Saving

            // Save all sets to database
            val isLbs = _uiState.value.weightUnit == "lbs"
            for (exerciseState in _uiState.value.exercises) {
                var setNumber = 0
                for (set in exerciseState.sets) {
                    val reps = set.reps.toIntOrNull() ?: continue
                    val typedWeight = set.weight.toDoubleOrNull() ?: continue
                    if (reps <= 0) continue
                    if (!set.isWarmup) setNumber++
                    // Storage is always kg; convert if user typed in lbs.
                    val weightKg = if (isLbs) typedWeight * KG_PER_LB else typedWeight
                    workoutRepository.addSet(
                        workoutExerciseId = exerciseState.workoutExerciseId,
                        setNumber = if (set.isWarmup) 0 else setNumber,
                        reps = reps,
                        weightKg = weightKg,
                        isWarmup = set.isWarmup,
                        rpe = if (set.isWarmup) null else set.rpe,
                    )
                }
            }
            val endMillis = System.currentTimeMillis()
            val startMillis = _uiState.value.startTimeMillis
            val workoutId = _uiState.value.workoutId
            workoutRepository.finishWorkout(workoutId)

            // Mirror to Health Connect. Service no-ops if disabled / not granted.
            healthConnectService.writeWorkout(
                startTimeMillis = startMillis,
                endTimeMillis = endMillis,
                title = "Strength training",
            )

            // PR detection runs after the workout's sets are persisted so it
            // sees them. Failures here mustn't block the finish flow — wrap.
            val prs = runCatching { progressAnalyzer.detectPRs(workoutId) }.getOrDefault(emptyList())

            _uiState.update { it.copy(isActive = false) }
            _finishState.value = FinishState.Done(prs)
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
        private val healthConnectService: HealthConnectService,
        private val progressAnalyzer: ProgressAnalyzer,
        private val routineId: Long?,
        private val initialExerciseIds: List<Long>,
        private val initialWeightUnit: String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ActiveWorkoutViewModel(
                workoutRepository,
                exerciseRepository,
                routineSuggestionEngine,
                healthConnectService,
                progressAnalyzer,
                routineId,
                initialExerciseIds,
                initialWeightUnit,
            ) as T
        }
    }
}
