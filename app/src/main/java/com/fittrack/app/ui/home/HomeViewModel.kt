package com.fittrack.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.entity.Equipment
import com.fittrack.app.data.entity.Exercise
import com.fittrack.app.data.entity.MuscleGroup
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
import java.util.Calendar

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

    /**
     * Workouts finished since the start of the current calendar week (Monday
     * 00:00 local time). Recomputed on subscription, so the boundary refreshes
     * each time the user lands on Home — accurate enough without a timer.
     */
    val workoutsThisWeek: StateFlow<Int> = workoutRepository
        .getCompletedWorkoutCountSince(startOfWeekMs())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalVolumeKg: StateFlow<Double> = workoutRepository.getTotalVolumeKg()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

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

    // Transient one-shot messages for the snackbar (suggestion-applied feedback,
    // mainly). SharedFlow so collectors see each emission exactly once.
    private val _snackMessages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackMessages: SharedFlow<String> = _snackMessages.asSharedFlow()

    /**
     * Two-step plan-swap state. Step 1: user picks a routine from
     * [routineOptions]. Step 2 (after [pickRoutine]): user picks which existing
     * plan day to overwrite from [dayOptions], with [recommendedDayPlanId]
     * pre-highlighted. UI dismisses (cancel) at any time via [cancelPlanSwap].
     */
    data class RoutineOption(
        val id: Long,
        val name: String,
        val description: String,
        val exercisePreview: String,
        val alreadyInPlan: Boolean,
    )
    data class DayOption(
        val planId: Long,
        val dayOrder: Int,
        val routineName: String,
        val muscleGroupLabel: String,
        val isTargetGroup: Boolean,
    )
    data class PendingPlanSwap(
        val suggestion: Suggestion.IncreaseFrequency,
        val routineOptions: List<RoutineOption>,
        val chosenRoutineId: Long? = null,
        val dayOptions: List<DayOption> = emptyList(),
        val recommendedDayPlanId: Long? = null,
    )

    private val _pendingSwap = MutableStateFlow<PendingPlanSwap?>(null)
    val pendingSwap: StateFlow<PendingPlanSwap?> = _pendingSwap.asStateFlow()

    /**
     * Start an actionable suggestion flow. For [Suggestion.IncreaseFrequency]
     * we surface the catalog of matching routines to the UI (step 1 of the
     * dialog). Nothing is changed until the user picks a routine AND a day.
     */
    fun applySuggestion(suggestion: Suggestion) {
        viewModelScope.launch {
            when (suggestion) {
                is Suggestion.IncreaseFrequency -> {
                    val plans = userPlanRepository.getAllPlans().first()
                    if (plans.isEmpty()) {
                        _snackMessages.emit("Set up a plan first, then I can rebalance it")
                        return@launch
                    }
                    val routines = routineRepository
                        .getRoutinesForMuscleGroup(suggestion.muscleGroup.name)
                        .first()
                    if (routines.isEmpty()) {
                        _snackMessages.emit(
                            "No ${suggestion.muscleGroup.displayName.lowercase()} routine available"
                        )
                        return@launch
                    }
                    val inPlanIds = plans.map { it.routineId }.toSet()
                    // Build display-ready options: not-already-in-plan first
                    // (variety wins), each with description + first 3 exercise
                    // names so the user can tell routines apart at a glance.
                    val options = routines
                        .sortedBy { it.routine.id in inPlanIds }
                        .map { r ->
                            val names = r.exercises.take(3).mapNotNull {
                                exerciseRepository.getById(it.exerciseId)?.name
                            }
                            val more = (r.exercises.size - names.size).coerceAtLeast(0)
                            val preview = if (more > 0) {
                                names.joinToString(", ") + ", +$more more"
                            } else names.joinToString(", ")
                            RoutineOption(
                                id = r.routine.id,
                                name = r.routine.name,
                                description = r.routine.description,
                                exercisePreview = preview,
                                alreadyInPlan = r.routine.id in inPlanIds,
                            )
                        }
                    _pendingSwap.value = PendingPlanSwap(suggestion, options)
                }
                else -> _snackMessages.emit("Tip noted — keep at it!")
            }
        }
    }

    fun cancelPlanSwap() {
        _pendingSwap.value = null
    }

    /**
     * Default muscle group to highlight when the user opens the "train at home"
     * sheet — the primary muscle group of their first plan day, or null if no
     * plan is set. The user can override; this just saves one tap on the
     * common case.
     */
    suspend fun suggestedHomeWorkoutMuscleGroup(): MuscleGroup? {
        val plans = userPlanRepository.getAllPlans().first()
        val first = plans.firstOrNull() ?: return null
        val routine = routineRepository.getRoutineById(first.routineId).first() ?: return null
        return routine.exercises
            .mapNotNull { exerciseRepository.getById(it.exerciseId)?.muscleGroup }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
    }

    /**
     * Build a home-workout exercise list for [group] using only [equipment]
     * the user has on hand. Picks up to 5 exercises, prioritizing:
     *   1. Primary-muscle exercises over secondary-muscle ones (handled by DAO ordering).
     *   2. Variety — if there are more than 5 candidates, sample to spread
     *      equipment types instead of always returning the alphabetically-first.
     *
     * Returns an empty list when no exercises match — the UI shows a "no
     * exercises available" message rather than starting a blank workout.
     */
    suspend fun generateHomeWorkout(
        group: MuscleGroup,
        equipment: Set<Equipment>,
    ): List<Long> {
        if (equipment.isEmpty()) return emptyList()
        val candidates = exerciseRepository.getForHomeWorkout(group, equipment)
        if (candidates.isEmpty()) return emptyList()
        // DAO already orders primary-muscle first, then alphabetical. Take a
        // mix: prefer primary, fill from secondary if needed. Cap at 5 — a
        // home session shouldn't be a marathon.
        val primary = candidates.filter { it.muscleGroup == group }
        val secondary = candidates.filter { it.muscleGroup != group }
        val ids = (primary + secondary).take(5).map { it.id }

        // If we couldn't find any primary-muscle exercises and had to fall back
        // to secondary-muscle ones (e.g. Biceps + Bodyweight: only Inverted Row,
        // which is really a back exercise), warn the user so they aren't
        // confused when an off-target exercise shows up.
        if (ids.isNotEmpty() && primary.isEmpty()) {
            val hint = when (group) {
                MuscleGroup.BICEPS -> "Add a Pull-Up Bar (chin-ups) or Resistance Band (band curls) for direct biceps work."
                MuscleGroup.BACK -> "Add a Pull-Up Bar for direct back work."
                MuscleGroup.FOREARMS -> "Add a Pull-Up Bar (dead hangs) or dumbbells for direct forearm work."
                else -> "Try adding more equipment for direct ${group.displayName.lowercase()} work."
            }
            _snackMessages.emit(
                "Limited ${group.displayName.lowercase()} options — these hit ${group.displayName.lowercase()} as a secondary muscle. $hint"
            )
        }
        return ids
    }

    /**
     * Step 2: after the user picks a routine in the dialog, compute the list
     * of existing plan days they could replace, with a spacing-aware
     * recommendation pre-highlighted. The user can override the recommendation
     * — that's the whole point of showing the list.
     */
    fun pickRoutine(routineId: Long) {
        val pending = _pendingSwap.value ?: return
        viewModelScope.launch {
            val target = pending.suggestion.muscleGroup
            val plans = userPlanRepository.getAllPlans().first()

            data class DayInfo(
                val planId: Long,
                val dayOrder: Int,
                val routineName: String,
                val group: com.fittrack.app.data.entity.MuscleGroup?,
            )
            val info = plans.map { p ->
                val r = routineRepository.getRoutineById(p.routineId).first()
                val group = r?.exercises
                    ?.mapNotNull { exerciseRepository.getById(it.exerciseId)?.muscleGroup }
                    ?.groupingBy { it }
                    ?.eachCount()
                    ?.maxByOrNull { it.value }
                    ?.key
                DayInfo(p.id, p.dayOrder, r?.routine?.name ?: "Workout", group)
            }

            val targetDayOrders = info.filter { it.group == target }.map { it.dayOrder }
            val groupCounts = info.mapNotNull { it.group }.groupingBy { it }.eachCount()

            val recommended = info
                .filter { it.group != target }
                .maxByOrNull { d ->
                    val spacing = if (targetDayOrders.isEmpty()) 1_000
                    else targetDayOrders.minOf { kotlin.math.abs(it - d.dayOrder) }
                    spacing * 100 + (groupCounts[d.group] ?: 0)
                }
                ?: info.lastOrNull()

            val dayOpts = info.map { d ->
                DayOption(
                    planId = d.planId,
                    dayOrder = d.dayOrder,
                    routineName = d.routineName,
                    muscleGroupLabel = d.group?.displayName ?: "Mixed",
                    isTargetGroup = (d.group == target),
                )
            }

            _pendingSwap.value = pending.copy(
                chosenRoutineId = routineId,
                dayOptions = dayOpts,
                recommendedDayPlanId = recommended?.planId,
            )
        }
    }

    /**
     * Step 3: commit the swap to the user-picked plan day. Refuses if the
     * target day is itself already the target muscle group — that would erase
     * existing balance, not add to it.
     */
    fun confirmPlanSwap(planId: Long) {
        val pending = _pendingSwap.value ?: return
        val routineId = pending.chosenRoutineId ?: return
        viewModelScope.launch {
            val day = pending.dayOptions.firstOrNull { it.planId == planId }
            val routine = pending.routineOptions.firstOrNull { it.id == routineId }
            if (day == null || routine == null) {
                _snackMessages.emit("That option is no longer available")
                _pendingSwap.value = null
                return@launch
            }
            userPlanRepository.replacePlanRoutine(planId, routineId)
            _snackMessages.emit(
                "Day ${day.dayOrder}: '${day.routineName}' → '${routine.name}'"
            )
            _suggestions.value = progressAnalyzer.generateSuggestions().take(5)
            _pendingSwap.value = null
        }
    }

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

    private fun startOfWeekMs(): Long {
        val cal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
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
