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

    private val _allExercises = MutableStateFlow<List<Exercise>>(emptyList())

    /** Ids of exercises that have at least one logged working set. */
    private val _historyIds = MutableStateFlow<Set<Long>>(emptySet())

    /**
     * Exercises shown in the picker: only those with logged history, further
     * narrowed by the selected muscle group. The full catalog is ~800 entries
     * and only ones with sets can chart anything.
     */
    val exercises: StateFlow<List<Exercise>> = combine(
        _allExercises, _selectedMuscleGroup, _historyIds,
    ) { all, group, history ->
        all.filter { it.id in history && (group == null || it.muscleGroup == group) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedExercise = MutableStateFlow<Exercise?>(null)
    val selectedExercise: StateFlow<Exercise?> = _selectedExercise

    private val _progressData = MutableStateFlow<List<ProgressDataPoint>>(emptyList())
    val progressData: StateFlow<List<ProgressDataPoint>> = _progressData

    /**
     * Per-exercise 1RM sparkline data. Map keyed by exercise.id; each value
     * is a recent-history list (≤ 8 points) of estimated-1RM values, oldest
     * to newest. Populated lazily as exercises load — only the visible
     * exercises matter, but the dataset is small enough that bulk-fetch
     * up front keeps the UI snappy.
     */
    private val _sparklineData = MutableStateFlow<Map<Long, List<Double>>>(emptyMap())
    val sparklineData: StateFlow<Map<Long, List<Double>>> = _sparklineData

    init {
        viewModelScope.launch {
            exerciseRepository.getAllExercises().collect { list ->
                _allExercises.value = list
                refreshSparklines(list)
            }
        }
    }

    private suspend fun refreshSparklines(@Suppress("UNUSED_PARAMETER") exercises: List<Exercise>) {
        // One query covers every exercise with history; the catalog list is
        // irrelevant since exercises without sets have no sparkline.
        val progress = progressAnalyzer.getProgressForAllExercises()
        _historyIds.value = progress.keys
        _sparklineData.value = progress
            .filterValues { it.size >= 2 }
            .mapValues { (_, points) -> points.takeLast(8).map { it.estimated1RM } }
    }

    fun selectMuscleGroup(group: MuscleGroup?) {
        _selectedMuscleGroup.value = group
        _selectedExercise.value = null
        _progressData.value = emptyList()
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
