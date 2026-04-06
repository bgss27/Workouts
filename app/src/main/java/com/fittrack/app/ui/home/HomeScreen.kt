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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fittrack.app.di.AppModule
import com.fittrack.app.ui.components.EmptyState
import com.fittrack.app.ui.components.SuggestionCard
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    appModule: AppModule,
    onStartWorkout: () -> Unit,
    onViewHistory: () -> Unit,
    onViewProgress: () -> Unit,
    onViewSuggestions: () -> Unit,
    onViewWorkoutDetail: (Long) -> Unit,
    onViewMyPlan: () -> Unit,
    onStartRoutine: (Long) -> Unit,
    onViewMlInsights: () -> Unit
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
    val suggestions by viewModel.suggestions.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val hasPlan by viewModel.hasPlan.collectAsState()
    val planDays by viewModel.planDays.collectAsState()
    val planProgramName by viewModel.planProgramName.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FitTrack") },
                actions = {
                    IconButton(onClick = onViewHistory) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onStartWorkout,
                icon = { Icon(Icons.Default.FitnessCenter, contentDescription = null) },
                text = { Text("Quick Workout") }
            )
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
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$workoutCount",
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                Text(
                                    text = "Workouts",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            // Quick Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onViewProgress() }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.ShowChart, contentDescription = null)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Progress", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    OutlinedCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onViewMlInsights() }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Psychology, contentDescription = null)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Insights", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    OutlinedCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onViewHistory() }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.History, contentDescription = null)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("History", style = MaterialTheme.typography.labelMedium)
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
                    SuggestionCard(suggestion = suggestion)
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
}
