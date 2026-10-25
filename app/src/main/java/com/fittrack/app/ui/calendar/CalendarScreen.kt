package com.fittrack.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
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
import com.fittrack.app.ui.components.IconBadge
import com.fittrack.app.ui.components.ListRow
import com.fittrack.app.ui.components.RowDivider
import com.fittrack.app.ui.components.ScreenGutter
import com.fittrack.app.ui.components.ScreenHeader
import com.fittrack.app.ui.components.Tile
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ScreenHeader(
                title = "Calendar",
                actions = {
                    IconButton(onClick = {
                        // Toolbar `+`: opens log-past form for whatever day is
                        // currently selected (defaults to today).
                        val dateMillis = selectedDate
                            ?.atStartOfDay(java.time.ZoneId.systemDefault())
                            ?.toInstant()?.toEpochMilli()
                        onLogPastWorkout(dateMillis)
                    }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Log past workout",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
            contentPadding = PaddingValues(horizontal = ScreenGutter, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                MonthHeader(
                    label = yearMonth.format(monthFormatter),
                    onPrev = { viewModel.previousMonth() },
                    onNext = { viewModel.nextMonth() },
                )
            }
            item {
                Column {
                    WeekdayHeader()
                    Spacer(modifier = Modifier.height(4.dp))
                    MonthGrid(
                        days = days,
                        selected = selectedDate,
                        onSelect = { viewModel.selectDate(it) },
                    )
                }
            }
            item { Legend() }
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
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrev) {
            Icon(
                Icons.Default.ChevronLeft,
                contentDescription = "Previous month",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        IconButton(onClick = onNext) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Next month",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WeekdayHeader() {
    // Sunday-anchored to match the screenshot reference.
    val labels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    Row(modifier = Modifier.fillMaxWidth()) {
        labels.forEach { label ->
            Text(
                text = label.uppercase(),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        // 6 rows of 7. List arrives pre-padded to 42.
        for (row in 0 until 6) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val day = days[row * 7 + col]
                    DayCell(
                        day = day,
                        isSelected = day.date == selected,
                        onClick = { onSelect(day.date) },
                        modifier = Modifier.weight(1f),
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
    val today = LocalDate.now()
    val isFuture = day.date > today
    val hasWorkout = day.workouts.isNotEmpty()

    val circleBg: Color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val showTodayRing = day.isToday && !isSelected

    val textColor: Color = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        !day.inMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
        isFuture -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }

    val dotColor: Color? = when {
        hasWorkout -> MaterialTheme.colorScheme.primary
        day.isMissed -> MaterialTheme.colorScheme.tertiary
        else -> null
    }

    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(circleBg)
                .then(
                    if (showTodayRing) {
                        Modifier.border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    } else Modifier
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (day.isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = textColor,
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(
                    dotColor?.copy(alpha = if (day.inMonth) 1f else 0.35f) ?: Color.Transparent
                )
        )
    }
}

@Composable
private fun Legend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendDot(color = MaterialTheme.colorScheme.primary, label = "Worked out")
        LegendDot(color = MaterialTheme.colorScheme.tertiary, label = "Missed")
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Compact 40dp primary action used inside the selected-day panel. */
@Composable
private fun CompactPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = MaterialTheme.shapes.small,
        contentPadding = PaddingValues(horizontal = 16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(text, fontWeight = FontWeight.Bold)
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
        Tile {
            Text(
                text = "Tap a day to see what you did.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    val today = LocalDate.now()

    Tile(contentPadding = PaddingValues(vertical = 16.dp)) {
        Text(
            text = day.date.format(dayFormatter),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))

        when {
            day.workouts.isNotEmpty() -> {
                day.workouts.forEachIndexed { index, w ->
                    if (index > 0) RowDivider()
                    val durationLabel = w.workout.endTime?.let {
                        val mins = (it - w.workout.startTime) / 60_000
                        "${w.exercises.size} exercises · ${mins}min"
                    } ?: "${w.exercises.size} exercises · in progress"
                    ListRow(
                        title = "Workout complete",
                        subtitle = durationLabel,
                        icon = Icons.Default.CheckCircle,
                        iconTint = MaterialTheme.colorScheme.primary,
                        onClick = { onViewWorkoutDetail(w.workout.id) },
                        trailing = {
                            TextButton(onClick = { onViewWorkoutDetail(w.workout.id) }) {
                                Text("View")
                            }
                        },
                    )
                }
            }
            day.isMissed -> {
                val dateMs = day.date.atStartOfDay(java.time.ZoneId.systemDefault())
                    .toInstant().toEpochMilli()
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        IconBadge(
                            icon = Icons.Default.ErrorOutline,
                            tint = MaterialTheme.colorScheme.tertiary,
                            size = 36.dp,
                        )
                        Text(
                            text = "Missed — no workout logged",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    CompactPrimaryButton(
                        text = "Log workout for this day",
                        onClick = { onLogPastWorkout(dateMs) },
                        icon = Icons.Default.Add,
                    )
                }
            }
            day.date == today -> {
                val dateMs = day.date.atStartOfDay(java.time.ZoneId.systemDefault())
                    .toInstant().toEpochMilli()
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        IconBadge(
                            icon = Icons.Default.FitnessCenter,
                            tint = MaterialTheme.colorScheme.primary,
                            size = 36.dp,
                        )
                        Text(
                            text = "No workout yet today",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        CompactPrimaryButton(text = "Start", onClick = onStartWorkout)
                    }
                    TextButton(onClick = { onLogPastWorkout(dateMs) }) {
                        Text("Log a past workout for today")
                    }
                }
            }
            day.date > today -> {
                Text(
                    text = "Upcoming",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            else -> {
                val dateMs = day.date.atStartOfDay(java.time.ZoneId.systemDefault())
                    .toInstant().toEpochMilli()
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Rest day — no workout logged",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = { onLogPastWorkout(dateMs) }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Log workout for this day")
                    }
                }
            }
        }
    }
}
