package com.fittrack.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.relation.WorkoutWithExercises
import com.fittrack.app.data.repository.UserPlanRepository
import com.fittrack.app.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class CalendarDay(
    val date: LocalDate,
    val inMonth: Boolean,
    val isToday: Boolean,
    val workouts: List<WorkoutWithExercises>,
    val isMissed: Boolean,
)

class CalendarViewModel(
    workoutRepository: WorkoutRepository,
    userPlanRepository: UserPlanRepository,
) : ViewModel() {

    private val _yearMonth = MutableStateFlow(YearMonth.now())
    val yearMonth: StateFlow<YearMonth> = _yearMonth.asStateFlow()

    private val _selectedDate = MutableStateFlow<LocalDate?>(LocalDate.now())
    val selectedDate: StateFlow<LocalDate?> = _selectedDate.asStateFlow()

    /** All completed/in-progress workouts grouped by their local-date start. */
    private val workoutsByDate: StateFlow<Map<LocalDate, List<WorkoutWithExercises>>> =
        workoutRepository.getAllWorkouts()
            .map { list ->
                list.groupBy { Instant.ofEpochMilli(it.workout.startTime).atZone(ZoneId.systemDefault()).toLocalDate() }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /** Number of routines in the user's plan — the "expected workouts per week" target. */
    private val planSize: StateFlow<Int> = userPlanRepository.getPlanCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val days: StateFlow<List<CalendarDay>> = combine(
        _yearMonth,
        workoutsByDate,
        planSize,
    ) { ym, byDate, plan ->
        buildGrid(ym, byDate, plan)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun previousMonth() {
        _yearMonth.value = _yearMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        _yearMonth.value = _yearMonth.value.plusMonths(1)
    }

    fun goToToday() {
        val today = LocalDate.now()
        _yearMonth.value = YearMonth.from(today)
        _selectedDate.value = today
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    /**
     * Build a 6-row × 7-column grid. The grid always starts on Sunday of the
     * week containing the 1st of the month — mirroring the macOS Calendar
     * layout the user asked for. Each cell is enriched with workout list and
     * a derived "missed" flag.
     */
    private fun buildGrid(
        ym: YearMonth,
        byDate: Map<LocalDate, List<WorkoutWithExercises>>,
        plan: Int,
    ): List<CalendarDay> {
        val today = LocalDate.now()
        val firstOfMonth = ym.atDay(1)

        // Sunday-anchored grid (DayOfWeek.SUNDAY.value = 7; ISO Monday = 1).
        // Pull back to the most recent Sunday on or before the 1st.
        val gridStart = firstOfMonth.minusDays(((firstOfMonth.dayOfWeek.value) % 7).toLong())

        val missed = computeMissedDates(byDate, plan, today)

        return (0 until 42).map { offset ->
            val date = gridStart.plusDays(offset.toLong())
            CalendarDay(
                date = date,
                inMonth = YearMonth.from(date) == ym,
                isToday = date == today,
                workouts = byDate[date].orEmpty(),
                isMissed = date in missed,
            )
        }
    }

    /**
     * Weekly-shortfall missed-day rule.
     *
     * For each ISO week (Mon–Sun) that has already ended before this week
     * started, if the user logged fewer workouts than their plan size, mark
     * the FIRST N empty days of that week as missed (where N = shortfall).
     * Picking the first-N keeps marks on weekdays for typical 3-5 day plans,
     * avoiding "you missed Sunday" guilt-trips.
     *
     * Bounded to the last 8 weeks and never marks days before the user's first
     * workout — no point flagging dates from before they started using the app.
     */
    private fun computeMissedDates(
        byDate: Map<LocalDate, List<WorkoutWithExercises>>,
        plan: Int,
        today: LocalDate,
    ): Set<LocalDate> {
        if (plan <= 0 || byDate.isEmpty()) return emptySet()
        val firstEver = byDate.keys.min()
        val currentWeekStart = today.with(DayOfWeek.MONDAY)
        val out = mutableSetOf<LocalDate>()
        for (weeksAgo in 1..8) {
            val weekStart = currentWeekStart.minusWeeks(weeksAgo.toLong())
            if (weekStart.plusDays(6) < firstEver) continue
            val week = (0..6).map { weekStart.plusDays(it.toLong()) }
            val done = week.count { it in byDate }
            val shortfall = plan - done
            if (shortfall <= 0) continue
            week.filter { it !in byDate }
                .take(shortfall)
                .forEach { out += it }
        }
        return out
    }

    class Factory(
        private val workoutRepository: WorkoutRepository,
        private val userPlanRepository: UserPlanRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CalendarViewModel(workoutRepository, userPlanRepository) as T
    }
}
