package com.fittrack.app.ui.myplan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fittrack.app.di.AppModule
import com.fittrack.app.ui.components.EmptyState
import com.fittrack.app.ui.components.IconBadge
import com.fittrack.app.ui.components.ScreenGutter
import com.fittrack.app.ui.components.ScreenHeader
import com.fittrack.app.ui.components.Tile
import com.fittrack.app.ui.components.TonalButton

@Composable
fun MyPlanScreen(
    appModule: AppModule,
    onStartRoutine: (Long) -> Unit,
    onBrowseRoutines: () -> Unit,
    onBack: () -> Unit
) {
    val viewModel: MyPlanViewModel = viewModel(
        factory = MyPlanViewModel.Factory(
            appModule.userPlanRepository,
            appModule.routineRepository,
            appModule.exerciseRepository
        )
    )

    val uiState by viewModel.uiState.collectAsState()
    val exerciseMap by viewModel.exerciseMap.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ScreenHeader(
                title = "My Plan",
                eyebrow = uiState.programName,
                actions = {
                    if (uiState.hasPlan) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = "Clear plan",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    // My Plan is a bottom tab; onBack is kept for the
                    // signature but a back arrow here would be confusing.
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (!uiState.hasPlan) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                EmptyState(
                    icon = Icons.Default.EventNote,
                    title = "No plan selected",
                    subtitle = "Browse routines and add a program to your plan to get started",
                    actionLabel = "Browse Routines",
                    onAction = onBrowseRoutines
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = ScreenGutter, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Program summary
                item {
                    Text(
                        text = "${uiState.days.size} workout${if (uiState.days.size != 1) "s" else ""} in your plan".uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // Day tiles
                items(uiState.days) { planDay ->
                    val routine = planDay.routine
                    val sortedExercises = routine?.exercises?.sortedBy { it.orderIndex } ?: emptyList()
                    val exerciseCount = sortedExercises.size

                    Tile {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            IconBadge(
                                icon = Icons.Default.FitnessCenter,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = planDay.dayLabel.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = routine?.routine?.name ?: "Unknown routine",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "$exerciseCount exercise${if (exerciseCount != 1) "s" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = {
                                    routine?.routine?.id?.let { onStartRoutine(it) }
                                },
                                modifier = Modifier.height(40.dp),
                                shape = MaterialTheme.shapes.small,
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Start", fontWeight = FontWeight.Bold)
                            }
                        }

                        routine?.let { r ->
                            if (r.routine.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = r.routine.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (sortedExercises.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                sortedExercises.forEachIndexed { i, re ->
                                    val name = exerciseMap[re.exerciseId]?.name ?: return@forEachIndexed
                                    Row(
                                        modifier = Modifier.padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "${i + 1}.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = name,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = "${re.suggestedSets}x${re.suggestedReps}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Change plan button
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    TonalButton(
                        text = "Change Plan",
                        onClick = onBrowseRoutines,
                        icon = Icons.Default.SwapHoriz
                    )
                }
            }
        }
    }

    // Clear plan dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Plan?") },
            text = { Text("This will remove all routines from your plan.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    viewModel.clearPlan()
                }) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
