package com.fittrack.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.fittrack.app.data.repository.ExerciseImageRepository
import com.fittrack.app.data.repository.ExerciseInstructions
import com.fittrack.app.data.repository.OpenExerciseDb
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Timer
import com.fittrack.app.data.WorkoutPrefs
import com.fittrack.app.data.entity.SetType

data class SetData(
    val id: Long = 0,
    val reps: String = "",
    val weight: String = "",
    val setType: SetType = SetType.WORKING,
    /** RPE 6-10. Null = unrated. Warmup sets are conventionally not rated. */
    val rpe: Int? = null,
    /**
     * Whether the user has marked this set as done. Auto-toggles on when
     * weight + reps are both valid for a non-warmup set, but the user can
     * tap the leading button to manually toggle it back off and edit.
     */
    val isCompleted: Boolean = false,
) {
    val isWarmup: Boolean get() = setType == SetType.WARMUP
}

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
    /** Explicit "done with this exercise" mark and its toggle. */
    isFinished: Boolean = false,
    onFinishToggle: (() -> Unit)? = null,
    // Expanded state is hoisted so the parent can implement an accordion —
    // only the "working" exercise stays open, the rest collapse. When the
    // parent passes null, the card falls back to its own self-managed
    // expanded state (default open).
    expanded: Boolean? = null,
    onExpandToggle: (() -> Unit)? = null,
    /** Effective rest for this exercise and whether it is the user's default. */
    restSeconds: Int? = null,
    restIsDefault: Boolean = true,
    onRestClick: (() -> Unit)? = null,
    /** Persistent per-exercise cue, e.g. "seat 4, grip at rings". */
    notes: String? = null,
    onNotesChange: ((String) -> Unit)? = null,
    /** "60 × 8" hints from the last session, by set index. */
    previousHints: List<String> = emptyList(),
    onFillPrevious: ((Int) -> Unit)? = null,
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

    val accent = if (inSuperset) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Tile(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(0.dp),
    ) {
        // Header: icon badge, name + muscle group, trailing actions.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { toggle() }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Done exercises swap the dumbbell badge for a check and dim the title.
            IconBadge(
                icon = if (isFinished) Icons.Default.CheckCircle else Icons.Default.FitnessCenter,
                tint = if (isFinished) MaterialTheme.colorScheme.primary else accent,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isFinished) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = muscleGroup,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (isFinished) DonePill()
                    if (inSuperset) SupersetBadge()
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onSwap != null) {
                    IconButton(onClick = onSwap, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Swap exercise",
                            tint = muted,
                        )
                    }
                }
                if (canToggleSuperset || inSuperset) {
                    IconButton(onClick = onSupersetToggle, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = if (inSuperset) Icons.Default.LinkOff else Icons.Default.Link,
                            contentDescription = if (inSuperset) "Remove from superset" else "Group with previous as superset",
                            tint = muted,
                        )
                    }
                }
                IconButton(onClick = onRemoveExercise, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove exercise", tint = muted)
                }
                IconButton(onClick = toggle, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = muted,
                    )
                }
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column {
                Column(modifier = Modifier.padding(horizontal = 14.dp)) {
                    // Rest length chip + persistent notes line.
                    if (onRestClick != null && restSeconds != null) {
                        AssistChip(
                            onClick = onRestClick,
                            leadingIcon = {
                                Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = {
                                Text(
                                    "Rest ${WorkoutPrefs.formatRest(restSeconds)}" + if (restIsDefault) "" else " ·",
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            },
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                    if (onNotesChange != null) {
                        ExerciseNotesLine(notes = notes, onSave = onNotesChange)
                    }

                    // How-to text — shown for any exercise we have written
                    // instructions for. Especially useful for bodyweight
                    // exercises pulled in by the "home workout" generator,
                    // since many of them have no image and may be unfamiliar.
                    if (instructions != null) {
                        Text(
                            text = instructions,
                            style = MaterialTheme.typography.bodySmall,
                            color = muted,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                    }
                    if (hasForm) {
                        TextButton(
                            onClick = { showForm = !showForm },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        ) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
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

                    // Set table header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SetHeaderLabel("SET", modifier = Modifier.width(28.dp))
                        SetHeaderLabel(weightUnit.uppercase(), modifier = Modifier.weight(1f))
                        SetHeaderLabel("REPS", modifier = Modifier.weight(1f))
                        // Matches the row's trailing cluster: check (28) +
                        // delete (28) with an 8dp gap.
                        Spacer(modifier = Modifier.width(64.dp))
                    }

                    var workingSetNumber = 0
                    sets.forEachIndexed { index, set ->
                        if (!set.isWarmup) workingSetNumber++
                        SetInputRow(
                            setNumber = workingSetNumber,
                            reps = set.reps,
                            weight = set.weight,
                            setType = set.setType,
                            rpe = set.rpe,
                            isCompleted = set.isCompleted,
                            weightUnit = weightUnit,
                            onRepsChange = { onUpdateSet(index, set.copy(reps = it)) },
                            onWeightChange = { onUpdateSet(index, set.copy(weight = it)) },
                            onSetTypeChange = { type ->
                                // Switching to warmup clears any RPE rating
                                // AND clears the completed flag (warmups don't
                                // count as completed working sets).
                                val warm = type == SetType.WARMUP
                                onUpdateSet(
                                    index,
                                    set.copy(
                                        setType = type,
                                        rpe = if (warm) null else set.rpe,
                                        isCompleted = if (warm) false else set.isCompleted,
                                    )
                                )
                            },
                            previousHint = previousHints.getOrNull(index),
                            onFillPrevious = onFillPrevious?.let { fill -> { fill(index) } },
                            onRpeChange = { onUpdateSet(index, set.copy(rpe = it)) },
                            onCompletedToggle = { onUpdateSet(index, set.copy(isCompleted = it)) },
                            onDelete = { onDeleteSet(index) },
                            onSetCompleted = onSetCompleted,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Footer strip on a container-high fill that takes the tile's
                // bottom corners: "Add set" on the left, "Done" / "Reopen" on
                // the right (when the parent supports finishing).
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onAddSet)
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Add set",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (onFinishToggle != null) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(24.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(onClick = onFinishToggle)
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                if (isFinished) Icons.Default.Refresh else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isFinished) "Reopen" else "Done",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * One-line exercise note with inline editing. Saved notes persist on the
 * exercise itself, so they show up again next session.
 */
@Composable
private fun ExerciseNotesLine(notes: String?, onSave: (String) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    var draft by remember(notes, editing) { mutableStateOf(notes ?: "") }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val focus = remember { FocusRequester() }
    if (editing) {
        LaunchedEffect(Unit) { focus.requestFocus() }
        Column(modifier = Modifier.padding(bottom = 6.dp)) {
            TextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("Setup cue, e.g. seat 4, grip at rings") },
                singleLine = false,
                maxLines = 3,
                shape = MaterialTheme.shapes.small,
                colors = fitTextFieldColors(),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { editing = false }) { Text("Cancel") }
                TextButton(onClick = { onSave(draft.trim()); editing = false }) { Text("Save") }
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .clickable { editing = true }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Default.Notes, contentDescription = "Exercise notes", tint = muted, modifier = Modifier.size(16.dp))
            Text(
                text = notes?.takeIf { it.isNotBlank() } ?: "Add note",
                style = MaterialTheme.typography.bodySmall,
                color = if (notes.isNullOrBlank()) muted else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun SetHeaderLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = modifier,
    )
}

/** Small pill marking an exercise as part of a superset. */
/** Small lime pill shown on a finished exercise's header. */
@Composable
private fun DonePill(modifier: Modifier = Modifier) {
    Text(
        text = "DONE",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onPrimary,
        modifier = modifier
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
            .padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

@Composable
private fun SupersetBadge(modifier: Modifier = Modifier) {
    val fg = MaterialTheme.colorScheme.secondary
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(fg.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            Icons.Default.Link,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = "Superset",
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
