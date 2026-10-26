package com.fittrack.app.ui.workout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fittrack.app.data.WeightUnits
import com.fittrack.app.ui.components.DetailHeader
import androidx.compose.material.icons.filled.Share
import com.fittrack.app.data.relation.WorkoutWithExercises
import com.fittrack.app.share.ShareSummary
import com.fittrack.app.share.WorkoutShareCard
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
    val context = LocalContext.current
    val unit = remember { WeightUnits.preferred(context) }

    Scaffold(
        topBar = {
            DetailHeader(
                title = "Workout Detail",
                onBack = onBack,
                actions = {
                    val w = workout
                    if (w?.workout?.endTime != null) {
                        IconButton(onClick = {
                            WorkoutShareCard.share(context, buildShareSummary(w, exerciseDetails, unit))
                        }) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Share workout",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
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
                            Text("Weight ($unit)", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
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
                                    text = WeightUnits.format(set.weightKg, unit),
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

                        // Volume summary — convert the kg-based total once.
                        val workingSets = detail.sets.filter { !it.isWarmup }
                        if (workingSets.isNotEmpty()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            val totalVolumeKg = workingSets.sumOf { it.weightKg * it.reps }
                            Text(
                                text = "Total volume: ${WeightUnits.format(totalVolumeKg, unit)}",
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

/** Share-card data for a past workout (no PR or streak context here). */
private fun buildShareSummary(
    w: WorkoutWithExercises,
    details: List<ExerciseDetail>,
    unit: String,
): ShareSummary {
    val end = w.workout.endTime ?: w.workout.startTime
    val working = details.flatMap { d -> d.sets.filter { !it.isWarmup } }
    val volumeKg = working.sumOf { it.weightKg * it.reps }
    val volume = WeightUnits.fromKg(volumeKg, unit)
    val volumeText = when {
        volume >= 10_000 -> "%.0fk".format(volume / 1000)
        volume >= 1_000 -> "%.1fk".format(volume / 1000)
        else -> "%.0f".format(volume)
    } + " " + unit
    return ShareSummary(
        headline = "Workout complete",
        dateText = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(w.workout.startTime)),
        durationMinutes = ((end - w.workout.startTime) / 60_000L).coerceAtLeast(1),
        exerciseCount = details.size,
        setCount = working.size,
        volumeText = volumeText,
        exerciseLines = details.map { d ->
            val n = d.sets.count { !it.isWarmup }
            "${d.exerciseName} · $n ${if (n == 1) "set" else "sets"}"
        },
        prLines = emptyList(),
        streakWeeks = 0,
    )
}
