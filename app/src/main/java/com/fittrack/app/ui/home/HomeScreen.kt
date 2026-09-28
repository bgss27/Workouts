package com.fittrack.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.filled.SelfImprovement
import com.fittrack.app.data.EquipmentPrefs
import com.fittrack.app.data.WeightUnits
import com.fittrack.app.data.entity.MuscleGroup
import com.fittrack.app.di.AppModule
import com.fittrack.app.domain.model.Suggestion
import com.fittrack.app.ui.components.EmptyState
import com.fittrack.app.ui.components.SuggestionCard
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    appModule: AppModule,
    proManager: com.fittrack.app.billing.ProManager,
    onStartWorkout: () -> Unit,
    onViewHistory: () -> Unit,
    onViewProgress: () -> Unit,
    onViewSuggestions: () -> Unit,
    onViewWorkoutDetail: (Long) -> Unit,
    onViewMyPlan: () -> Unit,
    onStartRoutine: (Long) -> Unit,
    onViewMlInsights: () -> Unit,
    onUpgrade: () -> Unit,
    onStartHomeWorkout: (List<Long>) -> Unit,
) {
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(
            appModule.workoutRepository,
            appModule.exerciseRepository,
            appModule.progressAnalyzer,
            appModule.userPlanRepository,
            appModule.routineRepository
        )
    )

    val recentWorkouts by viewModel.recentWorkouts.collectAsState()
    val workoutCount by viewModel.completedWorkoutCount.collectAsState()
    val workoutsThisWeek by viewModel.workoutsThisWeek.collectAsState()
    val totalVolumeKg by viewModel.totalVolumeKg.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val hasPlan by viewModel.hasPlan.collectAsState()
    val planDays by viewModel.planDays.collectAsState()
    val planProgramName by viewModel.planProgramName.collectAsState()
    val isPro by proManager.isPro.collectAsState()

    val context = LocalContext.current
    val unit = remember { WeightUnits.preferred(context) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.snackMessages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val pendingSwap by viewModel.pendingSwap.collectAsState()

    // "Train at home" sheet state. defaultGroup is refreshed each time the
    // sheet opens so changes to the user's plan are picked up.
    val coroutineScope = rememberCoroutineScope()
    var showHomeSheet by remember { mutableStateOf(false) }
    var homeSheetDefaultGroup by remember { mutableStateOf<MuscleGroup?>(null) }
    LaunchedEffect(showHomeSheet) {
        if (showHomeSheet) {
            homeSheetDefaultGroup = viewModel.suggestedHomeWorkoutMuscleGroup()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("FitTrack") },
                actions = {
                    IconButton(onClick = onViewProgress) {
                        Icon(Icons.Default.ShowChart, contentDescription = "Progress")
                    }
                    IconButton(onClick = onViewMlInsights) {
                        Icon(Icons.Default.Psychology, contentDescription = "Insights")
                    }
                    IconButton(onClick = onViewHistory) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Secondary action: build a workout from what the user has at
                // home. Labeled "Home Workout" (not just an icon) so it can't
                // be confused with the bottom-nav Home destination.
                ExtendedFloatingActionButton(
                    onClick = { showHomeSheet = true },
                    icon = { Icon(Icons.Default.SelfImprovement, contentDescription = null) },
                    text = { Text("Home Workout") },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                ExtendedFloatingActionButton(
                    onClick = onStartWorkout,
                    icon = { Icon(Icons.Default.FitnessCenter, contentDescription = null) },
                    text = { Text("Quick Workout") }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // My Plan Section
            if (hasPlan && planDays.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "My Plan",
                                style = MaterialTheme.typography.titleMedium
                            )
                            planProgramName?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        TextButton(onClick = onViewMyPlan) {
                            Text("View All")
                        }
                    }
                }

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(planDays) { index, day ->
                            Card(
                                modifier = Modifier.width(200.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = day.dayLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = day.routine?.routine?.name ?: "Workout",
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${day.routine?.exercises?.size ?: 0} exercises",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            day.routine?.routine?.id?.let { onStartRoutine(it) }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Start", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // No plan - prompt to set one up
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onViewMyPlan() },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.EventNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Set up your workout plan",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Choose a 3-day or 5-day program to follow",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            // Stats Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Your Progress",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            HomeStat(value = "$workoutCount", label = "Total")
                            HomeStat(value = "$workoutsThisWeek", label = "This Week")
                            HomeStat(
                                value = formatVolume(totalVolumeKg, unit),
                                label = "Volume ($unit)"
                            )
                        }
                    }
                }
            }

            // Pro upgrade banner (only shown to free users)
            if (!isPro) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onUpgrade() },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Upgrade to Pro",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Unlock ML insights, all programs, and more",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                                )
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            // Suggestions
            if (suggestions.isNotEmpty()) {
                item {
                    Text(
                        text = "Improvement Tips",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                items(suggestions.take(3)) { suggestion ->
                    val actionable = suggestion is Suggestion.IncreaseFrequency
                    SuggestionCard(
                        suggestion = suggestion,
                        onClick = if (actionable) {
                            { viewModel.applySuggestion(suggestion) }
                        } else null,
                    )
                }
                if (suggestions.size > 3) {
                    item {
                        TextButton(onClick = onViewSuggestions) {
                            Text("View all suggestions")
                        }
                    }
                }
            }

            // Recent Workouts
            item {
                Text(
                    text = "Recent Workouts",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (recentWorkouts.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.FitnessCenter,
                        title = "No workouts yet",
                        subtitle = "Start your first workout to begin tracking progress!",
                        actionLabel = "Start Workout",
                        onAction = onStartWorkout
                    )
                }
            } else {
                items(recentWorkouts) { workoutWithExercises ->
                    val dateFormat = SimpleDateFormat("MMM d, yyyy 'at' HH:mm", Locale.getDefault())
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onViewWorkoutDetail(workoutWithExercises.workout.id) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = dateFormat.format(Date(workoutWithExercises.workout.startTime)),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val exerciseNames = workoutWithExercises.exercises.mapNotNull { we ->
                                exercises[we.exerciseId]?.name
                            }
                            Text(
                                text = if (exerciseNames.isEmpty()) "${workoutWithExercises.exercises.size} exercises"
                                else exerciseNames.joinToString(", "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                maxLines = 2
                            )
                            if (workoutWithExercises.workout.endTime != null) {
                                val duration = (workoutWithExercises.workout.endTime - workoutWithExercises.workout.startTime) / 60000
                                Text(
                                    text = "${duration}min",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Bottom spacing for FAB
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    pendingSwap?.let { swap ->
        if (swap.chosenRoutineId == null) {
            PickRoutineDialog(
                swap = swap,
                onPick = { viewModel.pickRoutine(it) },
                onDismiss = { viewModel.cancelPlanSwap() },
            )
        } else {
            PickDayDialog(
                swap = swap,
                onPick = { viewModel.confirmPlanSwap(it) },
                onDismiss = { viewModel.cancelPlanSwap() },
            )
        }
    }

    if (showHomeSheet) {
        HomeWorkoutSheet(
            defaultGroup = homeSheetDefaultGroup,
            defaultEquipment = remember(showHomeSheet) { EquipmentPrefs.load(context) },
            onGenerate = { group, equipment ->
                coroutineScope.launch {
                    EquipmentPrefs.save(context, equipment)
                    val ids = viewModel.generateHomeWorkout(group, equipment)
                    showHomeSheet = false
                    if (ids.isEmpty()) {
                        snackbarHostState.showSnackbar(
                            "No ${group.displayName.lowercase()} exercises for that equipment combo"
                        )
                    } else {
                        onStartHomeWorkout(ids)
                    }
                }
            },
            onDismiss = { showHomeSheet = false },
        )
    }
}

@Composable
private fun PickRoutineDialog(
    swap: HomeViewModel.PendingPlanSwap,
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val target = swap.suggestion.muscleGroup.displayName
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick a $target routine") },
        text = {
            Column {
                Text(
                    text = "Step 1 of 2 — choose which routine you want to train. " +
                        "Next you'll pick which existing day to convert.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.heightIn(max = 380.dp)
                ) {
                    items(swap.routineOptions) { opt ->
                        ListItem(
                            headlineContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(opt.name, modifier = Modifier.weight(1f))
                                    if (opt.alreadyInPlan) {
                                        AssistChip(
                                            onClick = {},
                                            label = { Text("In plan", style = MaterialTheme.typography.labelSmall) },
                                            enabled = false,
                                        )
                                    }
                                }
                            },
                            supportingContent = {
                                Column {
                                    if (opt.description.isNotBlank()) {
                                        Text(opt.description, style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (opt.exercisePreview.isNotBlank()) {
                                        Text(
                                            text = opt.exercisePreview,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.clickable { onPick(opt.id) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun PickDayDialog(
    swap: HomeViewModel.PendingPlanSwap,
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val routineName = swap.routineOptions.firstOrNull { it.id == swap.chosenRoutineId }?.name
        ?: "the new routine"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Replace which day?") },
        text = {
            Column {
                Text(
                    text = "Step 2 of 2 — pick the existing plan day to convert into " +
                        "'$routineName'. The recommended day is highlighted; it's the " +
                        "one most spaced out from your existing ${swap.suggestion.muscleGroup.displayName.lowercase()} days.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp)
                ) {
                    items(swap.dayOptions) { d ->
                        val isRecommended = d.planId == swap.recommendedDayPlanId
                        val isAlreadyTarget = d.isTargetGroup
                        ListItem(
                            headlineContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Day ${d.dayOrder}: ${d.routineName}", modifier = Modifier.weight(1f))
                                    if (isRecommended) {
                                        AssistChip(
                                            onClick = {},
                                            label = { Text("Recommended", style = MaterialTheme.typography.labelSmall) },
                                            enabled = false,
                                        )
                                    }
                                }
                            },
                            supportingContent = {
                                Text(
                                    text = when {
                                        isAlreadyTarget -> "Already ${swap.suggestion.muscleGroup.displayName} — picking this would erase, not add"
                                        else -> d.muscleGroupLabel
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isAlreadyTarget) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                )
                            },
                            colors = if (isRecommended) ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            ) else ListItemDefaults.colors(),
                            modifier = Modifier.clickable { onPick(d.planId) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun HomeStat(value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            maxLines = 1
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Compact volume formatting for the stats card. Numbers can run into the
 * millions of kg/lbs for power users, so anything ≥1000 collapses to "12.3k"
 * to avoid wrapping the row.
 */
private fun formatVolume(weightKg: Double, unit: String): String {
    val value = WeightUnits.fromKg(weightKg, unit)
    return when {
        value >= 1_000_000 -> "%.1fM".format(value / 1_000_000)
        value >= 10_000 -> "%.0fk".format(value / 1000)
        value >= 1_000 -> "%.1fk".format(value / 1000)
        else -> "%.0f".format(value)
    }
}
