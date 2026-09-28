package com.fittrack.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fittrack.app.di.AppModule
import com.fittrack.app.ui.theme.FitTrackTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    appModule: AppModule,
    onViewWorkoutDetail: (Long) -> Unit,
    onStartWorkout: () -> Unit,
    onLogPastWorkout: (Long?) -> Unit,
) {
    val viewModel: CalendarViewModel = viewModel(
        factory = CalendarViewModel.Factory(
            appModule.workoutRepository,
            appModule.userPlanRepository,
        )
    )

    val yearMonth by viewModel.yearMonth.collectAsState()
    val days by viewModel.days.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy") }
    val dayFormatter = remember { DateTimeFormatter.ofPattern("EEEE, MMM d") }

    val selectedDay = remember(selectedDate, days) {
        selectedDate?.let { sel -> days.firstOrNull { it.date == sel } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar") },
                actions = {
                    IconButton(onClick = {
                        // Toolbar `+`: opens log-past form for whatever day is
                        // currently selected (defaults to today).
                        val dateMillis = selectedDate
                            ?.atStartOfDay(java.time.ZoneId.systemDefault())
                            ?.toInstant()?.toEpochMilli()
                        onLogPastWorkout(dateMillis)
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Log past workout")
                    }
                    TextButton(onClick = { viewModel.goToToday() }) {
                        Text("Today")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                MonthHeader(
                    label = yearMonth.format(monthFormatter),
                    onPrev = { viewModel.previousMonth() },
                    onNext = { viewModel.nextMonth() },
                )
            }
            item { WeekdayHeader() }
            item {
                MonthGrid(
                    days = days,
                    selected = selectedDate,
                    onSelect = { viewModel.selectDate(it) },
                )
            }
            item { Legend() }
            item {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
            }
            item {
                SelectedDayPanel(
                    day = selectedDay,
                    dayFormatter = dayFormatter,
                    onViewWorkoutDetail = onViewWorkoutDetail,
                    onStartWorkout = onStartWorkout,
                    onLogPastWorkout = onLogPastWorkout,
                )
            }
        }
    }
}

@Composable
private fun MonthHeader(label: String, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous month")
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next month")
        }
    }
}

@Composable
private fun WeekdayHeader() {
    // Sunday-anchored to match the screenshot reference.
    val labels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        labels.forEach { label ->
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun MonthGrid(
    days: List<CalendarDay>,
    selected: LocalDate?,
    onSelect: (LocalDate) -> Unit,
) {
    if (days.isEmpty()) return
    Column(modifier = Modifier.padding(horizontal = 4.dp)) {
        // 6 rows of 7. List arrives pre-padded to 42.
        for (row in 0 until 6) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val day = days[row * 7 + col]
                    DayCell(
                        day = day,
                        isSelected = day.date == selected,
                        onClick = { onSelect(day.date) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: CalendarDay,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val semantic = FitTrackTheme.colors
    val today = LocalDate.now()
    val isFuture = day.date > today
    val hasWorkout = day.workouts.isNotEmpty()

    // Cell background: subtle status tint that survives both light and dark.
    val bg: Color = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        hasWorkout -> semantic.success.copy(alpha = 0.18f)
        day.isMissed -> semantic.danger.copy(alpha = 0.18f)
        else -> Color.Transparent
    }
    val border = if (day.isToday && !isSelected) {
        MaterialTheme.colorScheme.primary
    } else null

    val textColor: Color = when {
        !day.inMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        isFuture -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .then(if (border != null) Modifier.border(1.5.dp, border, RoundedCornerShape(8.dp)) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                color = textColor,
            )
            Spacer(modifier = Modifier.height(2.dp))
            when {
                hasWorkout -> Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(semantic.success)
                )
                day.isMissed -> Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(semantic.danger)
                )
                else -> Spacer(modifier = Modifier.size(6.dp))
            }
        }
    }
}

@Composable
private fun Legend() {
    val semantic = FitTrackTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendDot(color = semantic.success, label = "Worked out")
        LegendDot(color = semantic.danger, label = "Missed")
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
    }
}

@Composable
private fun SelectedDayPanel(
    day: CalendarDay?,
    dayFormatter: DateTimeFormatter,
    onViewWorkoutDetail: (Long) -> Unit,
    onStartWorkout: () -> Unit,
    onLogPastWorkout: (Long?) -> Unit,
) {
    if (day == null) {
        Text(
            text = "Tap a day to see what you did.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(16.dp),
        )
        return
    }
    val today = LocalDate.now()
    val semantic = FitTrackTheme.colors

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = day.date.format(dayFormatter),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))

        when {
            day.workouts.isNotEmpty() -> {
                day.workouts.forEach { w ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onViewWorkoutDetail(w.workout.id) },
                        colors = CardDefaults.cardColors(
                            containerColor = semantic.success.copy(alpha = 0.12f)
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = semantic.success)
                            Column(modifier = Modifier.weight(1f)) {
                                val durationLabel = w.workout.endTime?.let {
                                    val mins = (it - w.workout.startTime) / 60_000
                                    "${w.exercises.size} exercises · ${mins}min"
                                } ?: "${w.exercises.size} exercises · in progress"
                                Text(
                                    text = "Workout complete",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    text = durationLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                )
                            }
                            TextButton(onClick = { onViewWorkoutDetail(w.workout.id) }) {
                                Text("View")
                            }
                        }
                    }
                }
            }
            day.isMissed -> {
                val dateMs = day.date.atStartOfDay(java.time.ZoneId.systemDefault())
                    .toInstant().toEpochMilli()
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = semantic.danger.copy(alpha = 0.12f)
                    ),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = semantic.danger)
                            Text(
                                text = "Missed — no workout logged",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Button(
                            onClick = { onLogPastWorkout(dateMs) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Log workout for this day")
                        }
                    }
                }
            }
            day.date == today -> {
                val dateMs = day.date.atStartOfDay(java.time.ZoneId.systemDefault())
                    .toInstant().toEpochMilli()
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(Icons.Default.FitnessCenter, contentDescription = null)
                            Text(
                                text = "No workout yet today",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            Button(onClick = onStartWorkout) { Text("Start") }
                        }
                        TextButton(onClick = { onLogPastWorkout(dateMs) }) {
                            Text("Log a past workout for today")
                        }
                    }
                }
            }
            day.date > today -> {
                Text(
                    text = "Upcoming",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
            else -> {
                val dateMs = day.date.atStartOfDay(java.time.ZoneId.systemDefault())
                    .toInstant().toEpochMilli()
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Rest day — no workout logged",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                    OutlinedButton(onClick = { onLogPastWorkout(dateMs) }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Log workout for this day")
                    }
                }
            }
        }
    }
}

