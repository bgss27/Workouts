package com.fittrack.app.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.entity.MuscleGroup
import com.fittrack.app.domain.ml.MlAnalysisEngine
import com.fittrack.app.domain.ml.MuscleInsight
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MuscleInsightViewModel(
    private val mlEngine: MlAnalysisEngine
) : ViewModel() {

    private val _allInsights = MutableStateFlow<List<MuscleInsight>>(emptyList())
    val allInsights: StateFlow<List<MuscleInsight>> = _allInsights

    private val _selectedInsight = MutableStateFlow<MuscleInsight?>(null)
    val selectedInsight: StateFlow<MuscleInsight?> = _selectedInsight

    private val _selectedMuscleGroup = MutableStateFlow<MuscleGroup?>(null)
    val selectedMuscleGroup: StateFlow<MuscleGroup?> = _selectedMuscleGroup

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadAllInsights()
    }

    private fun loadAllInsights() {
        viewModelScope.launch {
            _isLoading.value = true
            _allInsights.value = mlEngine.analyzeAllMuscleGroups()
            _isLoading.value = false
        }
    }

    fun selectMuscleGroup(group: MuscleGroup?) {
        _selectedMuscleGroup.value = group
        if (group == null) {
            _selectedInsight.value = null
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _selectedInsight.value = mlEngine.analyzeMuscleGroup(group)
            _isLoading.value = false
        }
    }

    class Factory(
        private val mlEngine: MlAnalysisEngine
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MuscleInsightViewModel(mlEngine) as T
        }
    }
}
