package com.fittrack.app.ui.routines

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fittrack.app.di.AppModule
import com.fittrack.app.ui.components.EmptyState
import com.fittrack.app.ui.components.MuscleGroupChipRow
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineListScreen(
    appModule: AppModule,
    onRoutineClick: (Long) -> Unit,
    onBack: () -> Unit
) {
    val viewModel: RoutinesViewModel = viewModel(
        factory = RoutinesViewModel.Factory(
            appModule.routineSuggestionEngine,
            appModule.exerciseRepository,
            appModule.userPlanRepository
        )
    )

    val selectedMuscleGroup by viewModel.selectedMuscleGroup.collectAsState()
    val selectedDays by viewModel.selectedDaysPerWeek.collectAsState()
    val routines by viewModel.filteredRoutines.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.addedToast.collectLatest { message ->
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout Routines") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Days per week filter
            item {
                Column {
                    Text(
                        text = "Days per Week",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedDays == null,
                            onClick = { viewModel.selectDaysPerWeek(null) },
                            label = { Text("All") }
                        )
                        FilterChip(
                            selected = selectedDays == 3,
                            onClick = { viewModel.selectDaysPerWeek(if (selectedDays == 3) null else 3) },
                            label = { Text("3-Day") },
                            leadingIcon = if (selectedDays == 3) {
                                { Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else null
                        )
                        FilterChip(
                            selected = selectedDays == 5,
                            onClick = { viewModel.selectDaysPerWeek(if (selectedDays == 5) null else 5) },
                            label = { Text("5-Day") },
                            leadingIcon = if (selectedDays == 5) {
                                { Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else null
                        )
                        FilterChip(
                            selected = selectedDays == 0,
                            onClick = { viewModel.selectDaysPerWeek(if (selectedDays == 0) null else 0) },
                            label = { Text("Single Day") }
                        )
                    }
                }
            }

            // Muscle group filter
            item {
                MuscleGroupChipRow(
                    selectedGroup = selectedMuscleGroup,
                    onGroupSelected = { viewModel.selectMuscleGroup(it) }
                )
            }

            if (routines.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.List,
                        title = "No routines found",
                        subtitle = "Try selecting a different filter"
                    )
                }
            } else {
                // Group routines by program
                val programRoutines = routines.filter { it.routine.programName != null }
                    .groupBy { it.routine.programName!! }
                val standaloneRoutines = routines.filter { it.routine.programName == null }

                // Show program groups
                programRoutines.forEach { (programName, programDays) ->
                    val sortedDays = programDays.sortedBy { it.routine.dayOrder }
                    val daysCount = sortedDays.firstOrNull()?.routine?.daysPerWeek ?: 0
                    val difficulty = sortedDays.firstOrNull()?.routine?.difficulty ?: "intermediate"

                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = programName,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            AssistChip(
                                                onClick = {},
                                                label = { Text("${daysCount}x/week") }
                                            )
                                            AssistChip(
                                                onClick = {},
                                                label = { Text(difficulty.replaceFirstChar { it.uppercase() }) }
                                            )
                                        }
                                    }
                                    FilledTonalButton(
                                        onClick = { viewModel.addProgramToPlan(programName) }
                                    ) {
                                        Icon(
                                            Icons.Default.EventNote,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Add to Plan")
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${sortedDays.size} workouts in this program",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    items(sortedDays) { routineWithExercises ->
                        val routine = routineWithExercises.routine
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 32.dp, end = 16.dp, top = 2.dp, bottom = 2.dp)
                                .clickable { onRoutineClick(routine.id) }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = routine.name,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = routine.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${routineWithExercises.exercises.size} exercises",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Standalone routines section
                if (standaloneRoutines.isNotEmpty()) {
                    item {
                        Text(
                            text = "Single Day Routines",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    items(standaloneRoutines) { routineWithExercises ->
                        val routine = routineWithExercises.routine
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clickable { onRoutineClick(routine.id) }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = routine.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        IconButton(
                                            onClick = { viewModel.addStandaloneRoutineToPlan(routine.id, routine.name) }
                                        ) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = "Add to My Plan",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        AssistChip(
                                            onClick = {},
                                            label = {
                                                Text(
                                                    routine.difficulty.replaceFirstChar { it.uppercase() },
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = routine.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${routineWithExercises.exercises.size} exercises",
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
