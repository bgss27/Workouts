package com.fittrack.app.ui.myplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.entity.Exercise
import com.fittrack.app.data.entity.UserPlan
import com.fittrack.app.data.relation.RoutineWithExercises
import com.fittrack.app.data.repository.ExerciseRepository
import com.fittrack.app.data.repository.RoutineRepository
import com.fittrack.app.data.repository.UserPlanRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PlanDayUiState(
    val userPlan: UserPlan,
    val routine: RoutineWithExercises?,
    val dayLabel: String
)

data class MyPlanUiState(
    val programName: String? = null,
    val days: List<PlanDayUiState> = emptyList(),
    val hasPlan: Boolean = false,
    val isLoading: Boolean = true
)

class MyPlanViewModel(
    private val userPlanRepository: UserPlanRepository,
    private val routineRepository: RoutineRepository,
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyPlanUiState())
    val uiState: StateFlow<MyPlanUiState> = _uiState

    private val _exerciseMap = MutableStateFlow<Map<Long, Exercise>>(emptyMap())
    val exerciseMap: StateFlow<Map<Long, Exercise>> = _exerciseMap

    init {
        viewModelScope.launch {
            exerciseRepository.getAllExercises().collect { list ->
                _exerciseMap.value = list.associateBy { it.id }
            }
        }
        viewModelScope.launch {
            userPlanRepository.getAllPlans().collect { plans ->
                if (plans.isEmpty()) {
                    _uiState.value = MyPlanUiState(hasPlan = false, isLoading = false)
                    return@collect
                }

                val programName = plans.firstOrNull()?.programName
                val days = plans.map { plan ->
                    val routine = routineRepository.getRoutineById(plan.routineId).first()
                    val dayLabel = if (programName != null) {
                        "Day ${plan.dayOrder}"
                    } else {
                        routine?.routine?.name ?: "Workout"
                    }
                    PlanDayUiState(
                        userPlan = plan,
                        routine = routine,
                        dayLabel = dayLabel
                    )
                }

                _uiState.value = MyPlanUiState(
                    programName = programName,
                    days = days,
                    hasPlan = true,
                    isLoading = false
                )
            }
        }
    }

    fun clearPlan() {
        viewModelScope.launch {
            userPlanRepository.clearPlan()
        }
    }

    fun removeDay(routineId: Long) {
        viewModelScope.launch {
            userPlanRepository.removeRoutineFromPlan(routineId)
        }
    }

    class Factory(
        private val userPlanRepository: UserPlanRepository,
        private val routineRepository: RoutineRepository,
        private val exerciseRepository: ExerciseRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MyPlanViewModel(userPlanRepository, routineRepository, exerciseRepository) as T
        }
    }
}
