package com.fittrack.app.ui.progress

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fittrack.app.di.AppModule
import com.fittrack.app.ui.components.EmptyState
import com.fittrack.app.ui.components.MuscleGroupChipRow
import com.fittrack.app.ui.components.ProgressChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    appModule: AppModule,
    onBack: () -> Unit
) {
    val viewModel: ProgressViewModel = viewModel(
        factory = ProgressViewModel.Factory(
            appModule.exerciseRepository,
            appModule.progressAnalyzer
        )
    )

    val selectedMuscleGroup by viewModel.selectedMuscleGroup.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val selectedExercise by viewModel.selectedExercise.collectAsState()
    val progressData by viewModel.progressData.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Progress") },
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
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                MuscleGroupChipRow(
                    selectedGroup = selectedMuscleGroup,
                    onGroupSelected = { viewModel.selectMuscleGroup(it) }
                )
            }

            // Chart area
            if (selectedExercise != null && progressData.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column {
                            ProgressChart(
                                dataPoints = progressData,
                                label = "Estimated 1RM (kg) - ${selectedExercise!!.name}",
                                valueSelector = { it.estimated1RM }
                            )
                            HorizontalDivider()
                            ProgressChart(
                                dataPoints = progressData,
                                label = "Total Volume (kg)",
                                valueSelector = { it.totalVolume }
                            )
                        }
                    }
                }
            } else if (selectedExercise != null) {
                item {
                    EmptyState(
                        icon = Icons.Default.ShowChart,
                        title = "No data yet",
                        subtitle = "Complete some workouts with ${selectedExercise!!.name} to see your progress",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Exercise list
            item {
                Text(
                    text = "Select an exercise to view progress",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            items(exercises) { exercise ->
                ListItem(
                    headlineContent = { Text(exercise.name) },
                    supportingContent = {
                        Text(
                            buildString {
                                append(exercise.muscleGroup.displayName)
                                exercise.secondaryMuscleGroup?.let { append(" / ${it.displayName}") }
                            }
                        )
                    },
                    modifier = Modifier.clickable { viewModel.selectExercise(exercise) },
                    colors = if (selectedExercise?.id == exercise.id) {
                        ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    } else {
                        ListItemDefaults.colors()
                    }
                )
            }
        }
    }
}
