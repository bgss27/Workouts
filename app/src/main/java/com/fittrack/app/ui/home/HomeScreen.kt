package com.fittrack.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.activity.compose.rememberLauncherForActivityResult
import com.fittrack.app.data.EquipmentPrefs
import com.fittrack.app.data.health.HealthConnectAvailability
import com.fittrack.app.data.health.StepsSummary
import com.fittrack.app.ui.components.PillChip
import com.fittrack.app.ui.components.StatValue
import androidx.compose.foundation.layout.fillMaxHeight
import com.fittrack.app.data.WeightUnits
import com.fittrack.app.data.entity.MuscleGroup
import com.fittrack.app.di.AppModule
import com.fittrack.app.domain.model.Suggestion
import com.fittrack.app.ui.components.EmptyState
import com.fittrack.app.ui.components.HeroCard
import com.fittrack.app.ui.components.HeroStat
import com.fittrack.app.ui.components.IconBadge
import com.fittrack.app.ui.components.ScreenGutter
import com.fittrack.app.ui.components.ScreenHeader
import com.fittrack.app.ui.components.SectionHeader
import com.fittrack.app.ui.components.SuggestionCard
import com.fittrack.app.ui.components.Tile
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
    onViewBodyMetrics: () -> Unit,
    onUpgrade: () -> Unit,
    onStartHomeWorkout: (List<Long>) -> Unit,
) {
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(
            appModule.workoutRepository,
            appModule.exerciseRepository,
            appModule.progressAnalyzer,
            appModule.userPlanRepository,
            appModule.routineRepository,
            appModule.healthConnectService,
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

    // Step tracker (Health Connect). Refreshed each time Home is shown.
    val healthConnect = appModule.healthConnectService
    val steps by viewModel.steps.collectAsState()
    val stepsGranted by viewModel.stepsGranted.collectAsState()
    LaunchedEffect(Unit) { viewModel.refreshSteps() }
    val stepsLauncher = rememberLauncherForActivityResult(
        contract = healthConnect.permissionRequestContract()
    ) { viewModel.refreshSteps() }

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

    // Eyebrow date, e.g. "Sunday, Oct 25".
    val todayLabel = remember {
        SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date())
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ScreenHeader(
                title = "FitTrack",
                eyebrow = todayLabel,
                actions = {
                    IconButton(onClick = onViewProgress) {
                        Icon(
                            Icons.Default.ShowChart,
                            contentDescription = "Progress",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onViewMlInsights) {
                        Icon(
                            Icons.Default.Psychology,
                            contentDescription = "Insights",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onViewBodyMetrics) {
                        Icon(
                            Icons.Default.MonitorWeight,
                            contentDescription = "Body Metrics",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onViewHistory) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = "History",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = ScreenGutter, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Quick actions: the two primary entry points, side by side.
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    QuickActionTile(
                        icon = Icons.Default.FitnessCenter,
                        title = "Quick Workout",
                        subtitle = "Log sets as you go",
                        tint = MaterialTheme.colorScheme.primary,
                        onClick = onStartWorkout,
                        modifier = Modifier.weight(1f),
                    )
                    // Labeled "Home Workout" (not just an icon) so it can't be
                    // confused with the bottom-nav Home destination.
                    QuickActionTile(
                        icon = Icons.Default.SelfImprovement,
                        title = "Home Workout",
                        subtitle = "Built from your gear",
                        tint = MaterialTheme.colorScheme.secondary,
                        onClick = { showHomeSheet = true },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // My Plan Section
            if (hasPlan && planDays.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "My Plan",
                        subtitle = planProgramName,
                        actionLabel = "View All",
                        onAction = onViewMyPlan,
                    )
                }

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(planDays) { day ->
                            Tile(modifier = Modifier.width(180.dp)) {
                                Text(
                                    text = day.dayLabel.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = day.routine?.routine?.name ?: "Workout",
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${day.routine?.exercises?.size ?: 0} exercises",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        day.routine?.routine?.id?.let { onStartRoutine(it) }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp),
                                    shape = MaterialTheme.shapes.small,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Start",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // No plan - prompt to set one up
                item {
                    PromptTile(
                        icon = Icons.Default.EventNote,
                        tint = MaterialTheme.colorScheme.primary,
                        title = "Set up your workout plan",
                        subtitle = "Choose a 3-day or 5-day program to follow",
                        onClick = onViewMyPlan,
                    )
                }
            }

            // Stats hero
            item {
                HeroCard {
                    Text(
                        text = "Your Progress",
                        style = MaterialTheme.typography.labelLarge,
                        color = com.fittrack.app.ui.theme.VoltInk.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        HeroStat(value = "$workoutCount", label = "Total", modifier = Modifier.weight(1f))
                        HeroStat(value = "$workoutsThisWeek", label = "This Week", modifier = Modifier.weight(1f))
                        HeroStat(
                            value = formatVolume(totalVolumeKg, unit),
                            label = "Volume ($unit)",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Step tracker
            if (healthConnect.availability == HealthConnectAvailability.AVAILABLE) {
                item {
                    StepsTile(
                        summary = steps,
                        granted = stepsGranted,
                        onConnect = { stepsLauncher.launch(healthConnect.stepsPermissions) },
                    )
                }
            }

            // Pro upgrade banner (only shown to free users)
            if (!isPro) {
                item {
                    PromptTile(
                        icon = Icons.Default.Star,
                        tint = MaterialTheme.colorScheme.tertiary,
                        title = "Upgrade to Pro",
                        subtitle = "Unlock ML insights, all programs, and more",
                        onClick = onUpgrade,
                    )
                }
            }

            // Suggestions
            if (suggestions.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Improvement Tips",
                        actionLabel = if (suggestions.size > 3) "View all" else null,
                        onAction = if (suggestions.size > 3) onViewSuggestions else null,
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
            }

            // Recent Workouts
            item {
                SectionHeader(
                    title = "Recent Workouts",
                    actionLabel = if (recentWorkouts.isNotEmpty()) "History" else null,
                    onAction = if (recentWorkouts.isNotEmpty()) onViewHistory else null,
                )
            }

            if (recentWorkouts.isEmpty()) {
                item {
                    // No action button here: the Quick Workout tile above
                    // already covers it.
                    EmptyState(
                        icon = Icons.Default.FitnessCenter,
                        title = "No workouts yet",
                        subtitle = "Tap Quick Workout to begin tracking progress!",
                    )
                }
            } else {
                items(recentWorkouts) { workoutWithExercises ->
                    val start = Date(workoutWithExercises.workout.startTime)
                    val dayFormat = remember { SimpleDateFormat("d", Locale.getDefault()) }
                    val monthFormat = remember { SimpleDateFormat("MMM", Locale.getDefault()) }
                    val timeFormat = remember { SimpleDateFormat("EEE, HH:mm", Locale.getDefault()) }
                    Tile(
                        onClick = { onViewWorkoutDetail(workoutWithExercises.workout.id) },
                        contentPadding = PaddingValues(14.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Calendar-style date badge
                            Column(
                                modifier = Modifier
                                    .size(width = 52.dp, height = 56.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = dayFormat.format(start),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = monthFormat.format(start).uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                val exerciseNames = workoutWithExercises.exercises.mapNotNull { we ->
                                    exercises[we.exerciseId]?.name
                                }
                                Text(
                                    text = if (exerciseNames.isEmpty()) "${workoutWithExercises.exercises.size} exercises"
                                    else exerciseNames.joinToString(", "),
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = timeFormat.format(start),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (workoutWithExercises.workout.endTime != null) {
                                        val duration = (workoutWithExercises.workout.endTime - workoutWithExercises.workout.startTime) / 60000
                                        Text(
                                            text = "·",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${duration} min",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
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

/**
 * Equal-width quick action tile: icon badge, bold title, one-line muted
 * subtitle. Two of these sit side by side at the top of the Home list.
 */
@Composable
private fun QuickActionTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Tile(modifier = modifier, onClick = onClick) {
        IconBadge(icon = icon, tint = tint)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Tappable prompt row: icon badge, title, muted subtitle, trailing chevron.
 * Used for the "set up a plan" nudge and the Pro upgrade banner.
 */
@Composable
private fun PromptTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Tile(modifier = modifier, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            IconBadge(icon = icon, tint = tint)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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

/**
 * Daily step count from Health Connect. Before permission is granted it
 * shows a one-tap connect prompt; afterwards today's total, the 7-day
 * average, and a small bar strip of the last week.
 */
@Composable
private fun StepsTile(
    summary: StepsSummary?,
    granted: Boolean,
    onConnect: () -> Unit,
) {
    Tile {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon = Icons.Default.DirectionsWalk, tint = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Steps", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = when {
                        !granted -> "Connect Health Connect to see your daily steps"
                        summary == null -> "Reading from Health Connect…"
                        else -> "7-day average ${"%,d".format(summary.weeklyAverage)}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!granted) {
                PillChip(label = "Connect", selected = true, onClick = onConnect)
            }
        }
        if (granted && summary != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                StatValue(
                    value = "%,d".format(summary.today),
                    label = "Today",
                    modifier = Modifier.weight(1f),
                )
                // Last-7-days bar strip; today is the rightmost, drawn in primary.
                val max = (summary.daily.maxOrNull() ?: 0L).coerceAtLeast(1L)
                Row(
                    modifier = Modifier.height(40.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    summary.daily.forEachIndexed { i, v ->
                        val isToday = i == summary.daily.lastIndex
                        val fraction = (v.toFloat() / max).coerceIn(0.08f, 1f)
                        Box(
                            modifier = Modifier
                                .width(10.dp)
                                .fillMaxHeight(fraction)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (isToday) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                        )
                    }
                }
            }
        }
    }
}
