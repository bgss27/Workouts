package com.fittrack.app.ui.workout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fittrack.app.di.AppModule
import com.fittrack.app.ui.components.ExerciseCard
import com.fittrack.app.ui.components.MuscleGroupChipRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    appModule: AppModule,
    routineId: Long?,
    onWorkoutComplete: () -> Unit,
    onBack: () -> Unit
) {
    val viewModel: ActiveWorkoutViewModel = viewModel(
        factory = ActiveWorkoutViewModel.Factory(
            appModule.workoutRepository,
            appModule.exerciseRepository,
            appModule.routineSuggestionEngine,
            routineId
        )
    )

    val uiState by viewModel.uiState.collectAsState()
    val availableExercises by viewModel.availableExercises.collectAsState()
    val selectedMuscleGroup by viewModel.selectedMuscleGroup.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showExercisePicker by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showFinishDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isActive) {
        if (!uiState.isActive && uiState.workoutId > 0) {
            onWorkoutComplete()
        }
    }

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
                itemsIndexed(uiState.exercises) { index, exerciseState ->
                    ExerciseCard(
                        exerciseName = exerciseState.exercise.name,
                        muscleGroup = exerciseState.exercise.muscleGroup.displayName,
                        sets = exerciseState.sets,
                        onAddSet = { viewModel.addSet(index) },
                        onUpdateSet = { setIndex, setData -> viewModel.updateSet(index, setIndex, setData) },
                        onDeleteSet = { setIndex -> viewModel.deleteSet(index, setIndex) },
                        onRemoveExercise = { viewModel.removeExercise(index) }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
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
                                modifier = Modifier.fillMaxWidth(),
                                leadingContent = {
                                    IconButton(onClick = {
                                        viewModel.addExercise(exercise)
                                        showExercisePicker = false
                                    }) {
                                        Icon(Icons.Default.Add, contentDescription = "Add")
                                    }
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
