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
import com.fittrack.app.data.WeightUnits
import com.fittrack.app.data.WorkoutDraftStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import com.fittrack.app.domain.analysis.StreakCalculator
import com.fittrack.app.share.ShareSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class WorkoutExerciseUiState(
    val workoutExerciseId: Long = 0,
    val exercise: Exercise,
    val sets: List<SetData> = listOf(SetData()),
    /** Non-null when this exercise is part of a superset; shared with peers. */
    val supersetGroup: Long? = null,
    /** User explicitly marked this exercise done for the session. */
    val isFinished: Boolean = false,
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
    /**
     * Workout saved. [prs] is empty when no 1RM personal records were hit.
     * [summary] feeds the celebration dialog and the share card; null when
     * nothing was logged (the session was dropped rather than recorded).
     */
    data class Done(val prs: List<PRResult>, val summary: ShareSummary? = null) : FinishState()
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
    private val draftStore: WorkoutDraftStore,
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

    /**
     * Completes once the workout row exists. Every mutation that needs
     * [ActiveWorkoutUiState.workoutId] joins this first so a tap in the
     * first few hundred ms can't write rows against workoutId = 0.
     */
    private val startup: kotlinx.coroutines.Job

    init {
        startup = viewModelScope.launch {
            // A plain "Quick Workout" entry resumes an interrupted session if
            // one was drafted (process death while the phone was locked, a
            // crash, etc.). Routine / generator entries always start fresh.
            val explicitStart = routineId != null || initialExerciseIds.isNotEmpty()
            if (!explicitStart && restoreDraft()) {
                startTicker()
                return@launch
            }
            draftStore.clear()

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
            startTicker()

            // If started from a routine, pre-populate exercises
            if (routineId != null) {
                routineSuggestionEngine.getRoutineById(routineId).first()?.let { routine ->
                    for (routineExercise in routine.exercises.sortedBy { it.orderIndex }) {
                        val exercise = exerciseRepository.getById(routineExercise.exerciseId) ?: continue
                        // Pre-fill the prescribed number of set rows (e.g. 4x6-8
                        // shows four rows) so the user logs instead of adding.
                        addExerciseInternal(workoutId, exercise, routineExercise.suggestedSets.coerceIn(1, 10))
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

    /** Number of user-created exercises, for the free-tier cap in the picker. */
    val customExerciseCount: StateFlow<Int> = exerciseRepository.getCustomCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /** Persist a new custom exercise and add it to this workout in one step. */
    fun createAndAddExercise(exercise: Exercise) {
        viewModelScope.launch {
            val id = exerciseRepository.insert(exercise)
            startup.join()
            addExerciseInternal(_uiState.value.workoutId, exercise.copy(id = id))
        }
    }

    fun addExercise(exercise: Exercise) {
        viewModelScope.launch {
            startup.join()
            addExerciseInternal(_uiState.value.workoutId, exercise)
        }
    }

    /**
     * Rebuild UI state from the saved draft when its workout row is still
     * open. Returns false (and drops a stale draft) when nothing matches.
     */
    private suspend fun restoreDraft(): Boolean {
        val draft = draftStore.load() ?: return false
        val active = workoutRepository.getActiveWorkout()
        if (active == null || active.id != draft.workoutId) {
            draftStore.clear()
            return false
        }
        val exercises = draft.exercises.mapNotNull { ex ->
            val exercise = exerciseRepository.getById(ex.exerciseId) ?: return@mapNotNull null
            WorkoutExerciseUiState(
                workoutExerciseId = ex.workoutExerciseId,
                exercise = exercise,
                sets = ex.sets.map { s ->
                    SetData(reps = s.reps, weight = s.weight, isWarmup = s.isWarmup, rpe = s.rpe, isCompleted = s.isCompleted)
                }.ifEmpty { listOf(SetData()) },
                supersetGroup = ex.supersetGroup,
                isFinished = ex.isFinished,
            )
        }
        _uiState.update {
            it.copy(
                workoutId = active.id,
                exercises = exercises,
                isActive = true,
                startTimeMillis = active.startTime,
                weightUnit = draft.weightUnit,
            )
        }
        return true
    }

    private var draftJob: Job? = null

    /** Mirror the current state to disk, debounced so typing doesn't thrash. */
    private fun persistDraft() {
        draftJob?.cancel()
        draftJob = viewModelScope.launch {
            delay(250)
            val state = _uiState.value
            if (!state.isActive || state.workoutId == 0L) return@launch
            draftStore.save(
                WorkoutDraftStore.Draft(
                    workoutId = state.workoutId,
                    weightUnit = state.weightUnit,
                    exercises = state.exercises.map { ex ->
                        WorkoutDraftStore.DraftExercise(
                            workoutExerciseId = ex.workoutExerciseId,
                            exerciseId = ex.exercise.id,
                            supersetGroup = ex.supersetGroup,
                            sets = ex.sets.map { s ->
                                WorkoutDraftStore.DraftSet(s.reps, s.weight, s.isWarmup, s.rpe, s.isCompleted)
                            },
                            isFinished = ex.isFinished,
                        )
                    },
                )
            )
        }
    }

    private suspend fun addExerciseInternal(workoutId: Long, exercise: Exercise, setRows: Int = 1) {
        val orderIndex = _uiState.value.exercises.size
        val workoutExerciseId = workoutRepository.addExerciseToWorkout(
            workoutId, exercise.id, orderIndex
        )
        _uiState.update { state ->
            state.copy(
                exercises = state.exercises + WorkoutExerciseUiState(
                    workoutExerciseId = workoutExerciseId,
                    exercise = exercise,
                    sets = List(setRows.coerceAtLeast(1)) { SetData() }
                )
            )
        }
        persistDraft()
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
            startup.join()
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
            persistDraft()
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
            persistDraft()
        }
    }

    fun addSet(exerciseIndex: Int) {
        _uiState.update { state ->
            val exercises = state.exercises.toMutableList()
            val exercise = exercises[exerciseIndex]
            exercises[exerciseIndex] = exercise.copy(sets = exercise.sets + SetData())
            state.copy(exercises = exercises)
        }
        persistDraft()
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
        persistDraft()
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
            persistDraft()
        }
    }

    /** Flip the explicit "done with this exercise" mark. */
    fun toggleExerciseFinished(exerciseIndex: Int) {
        _uiState.update { state ->
            if (exerciseIndex !in state.exercises.indices) return@update state
            val exercises = state.exercises.toMutableList()
            val ex = exercises[exerciseIndex]
            exercises[exerciseIndex] = ex.copy(isFinished = !ex.isFinished)
            state.copy(exercises = exercises)
        }
        persistDraft()
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
        persistDraft()
    }

    /**
     * Drives [ActiveWorkoutUiState.elapsedSeconds] once per second while the
     * workout is active. Derived from wall-clock start time rather than
     * counting ticks, so backgrounding the app doesn't drift the clock.
     */
    private fun startTicker() {
        viewModelScope.launch {
            while (_uiState.value.isActive) {
                val start = _uiState.value.startTimeMillis
                if (start > 0) {
                    _uiState.update {
                        it.copy(elapsedSeconds = (System.currentTimeMillis() - start) / 1000)
                    }
                }
                kotlinx.coroutines.delay(1_000)
            }
        }
    }

    fun finishWorkout() {
        // Re-entry guard: a second tap while saving must not write the sets
        // again (double volume, duplicate Health Connect session).
        if (_finishState.value != FinishState.NotFinished) return
        _finishState.value = FinishState.Saving
        viewModelScope.launch {
            startup.join()

            // Save all sets to database
            val isLbs = _uiState.value.weightUnit == "lbs"
            var savedSets = 0
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
                    savedSets++
                }
            }
            val endMillis = System.currentTimeMillis()
            val startMillis = _uiState.value.startTimeMillis
            val workoutId = _uiState.value.workoutId

            // Nothing logged: don't record a completed workout that would
            // inflate counters and the calendar. Drop the row and leave.
            draftJob?.cancel()
            draftStore.clear()
            if (savedSets == 0) {
                workoutRepository.deleteWorkout(workoutId)
                _uiState.update { it.copy(isActive = false) }
                _finishState.value = FinishState.Done(emptyList())
                return@launch
            }
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
            val summary = runCatching { buildShareSummary(startMillis, endMillis, savedSets, prs) }.getOrNull()

            _uiState.update { it.copy(isActive = false) }
            _finishState.value = FinishState.Done(prs, summary)
        }
    }

    /** Snapshot the finished session for the celebration dialog and share card. */
    private suspend fun buildShareSummary(
        startMillis: Long,
        endMillis: Long,
        savedSets: Int,
        prs: List<PRResult>,
    ): ShareSummary {
        val unit = _uiState.value.weightUnit
        val exercises = _uiState.value.exercises
        val volumeTyped = exercises.sumOf { ex ->
            ex.sets.filter { !it.isWarmup }.sumOf { s ->
                (s.weight.toDoubleOrNull() ?: 0.0) * (s.reps.toIntOrNull() ?: 0)
            }
        }
        val volumeText = when {
            volumeTyped >= 10_000 -> "%.0fk".format(volumeTyped / 1000)
            volumeTyped >= 1_000 -> "%.1fk".format(volumeTyped / 1000)
            else -> "%.0f".format(volumeTyped)
        } + " " + unit
        val lines = exercises.map { ex ->
            val n = ex.sets.count { !it.isWarmup && it.reps.toIntOrNull() != null && it.weight.toDoubleOrNull() != null }
            "${ex.exercise.name} · $n ${if (n == 1) "set" else "sets"}"
        }
        val completed = workoutRepository.getAllWorkouts().first()
            .filter { it.workout.endTime != null }
            .map { it.workout.startTime }
        val streak = StreakCalculator.compute(completed)
        return ShareSummary(
            headline = when {
                prs.size > 1 -> "${prs.size} new PRs!"
                prs.size == 1 -> "New PR!"
                else -> "Workout complete"
            },
            dateText = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(startMillis)),
            durationMinutes = ((endMillis - startMillis) / 60_000L).coerceAtLeast(1),
            exerciseCount = exercises.size,
            setCount = savedSets,
            volumeText = volumeText,
            exerciseLines = lines,
            prLines = prs.map { "${it.exerciseName} ${WeightUnits.format(it.newBest1RM, unit)}" },
            streakWeeks = streak.currentWeeks,
        )
    }

    fun discardWorkout() {
        viewModelScope.launch {
            draftJob?.cancel()
            draftStore.clear()
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
        private val draftStore: WorkoutDraftStore,
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
                draftStore,
            ) as T
        }
    }
}
