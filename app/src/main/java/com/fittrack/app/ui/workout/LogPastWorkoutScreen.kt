package com.fittrack.app.ui.workout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fittrack.app.data.WeightUnits
import com.fittrack.app.data.entity.MuscleGroup
import com.fittrack.app.di.AppModule
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Form to log a workout that already happened. Mirrors the iOS
 * `LogPastWorkoutView`.
 *
 * Entry points: Calendar tab (top-bar `+` action) and individual empty-day
 * cards in the calendar's selected-day panel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogPastWorkoutScreen(
    appModule: AppModule,
    initialDateMillis: Long?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val viewModel: LogPastWorkoutViewModel = viewModel(
        factory = LogPastWorkoutViewModel.Factory(
            appModule.workoutRepository,
            appModule.exerciseRepository,
            initialDateMillis,
        )
    )

    val state by viewModel.ui.collectAsState()
    val canSave by viewModel.canSave.collectAsState()
    val context = LocalContext.current
    val unit = remember { WeightUnits.preferred(context) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showExercisePicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log Past Workout") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.save(weightUnitIsLbs = unit == WeightUnits.LBS) },
                        enabled = canSave,
                    ) { Text("Save") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("When", style = MaterialTheme.typography.labelLarge)
            }
            item { WhenCard(
                date = state.date,
                startTime = state.startTime,
                endTime = state.endTime,
                onDate = { showDatePicker = true },
                onStart = { showStartTimePicker = true },
                onEnd = { showEndTimePicker = true },
            ) }
            item {
                Text("Exercises", style = MaterialTheme.typography.labelLarge)
            }
            if (state.entries.isEmpty()) {
                item {
                    Text(
                        text = "Add the exercises you did. Tap Add Exercise below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }
            items(state.entries, key = { it.id }) { entry ->
                PastExerciseCard(
                    entry = entry,
                    weightUnit = unit,
                    onAddSet = { viewModel.addSet(entry.id) },
                    onRemoveSet = { setId -> viewModel.removeSet(entry.id, setId) },
                    onUpdateSet = { setId, transform -> viewModel.updateSet(entry.id, setId, transform) },
                    onRemoveExercise = { viewModel.removeExercise(entry.id) },
                )
            }
            item {
                Button(
                    onClick = { showExercisePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Exercise")
                }
            }
            item {
                Text("Notes (optional)", style = MaterialTheme.typography.labelLarge)
            }
            item {
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = viewModel::setNotes,
                    placeholder = { Text("How did it go?") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 5,
                )
            }
        }
    }

    // Pickers — Material3 already-built widgets.
    if (showDatePicker) {
        PastDatePickerSheet(
            initial = state.date,
            onDismiss = { showDatePicker = false },
            onConfirm = { date -> viewModel.setDate(date); showDatePicker = false },
        )
    }
    if (showStartTimePicker) {
        PastTimePickerSheet(
            initial = state.startTime,
            onDismiss = { showStartTimePicker = false },
            onConfirm = { time -> viewModel.setStartTime(time); showStartTimePicker = false },
        )
    }
    if (showEndTimePicker) {
        PastTimePickerSheet(
            initial = state.endTime,
            onDismiss = { showEndTimePicker = false },
            onConfirm = { time -> viewModel.setEndTime(time); showEndTimePicker = false },
        )
    }
    if (showExercisePicker) {
        ExercisePickerSheet(
            state = state,
            onGroupChange = viewModel::setPickerMuscleGroup,
            onQueryChange = viewModel::setPickerQuery,
            onPick = { ex ->
                viewModel.addExercise(ex)
                showExercisePicker = false
            },
            onDismiss = { showExercisePicker = false },
        )
    }

    state.saveError?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissError() },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissError() }) { Text("OK") }
            },
            title = { Text("Couldn't save") },
            text = { Text(msg) },
        )
    }
}

