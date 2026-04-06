package com.fittrack.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    onViewWorkoutDetail: (Long) -> Unit
) {
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(
            appModule.workoutRepository,
            appModule.exerciseRepository,
            appModule.progressAnalyzer
        )
    )

    val recentWorkouts by viewModel.recentWorkouts.collectAsState()
    val workoutCount by viewModel.completedWorkoutCount.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val exercises by viewModel.exercises.collectAsState()

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
                text = { Text("Start Workout") }
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
                            .clickable { onViewSuggestions() }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Tips", style = MaterialTheme.typography.labelMedium)
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
