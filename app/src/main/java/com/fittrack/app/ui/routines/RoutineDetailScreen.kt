package com.fittrack.app.ui.routines

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fittrack.app.di.AppModule
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineDetailScreen(
    appModule: AppModule,
    routineId: Long,
    onStartRoutine: () -> Unit,
    onBack: () -> Unit
) {
    val viewModel: RoutinesViewModel = viewModel(
        factory = RoutinesViewModel.Factory(
            appModule.routineSuggestionEngine,
            appModule.exerciseRepository,
            appModule.userPlanRepository
        )
    )

    LaunchedEffect(routineId) {
        viewModel.loadRoutineDetail(routineId)
    }

    val routineDetail by viewModel.routineDetail.collectAsState()
    val exerciseMap by viewModel.exerciseMap.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.addedToast.collectLatest { message ->
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(routineDetail?.routine?.name ?: "Routine") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onStartRoutine,
                icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                text = { Text("Start Routine") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        routineDetail?.let { detail ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Info card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = detail.routine.name,
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = detail.routine.description,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text(detail.routine.difficulty.replaceFirstChar { it.uppercase() }) }
                                )
                                AssistChip(
                                    onClick = {},
                                    label = { Text("${detail.exercises.size} exercises") }
                                )
                                if (detail.routine.daysPerWeek > 0) {
                                    AssistChip(
                                        onClick = {},
                                        label = { Text("${detail.routine.daysPerWeek}x/week") }
                                    )
                                }
                            }
                            detail.routine.programName?.let { program ->
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Part of: $program",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                // Add to My Plan button
                item {
                    val programName = detail.routine.programName
                    OutlinedButton(
                        onClick = {
                            if (programName != null) {
                                viewModel.addProgramToPlan(programName)
                            } else {
                                viewModel.addStandaloneRoutineToPlan(detail.routine.id, detail.routine.name)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.EventNote,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (programName != null) "Add $programName to My Plan"
                            else "Add to My Plan"
                        )
                    }
                }

                // Exercise list
                item {
                    Text(
                        text = "Exercises",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                items(detail.exercises.sortedBy { it.orderIndex }) { routineExercise ->
                    val exercise = exerciseMap[routineExercise.exerciseId]
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = exercise?.name ?: "Exercise #${routineExercise.exerciseId}",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = exercise?.muscleGroup?.displayName ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                                Text(
                                    text = "${routineExercise.suggestedSets} sets",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${routineExercise.suggestedReps} reps",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                // Bottom spacing for FAB
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}
