package com.fittrack.app.ui.workout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fittrack.app.di.AppModule
import com.fittrack.app.ui.components.EmptyState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutHistoryScreen(
    appModule: AppModule,
    onWorkoutClick: (Long) -> Unit,
    onBack: () -> Unit
) {
    val viewModel: WorkoutHistoryViewModel = viewModel(
        factory = WorkoutHistoryViewModel.Factory(
            appModule.workoutRepository,
            appModule.exerciseRepository
        )
    )

    val workouts by viewModel.workouts.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val dateFormat = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (workouts.isEmpty()) {
            EmptyState(
                icon = Icons.Default.FitnessCenter,
                title = "No workout history",
                subtitle = "Complete your first workout to see it here!",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(workouts.filter { it.workout.endTime != null }) { workoutWithExercises ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onWorkoutClick(workoutWithExercises.workout.id) }
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
                                text = exerciseNames.joinToString(", ").ifEmpty { "${workoutWithExercises.exercises.size} exercises" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                maxLines = 2
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text(
                                    text = timeFormat.format(Date(workoutWithExercises.workout.startTime)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                workoutWithExercises.workout.endTime?.let { endTime ->
                                    val duration = (endTime - workoutWithExercises.workout.startTime) / 60000
                                    Text(
                                        text = "${duration} min",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
