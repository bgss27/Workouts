package com.fittrack.app.ui.suggestions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.entity.MuscleGroup
import com.fittrack.app.domain.analysis.ExerciseSuggestion
import com.fittrack.app.domain.analysis.ProgressAnalyzer
import com.fittrack.app.domain.analysis.RoutineSuggestionEngine
import com.fittrack.app.domain.model.Suggestion
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SuggestionsViewModel(
    private val progressAnalyzer: ProgressAnalyzer,
    private val routineSuggestionEngine: RoutineSuggestionEngine
) : ViewModel() {

    private val _selectedMuscleGroup = MutableStateFlow<MuscleGroup?>(null)
    val selectedMuscleGroup: StateFlow<MuscleGroup?> = _selectedMuscleGroup

    private val _suggestions = MutableStateFlow<List<Suggestion>>(emptyList())
    val suggestions: StateFlow<List<Suggestion>> = _suggestions

    private val _exerciseSuggestions = MutableStateFlow<List<ExerciseSuggestion>>(emptyList())
    val exerciseSuggestions: StateFlow<List<ExerciseSuggestion>> = _exerciseSuggestions

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadAllSuggestions()
    }

    private fun loadAllSuggestions() {
        viewModelScope.launch {
            _isLoading.value = true
            _suggestions.value = progressAnalyzer.generateSuggestions()
            _isLoading.value = false
        }
    }

    fun selectMuscleGroup(group: MuscleGroup?) {
        _selectedMuscleGroup.value = group
        viewModelScope.launch {
            _isLoading.value = true
            if (group == null) {
                _suggestions.value = progressAnalyzer.generateSuggestions()
                _exerciseSuggestions.value = emptyList()
            } else {
                _suggestions.value = progressAnalyzer.getSuggestionsForMuscleGroup(group)
                _exerciseSuggestions.value = routineSuggestionEngine.suggestExercisesForMuscleGroup(group)
            }
            _isLoading.value = false
        }
    }

    class Factory(
        private val progressAnalyzer: ProgressAnalyzer,
        private val routineSuggestionEngine: RoutineSuggestionEngine
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SuggestionsViewModel(progressAnalyzer, routineSuggestionEngine) as T
        }
    }
}