@Composable
private fun WhenCard(
    date: LocalDate,
    startTime: LocalTime,
    endTime: LocalTime,
    onDate: () -> Unit,
    onStart: () -> Unit,
    onEnd: () -> Unit,
) {
    val dateFmt = remember { DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy") }
    val timeFmt = remember { DateTimeFormatter.ofPattern("h:mm a") }
    val mins = run {
        val s = startTime.toSecondOfDay()
        val e = endTime.toSecondOfDay()
        val span = if (e >= s) e - s else (24 * 3600) - s + e
        span / 60
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ClickableRow("Date", date.format(dateFmt), onClick = onDate)
            HorizontalDivider()
            ClickableRow("Start", startTime.format(timeFmt), onClick = onStart)
            HorizontalDivider()
            ClickableRow("End", endTime.format(timeFmt), onClick = onEnd)
            Text(
                text = "Duration: ${mins}min",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun ClickableRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun PastExerciseCard(
    entry: PastExerciseEntry,
    weightUnit: String,
    onAddSet: () -> Unit,
    onRemoveSet: (String) -> Unit,
    onUpdateSet: (String, (PastSetEntry) -> PastSetEntry) -> Unit,
    onRemoveExercise: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(entry.exercise.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        entry.exercise.muscleGroup.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
                IconButton(onClick = onRemoveExercise) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove exercise")
                }
            }
            entry.sets.forEachIndexed { index, set ->
                val workingNumber = entry.sets.subList(0, index + 1).count { !it.isWarmup }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (set.isWarmup) "W" else "$workingNumber",
                        modifier = Modifier.width(20.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (set.isWarmup) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                    )
                    OutlinedTextField(
                        value = set.weight,
                        onValueChange = { v -> onUpdateSet(set.id) { it.copy(weight = v) } },
                        placeholder = { Text(weightUnit) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = set.reps,
                        onValueChange = { v -> onUpdateSet(set.id) { it.copy(reps = v) } },
                        placeholder = { Text("Reps") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { onUpdateSet(set.id) { it.copy(isWarmup = !it.isWarmup) } }) {
                        Text(
                            "W",
                            color = if (set.isWarmup) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        )
                    }
                    IconButton(onClick = { onRemoveSet(set.id) }) {
                        Icon(Icons.Default.Close, contentDescription = "Remove set")
                    }
                }
            }
            TextButton(onClick = onAddSet) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add set")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PastDatePickerSheet(
    initial: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val zone = java.time.ZoneId.systemDefault()
    val initialMs = initial.atStartOfDay(zone).toInstant().toEpochMilli()
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMs,
        // Cap to today — no logging future workouts.
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val today = LocalDate.now().atStartOfDay(zone).toInstant().toEpochMilli()
                return utcTimeMillis <= today
            }
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                pickerState.selectedDateMillis?.let { ms ->
                    val date = java.time.Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
                    onConfirm(date)
                } ?: onDismiss()
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    ) {
        DatePicker(state = pickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PastTimePickerSheet(
    initial: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = false,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onConfirm(LocalTime.of(state.hour, state.minute))
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Pick a time") },
        text = { TimePicker(state = state) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExercisePickerSheet(
    state: LogPastUiState,
    onGroupChange: (MuscleGroup?) -> Unit,
    onQueryChange: (String) -> Unit,
    onPick: (com.fittrack.app.data.entity.Exercise) -> Unit,
    onDismiss: () -> Unit,
) {
    val filtered = remember(state.availableExercises, state.pickerMuscleGroup, state.pickerQuery) {
        state.availableExercises.filter { ex ->
            val mg = state.pickerMuscleGroup == null || ex.muscleGroup == state.pickerMuscleGroup
            val q = state.pickerQuery.isBlank() || ex.name.contains(state.pickerQuery, ignoreCase = true)
            mg && q
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text("Add Exercise", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.pickerQuery,
                onValueChange = onQueryChange,
                placeholder = { Text("Search…") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = state.pickerMuscleGroup == null,
                        onClick = { onGroupChange(null) },
                        label = { Text("All") }
                    )
                }
                items(MuscleGroup.entries.toTypedArray()) { g ->
                    FilterChip(
                        selected = state.pickerMuscleGroup == g,
                        onClick = { onGroupChange(if (state.pickerMuscleGroup == g) null else g) },
                        label = { Text(g.displayName) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(filtered, key = { it.id }) { exercise ->
                    ListItem(
                        headlineContent = { Text(exercise.name) },
                        supportingContent = { Text(exercise.muscleGroup.displayName, style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(exercise) },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
