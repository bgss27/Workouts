package com.fittrack.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext
import com.fittrack.app.data.repository.ExerciseImageRepository
import com.fittrack.app.data.repository.ExerciseInstructions
import com.fittrack.app.data.repository.OpenExerciseDb
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class SetData(
    val id: Long = 0,
    val reps: String = "",
    val weight: String = "",
    val isWarmup: Boolean = false,
    /** RPE 6-10. Null = unrated. Warmup sets are conventionally not rated. */
    val rpe: Int? = null,
    /**
     * Whether the user has marked this set as done. Auto-toggles on when
     * weight + reps are both valid for a non-warmup set, but the user can
     * tap the leading button to manually toggle it back off and edit.
     */
    val isCompleted: Boolean = false,
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
    modifier: Modifier = Modifier,
    weightUnit: String = "kg",
    inSuperset: Boolean = false,
    canToggleSuperset: Boolean = false,
    onSupersetToggle: () -> Unit = {},
    onSwap: (() -> Unit)? = null,
    onSetCompleted: () -> Unit = {},
    // Expanded state is hoisted so the parent can implement an accordion —
    // only the "working" exercise stays open, the rest collapse. When the
    // parent passes null, the card falls back to its own self-managed
    // expanded state (default open).
    expanded: Boolean? = null,
    onExpandToggle: (() -> Unit)? = null,
) {
    var localExpanded by remember { mutableStateOf(true) }
    val isExpanded = expanded ?: localExpanded
    val toggle: () -> Unit = onExpandToggle ?: { localExpanded = !localExpanded }
    var showForm by remember(exerciseName) { mutableStateOf(false) }
    val hasForm = remember(exerciseName) {
        ExerciseImageRepository.getExerciseImages(exerciseName).isNotEmpty()
    }
    // Prefer instructions from the bundled open-source dataset
    // (free-exercise-db, public domain — see OpenExerciseDb). Fall back to our
    // hand-written entries for exercises the dataset doesn't cover (the
    // four no-equipment biceps moves, novel bodyweight variants, etc.).
    val instructionsContext = LocalContext.current
    val instructions = remember(exerciseName) {
        OpenExerciseDb.getInstructions(instructionsContext, exerciseName)
            ?: ExerciseInstructions.get(exerciseName)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (inSuperset) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (inSuperset) {
                SupersetBadge(modifier = Modifier.padding(bottom = 6.dp))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { toggle() },
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
                    if (onSwap != null) {
                        IconButton(onClick = onSwap) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Swap exercise",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    if (canToggleSuperset || inSuperset) {
                        IconButton(onClick = onSupersetToggle) {
                            Icon(
                                imageVector = if (inSuperset) Icons.Default.LinkOff else Icons.Default.Link,
                                contentDescription = if (inSuperset) "Remove from superset" else "Group with previous as superset",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    IconButton(onClick = onRemoveExercise) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove exercise", tint = MaterialTheme.colorScheme.error)
                    }
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand"
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    // How-to text — shown for any exercise we have written
                    // instructions for. Especially useful for bodyweight
                    // exercises pulled in by the "home workout" generator,
                    // since many of them have no image and may be unfamiliar.
                    if (instructions != null) {
                        Text(
                            text = instructions,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                            modifier = Modifier.padding(vertical = 6.dp),
                        )
                    }
                    if (hasForm) {
                        TextButton(
                            onClick = { showForm = !showForm },
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (showForm) "Hide form" else "View form",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        AnimatedVisibility(visible = showForm) {
                            ExerciseImageGallery(
                                exerciseName = exerciseName,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }

                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Set", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(24.dp))
                        Text("Weight ($weightUnit)", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
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
                            rpe = set.rpe,
                            isCompleted = set.isCompleted,
                            weightUnit = weightUnit,
                            onRepsChange = { onUpdateSet(index, set.copy(reps = it)) },
                            onWeightChange = { onUpdateSet(index, set.copy(weight = it)) },
                            onWarmupToggle = {
                                // Switching to warmup clears any RPE rating
                                // AND clears the completed flag (warmups don't
                                // count as completed working sets).
                                onUpdateSet(
                                    index,
                                    set.copy(
                                        isWarmup = it,
                                        rpe = if (it) null else set.rpe,
                                        isCompleted = if (it) false else set.isCompleted,
                                    )
                                )
                            },
                            onRpeChange = { onUpdateSet(index, set.copy(rpe = it)) },
                            onCompletedToggle = { onUpdateSet(index, set.copy(isCompleted = it)) },
                            onDelete = { onDeleteSet(index) },
                            onSetCompleted = onSetCompleted,
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

@Composable
private fun SupersetBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                Icons.Default.Link,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = "Superset",
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
