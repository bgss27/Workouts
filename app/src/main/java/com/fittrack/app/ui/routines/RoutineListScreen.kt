package com.fittrack.app.ui.routines

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fittrack.app.di.AppModule
import com.fittrack.app.ui.components.EmptyState
import com.fittrack.app.ui.components.MuscleGroupChipRow

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
            appModule.exerciseRepository
        )
    )

    val selectedMuscleGroup by viewModel.selectedMuscleGroup.collectAsState()
    val routines by viewModel.filteredRoutines.collectAsState()

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

            if (routines.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.List,
                        title = "No routines found",
                        subtitle = "Try selecting a different muscle group"
                    )
                }
            } else {
                items(routines) { routineWithExercises ->
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
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = routine.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
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
