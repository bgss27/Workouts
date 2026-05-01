package com.fittrack.app.ui.exercises

import androidx.compose.animation.AnimatedVisibility
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
import com.fittrack.app.data.entity.Exercise
import com.fittrack.app.data.entity.MuscleGroup
import com.fittrack.app.di.AppModule
import com.fittrack.app.domain.model.ExerciseGuideData
import com.fittrack.app.ui.components.MuscleGroupChipRow
import com.fittrack.app.ui.components.MovementAnimation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisesScreen(
    appModule: AppModule
) {
    val viewModel: ExercisesViewModel = viewModel(
        factory = ExercisesViewModel.Factory(appModule.exerciseRepository)
    )

    val selectedMuscleGroup by viewModel.selectedMuscleGroup.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val groupedExercises by viewModel.groupedExercises.collectAsState()

    val showGrouped = selectedMuscleGroup == null && searchQuery.isBlank()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Exercises") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Search bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.search(it) },
                    label = { Text("Search exercises") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.search("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    }
                )
            }

            // Muscle group filter
            item {
                MuscleGroupChipRow(
                    selectedGroup = selectedMuscleGroup,
                    onGroupSelected = { viewModel.selectMuscleGroup(it) }
                )
            }

            // Exercise count
            item {
                Text(
                    text = "${exercises.size} exercises",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            if (showGrouped) {
                // Grouped by muscle
                val sortedGroups = groupedExercises.entries.sortedBy { it.key.displayName }
                sortedGroups.forEach { (group, groupExercises) ->
                    item {
                        MuscleGroupHeader(group, groupExercises.size)
                    }
                    items(groupExercises.sortedBy { it.name }) { exercise ->
                        ExerciseDetailCard(exercise)
                    }
                }
            } else {
                // Flat filtered list
                items(exercises.sortedBy { it.name }) { exercise ->
                    ExerciseDetailCard(exercise)
                }
            }
        }
    }
}

@Composable
private fun MuscleGroupHeader(group: MuscleGroup, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = group.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        AssistChip(
            onClick = {},
            label = { Text("$count") }
        )
    }
}

@Composable
private fun ExerciseDetailCard(exercise: Exercise) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = exercise.muscleGroup.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            AnimatedVisibility(visible = expanded) {
                val guide = ExerciseGuideData.getGuide(exercise.name)

                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))

                    // Movement animation
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        MovementAnimation(
                            pattern = guide.movementPattern,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Muscles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Circle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                        Text("Primary:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text(exercise.muscleGroup.displayName, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Circle, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(12.dp))
                        Text("Secondary:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text(
                            exercise.secondaryMuscleGroup?.displayName ?: "None",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (exercise.secondaryMuscleGroup != null) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.size(12.dp))
                        Text("Type:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text(if (exercise.secondaryMuscleGroup != null) "Compound" else "Isolation", style = MaterialTheme.typography.bodyMedium)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // How to perform
                    Text("How to Perform", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    guide.steps.forEachIndexed { i, step ->
                        Row(modifier = Modifier.padding(vertical = 2.dp)) {
                            Text("${i + 1}. ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(step, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Breathing
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Air, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(14.dp))
                        Text("Breathing:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(guide.breathingCue, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 20.dp))

                    Spacer(modifier = Modifier.height(8.dp))

                    // Tempo
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                        Text("Tempo:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text(guide.tempo, style = MaterialTheme.typography.bodySmall)
                        if (guide.tempo.contains("-")) {
                            Text("(eccentric-pause-concentric)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Common mistakes
                    Text("Common Mistakes", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(4.dp))
                    guide.commonMistakes.forEach { mistake ->
                        Row(modifier = Modifier.padding(vertical = 1.dp)) {
                            Text("  ✗  ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            Text(mistake, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
