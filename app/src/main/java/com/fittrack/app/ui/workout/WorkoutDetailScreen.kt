package com.fittrack.app.ui.workout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fittrack.app.data.entity.WorkoutExercise
import com.fittrack.app.data.entity.WorkoutSet
import com.fittrack.app.di.AppModule
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    appModule: AppModule,
    workoutId: Long,
    onBack: () -> Unit
) {
    val viewModel: WorkoutDetailViewModel = viewModel(
        factory = WorkoutDetailViewModel.Factory(appModule.workoutRepository, appModule.exerciseRepository, workoutId)
    )

    val workout by viewModel.workout.collectAsState()
    val exerciseDetails by viewModel.exerciseDetails.collectAsState()
    val dateFormat = SimpleDateFormat("EEEE, MMM d, yyyy 'at' HH:mm", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            workout?.let { w ->
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = dateFormat.format(Date(w.workout.startTime)),
                                style = MaterialTheme.typography.titleMedium
                            )
                            w.workout.endTime?.let { endTime ->
                                val duration = (endTime - w.workout.startTime) / 60000
                                Text(
                                    text = "Duration: ${duration} minutes",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            w.workout.notes?.let { notes ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = notes,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            items(exerciseDetails) { detail ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = detail.exerciseName,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = detail.muscleGroup,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Set", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                            Text("Weight", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                            Text("Reps", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        detail.sets.forEachIndexed { index, set ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (set.isWarmup) "W" else "${set.setNumber}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                    color = if (set.isWarmup) MaterialTheme.colorScheme.tertiary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${set.weightKg} kg",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${set.reps}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Volume summary
                        val workingSets = detail.sets.filter { !it.isWarmup }
                        if (workingSets.isNotEmpty()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            val totalVolume = workingSets.sumOf { it.weightKg * it.reps }
                            Text(
                                text = "Total volume: ${"%.1f".format(totalVolume)} kg",
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
