package com.fittrack.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class SetData(
    val id: Long = 0,
    val reps: String = "",
    val weight: String = "",
    val isWarmup: Boolean = false
)

@Composable
fun ExerciseCard(
    exerciseName: String,
    muscleGroup: String,
    sets: List<SetData>,
    onAddSet: () -> Unit,
    onUpdateSet: (Int, SetData) -> Unit,
    onDeleteSet: (Int) -> Unit,
    onRemoveExercise: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exerciseName,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = muscleGroup,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Row {
                    IconButton(onClick = onRemoveExercise) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove exercise", tint = MaterialTheme.colorScheme.error)
                    }
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Set", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(24.dp))
                        Text("Weight", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                        Text("Reps", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(80.dp))
                    }

                    var workingSetNumber = 0
                    sets.forEachIndexed { index, set ->
                        if (!set.isWarmup) workingSetNumber++
                        SetInputRow(
                            setNumber = workingSetNumber,
                            reps = set.reps,
                            weight = set.weight,
                            isWarmup = set.isWarmup,
                            onRepsChange = { onUpdateSet(index, set.copy(reps = it)) },
                            onWeightChange = { onUpdateSet(index, set.copy(weight = it)) },
                            onWarmupToggle = { onUpdateSet(index, set.copy(isWarmup = it)) },
                            onDelete = { onDeleteSet(index) }
                        )
                    }

                    TextButton(
                        onClick = onAddSet,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Set")
                    }
                }
            }
        }
    }
}
