package com.fittrack.app.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.entity.Exercise
import com.fittrack.app.data.entity.MuscleGroup
import com.fittrack.app.data.repository.ExerciseRepository
import com.fittrack.app.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

/**
 * State for one exercise in the manual-log form. UI-only — does NOT persist
 * until [LogPastWorkoutViewModel.save].
 */
data class PastExerciseEntry(
    val id: String = UUID.randomUUID().toString(),
    val exercise: Exercise,
    val sets: List<PastSetEntry> = listOf(PastSetEntry()),
)

data class PastSetEntry(
    val id: String = UUID.randomUUID().toString(),
    val weight: String = "",
    val reps: String = "",
    val isWarmup: Boolean = false,
)

data class LogPastUiState(
    val date: LocalDate = LocalDate.now(),
    val startTime: LocalTime = LocalTime.now().minusHours(1),
    val endTime: LocalTime = LocalTime.now(),
    val notes: String = "",
    val entries: List<PastExerciseEntry> = emptyList(),
    val availableExercises: List<Exercise> = emptyList(),
    val pickerMuscleGroup: MuscleGroup? = null,
    val pickerQuery: String = "",
    val saveError: String? = null,
    val saved: Boolean = false,
)

class LogPastWorkoutViewModel(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    initialDateMillis: Long?,
) : ViewModel() {

    private val _ui = MutableStateFlow(
        LogPastUiState(
            date = initialDateMillis
                ?.takeIf { it > 0 }
                ?.let { java.time.Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
                ?: LocalDate.now(),
        )
    )
    val ui: StateFlow<LogPastUiState> = _ui

    init {
        viewModelScope.launch {
            exerciseRepository.getAllExercises().collect { list ->
                _ui.update { it.copy(availableExercises = list) }
            }
        }
    }

    fun setDate(date: LocalDate) { _ui.update { it.copy(date = date) } }
    fun setStartTime(time: LocalTime) { _ui.update { it.copy(startTime = time) } }
    fun setEndTime(time: LocalTime) { _ui.update { it.copy(endTime = time) } }
    fun setNotes(notes: String) { _ui.update { it.copy(notes = notes) } }
    fun setPickerMuscleGroup(group: MuscleGroup?) { _ui.update { it.copy(pickerMuscleGroup = group) } }
    fun setPickerQuery(query: String) { _ui.update { it.copy(pickerQuery = query) } }

    fun addExercise(exercise: Exercise) {
        _ui.update { it.copy(entries = it.entries + PastExerciseEntry(exercise = exercise)) }
    }

    fun removeExercise(entryId: String) {
        _ui.update { it.copy(entries = it.entries.filterNot { e -> e.id == entryId }) }
    }

    fun addSet(entryId: String) {
        _ui.update { state ->
            state.copy(entries = state.entries.map { e ->
                if (e.id == entryId) e.copy(sets = e.sets + PastSetEntry()) else e
            })
        }
    }

    fun removeSet(entryId: String, setId: String) {
        _ui.update { state ->
            state.copy(entries = state.entries.map { e ->
                if (e.id == entryId) e.copy(sets = e.sets.filterNot { it.id == setId }) else e
            })
        }
    }

    fun updateSet(entryId: String, setId: String, transform: (PastSetEntry) -> PastSetEntry) {
        _ui.update { state ->
            state.copy(entries = state.entries.map { e ->
                if (e.id != entryId) e
                else e.copy(sets = e.sets.map { s -> if (s.id == setId) transform(s) else s })
            })
        }
    }

    /**
     * Whether the Save button should be enabled. Requires at least one
     * exercise with at least one valid working set (weight + reps > 0).
     */
    val canSave: StateFlow<Boolean> = ui.map { state ->
        state.entries.isNotEmpty() && state.entries.all { e ->
            e.sets.any { s ->
                (s.reps.toIntOrNull() ?: 0) > 0 && (s.weight.toDoubleOrNull() ?: 0.0) > 0
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    /**
     * Persist the workout. Combines [LogPastUiState.date] with [startTime] /
     * [endTime] to derive epoch millis. If endTime is "earlier" than startTime
     * (e.g. workout that crossed midnight: 11:00 PM → 12:30 AM), endTime is
     * bumped to the next day.
     *
     * Weight values are interpreted in [weightUnitIsLbs] — converted to kg
     * before storage, matching the live-workout pipeline.
     */
    fun save(weightUnitIsLbs: Boolean) {
        val state = _ui.value
        if (state.entries.isEmpty()) {
            _ui.update { it.copy(saveError = "Add at least one exercise") }
            return
        }
        val zone = ZoneId.systemDefault()
        val startMillis = state.date.atTime(state.startTime).atZone(zone).toInstant().toEpochMilli()
        var endMillis = state.date.atTime(state.endTime).atZone(zone).toInstant().toEpochMilli()
        if (endMillis <= startMillis) endMillis += 24L * 60 * 60 * 1000

        viewModelScope.launch {
            val workoutId = workoutRepository.insertPastWorkout(
                startTime = startMillis,
                endTime = endMillis,
                notes = state.notes.ifBlank { null },
            )
            state.entries.forEachIndexed { exerciseIndex, entry ->
                val workoutExerciseId = workoutRepository.addExerciseToWorkout(
                    workoutId = workoutId,
                    exerciseId = entry.exercise.id,
                    orderIndex = exerciseIndex,
                )
                var setNumber = 0
                for (set in entry.sets) {
                    val reps = set.reps.toIntOrNull() ?: continue
                    val typed = set.weight.toDoubleOrNull() ?: continue
                    if (reps <= 0 || typed <= 0) continue
                    if (!set.isWarmup) setNumber++
                    val weightKg = if (weightUnitIsLbs) typed * 0.45359237 else typed
                    workoutRepository.addSet(
                        workoutExerciseId = workoutExerciseId,
                        setNumber = if (set.isWarmup) 0 else setNumber,
                        reps = reps,
                        weightKg = weightKg,
                        isWarmup = set.isWarmup,
                    )
                }
            }
            _ui.update { it.copy(saved = true) }
        }
    }

    fun dismissError() { _ui.update { it.copy(saveError = null) } }

    class Factory(
        private val workoutRepository: WorkoutRepository,
        private val exerciseRepository: ExerciseRepository,
        private val initialDateMillis: Long?,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LogPastWorkoutViewModel(
                workoutRepository,
                exerciseRepository,
                initialDateMillis,
            ) as T
        }
    }
}
