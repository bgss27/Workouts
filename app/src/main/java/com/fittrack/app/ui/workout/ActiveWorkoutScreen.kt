package com.fittrack.app.ui.workout

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.fittrack.app.data.WeightUnits
import com.fittrack.app.data.repository.ExerciseImageRepository
import com.fittrack.app.domain.analysis.WarmupCatalog
import com.fittrack.app.domain.analysis.WarmupSection
import com.fittrack.app.di.AppModule
import com.fittrack.app.ui.components.ExerciseCard
import com.fittrack.app.ui.components.MuscleGroupChipRow
import com.fittrack.app.ui.components.PlateCalculatorSheet
import com.fittrack.app.ui.components.RestTimerChip
import com.fittrack.app.ui.components.RestTimerSheet
import kotlinx.coroutines.delay

/** Default rest length auto-started when a working set is logged. */
private const val DEFAULT_REST_SECONDS = 90L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    appModule: AppModule,
    routineId: Long?,
    initialExerciseIds: List<Long> = emptyList(),
    onWorkoutComplete: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    // Snapshot the user's preferred weight unit at workout start so flipping
    // the Settings toggle mid-session doesn't reinterpret already-entered
    // numbers.
    val initialWeightUnit = remember { WeightUnits.preferred(context) }
    val viewModel: ActiveWorkoutViewModel = viewModel(
        factory = ActiveWorkoutViewModel.Factory(
            appModule.workoutRepository,
            appModule.exerciseRepository,
            appModule.routineSuggestionEngine,
            appModule.healthConnectService,
            appModule.progressAnalyzer,
            routineId,
            initialExerciseIds,
            initialWeightUnit,
        )
    )

    val uiState by viewModel.uiState.collectAsState()
    val availableExercises by viewModel.availableExercises.collectAsState()
    val selectedMuscleGroup by viewModel.selectedMuscleGroup.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val guidance = viewModel.timeGuidance

    var showExercisePicker by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showFinishDialog by remember { mutableStateOf(false) }
    var showTipsExpanded by remember { mutableStateOf(false) }
    // Per-workout dismissal of the warmup banner. Resets when the user starts
    // a new workout — rememberSaveable so screen rotation / process death
    // don't bring it back uninvited.
    var warmupDismissed by rememberSaveable { mutableStateOf(false) }
    // When non-null, the swap-exercise dialog is open for the exercise at
    // this index. Resets on workout end (state lives in the screen, not the VM).
    var swapForIndex by remember { mutableStateOf<Int?>(null) }

    // Rest timer + plate calc state. Timer state lives here (not in the sheet)
    // so it keeps counting and the top-bar chip stays visible after dismiss.
    var restTimerEndsAtMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var showRestTimerSheet by remember { mutableStateOf(false) }
    var showPlateCalcSheet by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    // Accordion: only one exercise card is expanded at a time. Defaults to the
    // first exercise; auto-advances to the next exercise that still has
    // incomplete working sets when the user finishes the current one. A null
    // value means the user explicitly collapsed everything and we don't
    // re-open without an explicit tap.
    var expandedIndex by rememberSaveable { mutableStateOf<Int?>(0) }
    LaunchedEffect(uiState.exercises) {
        if (uiState.exercises.isEmpty()) {
            expandedIndex = null
            return@LaunchedEffect
        }
        val cur = expandedIndex
        if (cur == null) return@LaunchedEffect
        if (cur !in uiState.exercises.indices) {
            expandedIndex = uiState.exercises.lastIndex
            return@LaunchedEffect
        }
        val working = uiState.exercises[cur].sets.filter { !it.isWarmup }
        val allDone = working.isNotEmpty() && working.all { it.isCompleted }
        if (allDone) {
            val next = ((cur + 1)..uiState.exercises.lastIndex).firstOrNull { i ->
                val ws = uiState.exercises[i].sets.filter { !it.isWarmup }
                ws.isEmpty() || ws.any { !it.isCompleted }
            }
            if (next != null) expandedIndex = next
        }
    }

    // Buzz + beep once when the timer hits 0. ToneGenerator on STREAM_MUSIC
    // routes through whatever output is currently active — so the beep plays
    // through connected Bluetooth/wired earbuds instead of the phone speaker
    // when they're in use. Brief tone (200ms) so it cues without disrupting
    // any music the user is listening to.
    LaunchedEffect(restTimerEndsAtMillis) {
        val end = restTimerEndsAtMillis ?: return@LaunchedEffect
        val delayMs = end - System.currentTimeMillis()
        if (delayMs > 0) delay(delayMs)
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        runCatching {
            val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 90)
            tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 250)
            // Release after the tone finishes so we don't leak the audio
            // resource. The tone itself plays asynchronously.
            delay(300)
            tone.release()
        }
    }

    // Finish flow: if the user finished a workout with one or more PRs, show
    // the celebration; otherwise navigate back as soon as the workout is saved.
    val finishState by viewModel.finishState.collectAsState()
    val done = finishState as? FinishState.Done
    LaunchedEffect(done) {
        if (done != null && done.prs.isEmpty()) {
            onWorkoutComplete()
        }
    }
    if (done != null && done.prs.isNotEmpty()) {
        PostWorkoutCelebration(
            prs = done.prs,
            displayUnit = uiState.weightUnit,
            onDismiss = onWorkoutComplete,
        )
    }

    // Intercept system back so the user can't accidentally orphan a workout
    // by gesture/hardware back. Same dialog as tapping the top-bar back arrow.
    BackHandler { showDiscardDialog = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout") },
                navigationIcon = {
                    IconButton(onClick = { showDiscardDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val activeEndsAt = restTimerEndsAtMillis
                    if (activeEndsAt != null) {
                        RestTimerChip(
                            endsAtMillis = activeEndsAt,
                            onClick = { showRestTimerSheet = true }
                        )
                    } else {
                        IconButton(onClick = { showRestTimerSheet = true }) {
                            Icon(Icons.Default.Timer, contentDescription = "Rest timer")
                        }
                    }
                    IconButton(onClick = { showPlateCalcSheet = true }) {
                        Icon(Icons.Default.Calculate, contentDescription = "Plate calculator")
                    }
                    TextButton(onClick = { showFinishDialog = true }) {
                        Text("Finish")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showExercisePicker = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add exercise")
            }
        }
    ) { padding ->
        if (uiState.exercises.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.FitnessCenter,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Add exercises to get started")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { showExercisePicker = true }) {
                        Text("Add Exercise")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Time-of-day guidance banner (with warmup recommendations).
                // Hidden entirely once the user dismisses it for this workout.
                if (!warmupDismissed) item {
                    val bannerColor = when (guidance.timeOfDay) {
                        com.fittrack.app.domain.analysis.TimeOfDay.MORNING -> MaterialTheme.colorScheme.tertiaryContainer
                        com.fittrack.app.domain.analysis.TimeOfDay.AFTERNOON -> MaterialTheme.colorScheme.secondaryContainer
                        com.fittrack.app.domain.analysis.TimeOfDay.EVENING -> MaterialTheme.colorScheme.primaryContainer
                    }
                    val bannerIcon = when (guidance.timeOfDay) {
                        com.fittrack.app.domain.analysis.TimeOfDay.MORNING -> Icons.Default.WbSunny
                        com.fittrack.app.domain.analysis.TimeOfDay.AFTERNOON -> Icons.Default.WbCloudy
                        com.fittrack.app.domain.analysis.TimeOfDay.EVENING -> Icons.Default.NightsStay
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = bannerColor),
                        onClick = { showTipsExpanded = !showTipsExpanded }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(bannerIcon, contentDescription = null, modifier = Modifier.size(20.dp))
                                Text(guidance.tip, style = MaterialTheme.typography.labelMedium)
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(
                                    if (showTipsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                IconButton(
                                    onClick = { warmupDismissed = true },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Skip warmup",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            if (showTipsExpanded) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = guidance.warmupAdvice.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )

                                // Concrete warmup exercises for this workout's muscles
                                val warmupSections = remember(uiState.exercises) {
                                    val groups = uiState.exercises
                                        .map { it.exercise.muscleGroup }
                                        .distinct()
                                    WarmupCatalog.forMuscleGroups(groups)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                warmupSections.forEach { section ->
                                    WarmupSectionView(section)
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Other tips",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                )
                                guidance.detailedTips.forEach { tip ->
                                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                        Text("  •  ", style = MaterialTheme.typography.bodySmall)
                                        Text(tip, style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                    }
                                }
                                if (guidance.intensityModifier < 1.0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Suggested intensity: ${(guidance.intensityModifier * 100).toInt()}% of your usual weight",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(
                                    onClick = { warmupDismissed = true },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Skip warmup")
                                }
                            }
                        }
                    }
                }

                itemsIndexed(uiState.exercises) { index, exerciseState ->
                    ExerciseCard(
                        exerciseName = exerciseState.exercise.name,
                        muscleGroup = exerciseState.exercise.muscleGroup.displayName,
                        sets = exerciseState.sets,
                        weightUnit = uiState.weightUnit,
                        inSuperset = exerciseState.supersetGroup != null,
                        canToggleSuperset = index > 0,
                        onSupersetToggle = { viewModel.toggleSuperset(index) },
                        onSwap = { swapForIndex = index },
                        onAddSet = { viewModel.addSet(index) },
                        onUpdateSet = { setIndex, setData -> viewModel.updateSet(index, setIndex, setData) },
                        onDeleteSet = { setIndex -> viewModel.deleteSet(index, setIndex) },
                        onRemoveExercise = { viewModel.removeExercise(index) },
                        expanded = (index == expandedIndex),
                        onExpandToggle = {
                            expandedIndex = if (expandedIndex == index) null else index
                        },
                        onSetCompleted = {
                            // Any completed working set restarts the rest timer
                            // to the full default. If the user is moving through
                            // a superset / new exercise mid-rest, that's the
                            // signal their previous rest is over.
                            restTimerEndsAtMillis =
                                System.currentTimeMillis() + DEFAULT_REST_SECONDS * 1000L
                        },
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    // Swap Exercise Dialog — appears when user taps the swap icon on a card.
    swapForIndex?.let { idx ->
        if (idx in uiState.exercises.indices) {
            val target = uiState.exercises[idx].exercise
            val candidates by viewModel.getSwapCandidates(idx).collectAsState(initial = emptyList())
            AlertDialog(
                onDismissRequest = { swapForIndex = null },
                title = { Text("Swap ${target.name}") },
                text = {
                    Column {
                        Text(
                            "Pick a different ${target.muscleGroup.displayName.lowercase()} " +
                                "exercise not already in this workout.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (candidates.isEmpty()) {
                            Text(
                                "No alternatives available — this workout already includes every " +
                                    "${target.muscleGroup.displayName.lowercase()} exercise we have.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        } else {
                            LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                                items(candidates.size) { i ->
                                    val candidate = candidates[i]
                                    val thumb = remember(candidate.name) {
                                        ExerciseImageRepository.getExerciseImages(candidate.name).firstOrNull()
                                    }
                                    ListItem(
                                        headlineContent = { Text(candidate.name) },
                                        supportingContent = {
                                            Text(
                                                buildString {
                                                    append(candidate.muscleGroup.displayName)
                                                    candidate.secondaryMuscleGroup?.let {
                                                        append(" / ${it.displayName}")
                                                    }
                                                }
                                            )
                                        },
                                        leadingContent = {
                                            if (thumb != null) {
                                                AsyncImage(
                                                    model = thumb,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(40.dp),
                                                )
                                            } else {
                                                Icon(
                                                    Icons.Default.FitnessCenter,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                                    modifier = Modifier.size(40.dp),
                                                )
                                            }
                                        },
                                        modifier = Modifier.clickable {
                                            viewModel.swapExercise(idx, candidate)
                                            swapForIndex = null
                                        },
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { swapForIndex = null }) { Text("Cancel") }
                },
            )
        }
    }

    // Exercise Picker Dialog
    if (showExercisePicker) {
        AlertDialog(
            onDismissRequest = { showExercisePicker = false },
            title = { Text("Add Exercise") },
            text = {
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchExercises(it) },
                        label = { Text("Search exercises") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                    )

                    MuscleGroupChipRow(
                        selectedGroup = selectedMuscleGroup,
                        onGroupSelected = { viewModel.filterByMuscleGroup(it) }
                    )

                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(availableExercises.size) { index ->
                            val exercise = availableExercises[index]
                            val thumbUrl = remember(exercise.name) {
                                ExerciseImageRepository.getExerciseImages(exercise.name).firstOrNull()
                            }
                            ListItem(
                                headlineContent = { Text(exercise.name) },
                                supportingContent = {
                                    Text(
                                        buildString {
                                            append(exercise.muscleGroup.displayName)
                                            exercise.secondaryMuscleGroup?.let {
                                                append(" / ${it.displayName}")
                                            }
                                        }
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addExercise(exercise)
                                        showExercisePicker = false
                                    },
                                leadingContent = {
                                    if (thumbUrl != null) {
                                        AsyncImage(
                                            model = thumbUrl,
                                            contentDescription = null,
                                            modifier = Modifier.size(40.dp)
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.FitnessCenter,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                },
                                trailingContent = {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Add",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExercisePicker = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Discard Dialog
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard Workout?") },
            text = { Text("All progress will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    viewModel.discardWorkout()
                    onBack()
                }) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Keep Going")
                }
            }
        )
    }

    // Rest Timer Sheet
    if (showRestTimerSheet) {
        RestTimerSheet(
            endsAtMillis = restTimerEndsAtMillis,
            onStartTimer = { restTimerEndsAtMillis = it },
            onCancelTimer = { restTimerEndsAtMillis = null },
            onDismiss = { showRestTimerSheet = false }
        )
    }

    // Plate Calculator Sheet
    if (showPlateCalcSheet) {
        PlateCalculatorSheet(
            onDismiss = { showPlateCalcSheet = false },
            defaultUnit = if (uiState.weightUnit == WeightUnits.LBS)
                com.fittrack.app.ui.components.PlateUnit.LB
            else com.fittrack.app.ui.components.PlateUnit.KG,
        )
    }

    // Finish Dialog
    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("Finish Workout?") },
            text = { Text("Save workout with ${uiState.exercises.size} exercise(s)?") },
            confirmButton = {
                TextButton(onClick = {
                    showFinishDialog = false
                    viewModel.finishWorkout()
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun WarmupSectionView(section: WarmupSection) {
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = section.title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(modifier = Modifier.height(2.dp))
    section.exercises.forEach { exercise ->
        Row(modifier = Modifier.padding(vertical = 1.dp)) {
            Text(
                text = "  •  ",
                style = MaterialTheme.typography.bodySmall,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                )
                Text(
                    text = exercise.prescription +
                        (exercise.note?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                )
            }
        }
    }
}
