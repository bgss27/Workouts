package com.fittrack.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.entity.Exercise
import com.fittrack.app.data.entity.UserPlan
import com.fittrack.app.data.relation.RoutineWithExercises
import com.fittrack.app.data.relation.WorkoutWithExercises
import com.fittrack.app.data.repository.ExerciseRepository
import com.fittrack.app.data.repository.RoutineRepository
import com.fittrack.app.data.repository.UserPlanRepository
import com.fittrack.app.data.repository.WorkoutRepository
import com.fittrack.app.domain.analysis.ProgressAnalyzer
import com.fittrack.app.domain.model.Suggestion
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PlanDayPreview(
    val userPlan: UserPlan,
    val routine: RoutineWithExercises?,
    val dayLabel: String
)

class HomeViewModel(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val progressAnalyzer: ProgressAnalyzer,
    private val userPlanRepository: UserPlanRepository,
    private val routineRepository: RoutineRepository
) : ViewModel() {

    val recentWorkouts: StateFlow<List<WorkoutWithExercises>> = workoutRepository.getAllWorkouts()
        .map { it.take(5) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedWorkoutCount: StateFlow<Int> = workoutRepository.getCompletedWorkoutCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _suggestions = MutableStateFlow<List<Suggestion>>(emptyList())
    val suggestions: StateFlow<List<Suggestion>> = _suggestions

    private val _exercises = MutableStateFlow<Map<Long, Exercise>>(emptyMap())
    val exercises: StateFlow<Map<Long, Exercise>> = _exercises

    private val _planDays = MutableStateFlow<List<PlanDayPreview>>(emptyList())
    val planDays: StateFlow<List<PlanDayPreview>> = _planDays

    private val _planProgramName = MutableStateFlow<String?>(null)
    val planProgramName: StateFlow<String?> = _planProgramName

    val hasPlan: StateFlow<Boolean> = userPlanRepository.getPlanCount()
        .map { it > 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        viewModelScope.launch {
            _suggestions.value = progressAnalyzer.generateSuggestions().take(5)
        }
        viewModelScope.launch {
            exerciseRepository.getAllExercises().collect { list ->
                _exercises.value = list.associateBy { it.id }
            }
        }
        viewModelScope.launch {
            userPlanRepository.getAllPlans().collect { plans ->
                if (plans.isEmpty()) {
                    _planDays.value = emptyList()
                    _planProgramName.value = null
                    return@collect
                }
                _planProgramName.value = plans.firstOrNull()?.programName
                _planDays.value = plans.map { plan ->
                    val routine = routineRepository.getRoutineById(plan.routineId).first()
                    PlanDayPreview(
                        userPlan = plan,
                        routine = routine,
                        dayLabel = if (plan.programName != null) "Day ${plan.dayOrder}" else routine?.routine?.name ?: "Workout"
                    )
                }
            }
        }
    }

    class Factory(
        private val workoutRepository: WorkoutRepository,
        private val exerciseRepository: ExerciseRepository,
        private val progressAnalyzer: ProgressAnalyzer,
        private val userPlanRepository: UserPlanRepository,
        private val routineRepository: RoutineRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(workoutRepository, exerciseRepository, progressAnalyzer, userPlanRepository, routineRepository) as T
        }
    }
}
