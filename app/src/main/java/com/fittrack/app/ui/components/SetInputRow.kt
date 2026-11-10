package com.fittrack.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fittrack.app.data.DurationFormat
import com.fittrack.app.data.entity.SetType
import com.fittrack.app.data.entity.TrackingMode

private val RPE_VALUES = listOf(6, 7, 8, 9, 10)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetInputRow(
    setNumber: Int,
    reps: String,
    weight: String,
    setType: SetType,
    rpe: Int?,
    isCompleted: Boolean,
    onRepsChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onSetTypeChange: (SetType) -> Unit,
    onRpeChange: (Int?) -> Unit,
    onCompletedToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    weightUnit: String = "kg",
    /** Fires once when a set transitions from incomplete → complete. */
    onSetCompleted: () -> Unit = {},
    /** "60 × 8" from the last session for this set index; shown while the row is empty. */
    previousHint: String? = null,
    onFillPrevious: (() -> Unit)? = null,
    /** Which fields this exercise records; drives the visible inputs. */
    trackingMode: TrackingMode = TrackingMode.WEIGHT_REPS,
    duration: String = "",
    distance: String = "",
    onDurationChange: (String) -> Unit = {},
    onDistanceChange: (String) -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current
    val isWarmup = setType == SetType.WARMUP

    // Auto-mark complete the first time a non-warmup set has every field the
    // mode uses populated with valid positive numbers. Auto-mark incomplete
    // if the user clears one of them. Manual toggling via the check circle
    // still works on top of this.
    val canBeComplete = SetData(
        reps = reps, weight = weight, setType = setType, duration = duration, distance = distance,
    ).canAutoCompleteFor(trackingMode)
    LaunchedEffect(canBeComplete, isWarmup) {
        if (canBeComplete && !isCompleted) {
            onCompletedToggle(true)
        } else if (!canBeComplete && isCompleted) {
            // Either the user blanked a field or just toggled the row to warmup
            // (warmups can't be "completed" in our model).
            onCompletedToggle(false)
        }
    }

    // Fire the haptic + onSetCompleted callback exactly once per false→true
    // transition (whether auto or manual).
    var wasCompleted by remember { mutableStateOf(isCompleted) }
    LaunchedEffect(isCompleted) {
        if (isCompleted && !wasCompleted) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onSetCompleted()
        }
        wasCompleted = isCompleted
    }

    val rowBg by animateColorAsState(
        targetValue = if (isCompleted)
            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        else Color.Transparent,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "setRowBg",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(rowBg)
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SetTypeBadge(
                setNumber = setNumber,
                setType = setType,
                isCompleted = isCompleted,
                onSelect = { type ->
                    // Switching to warm-up clears RPE and the completed flag
                    // (warm-ups don't count as completed working sets).
                    onSetTypeChange(type)
                },
                modifier = Modifier.width(28.dp),
            )

            if (trackingMode.usesWeight) {
                CompactNumberField(
                    value = weight,
                    onValueChange = { onWeightChange(it.filter { c -> c.isDigit() || c == '.' }) },
                    placeholder = weightUnit,
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f),
                )
            }

            if (trackingMode.usesReps) {
                CompactNumberField(
                    value = reps,
                    onValueChange = { onRepsChange(it.filter { c -> c.isDigit() }) },
                    placeholder = "reps",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
            }

            if (trackingMode.usesDuration) {
                CompactNumberField(
                    value = duration,
                    onValueChange = { onDurationChange(DurationFormat.sanitize(it)) },
                    placeholder = "m:ss",
                    // Phone keypad has the colon; plain Number does not.
                    keyboardType = KeyboardType.Phone,
                    modifier = Modifier.weight(1f),
                )
            }

            if (trackingMode.usesDistance) {
                CompactNumberField(
                    value = distance,
                    onValueChange = { onDistanceChange(it.filter { c -> c.isDigit() || c == '.' }) },
                    placeholder = com.fittrack.app.data.DistanceUnits.label(trackingMode, weightUnit),
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f),
                )
            }

            // Completion toggle: outlined when incomplete, primary fill when
            // complete. Disabled for warmups (they can't be "completed").
            CompletionCircle(
                isCompleted = isCompleted,
                enabled = !isWarmup,
                onClick = { onCompletedToggle(!isCompleted) },
            )

            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Delete set",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        // Last session's numbers, until the user types something.
        val blank = SetData(reps = reps, weight = weight, duration = duration, distance = distance).isBlankFor(trackingMode)
        if (previousHint != null && blank) {
            Text(
                text = "Prev $previousHint",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 36.dp, top = 2.dp)
                    .clip(CircleShape)
                    .then(if (onFillPrevious != null) Modifier.clickable(onClick = onFillPrevious) else Modifier)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }

        // RPE row — hidden for warmup sets (rating warmups isn't conventional)
        if (!isWarmup) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 36.dp, top = 4.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "RPE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 4.dp)
                )
                RPE_VALUES.forEach { value ->
                    RpeChip(
                        value = value,
                        selected = rpe == value,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onRpeChange(if (rpe == value) null else value)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Leading set indicator and set-type picker: a 28dp circle showing the set
 * number (or W / D / F for warm-up, drop set, failure). Tapping opens a menu
 * to change the type.
 */
@Composable
private fun SetTypeBadge(
    setNumber: Int,
    setType: SetType,
    isCompleted: Boolean,
    onSelect: (SetType) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    val (bgTarget, fg) = when (setType) {
        SetType.WARMUP -> scheme.surfaceContainerHigh to scheme.onSurfaceVariant
        SetType.DROPSET -> scheme.tertiary.copy(alpha = 0.18f) to scheme.tertiary
        SetType.FAILURE -> scheme.primary.copy(alpha = 0.18f) to scheme.primary
        SetType.WORKING ->
            if (isCompleted) scheme.primary to scheme.onPrimary
            else scheme.surfaceContainerHigh to scheme.onSurface
    }
    val bg by animateColorAsState(
        targetValue = bgTarget,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "setNumberBg",
    )
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(bg)
                .clickable { open = true },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = setType.badge ?: "$setNumber",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = fg,
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            SetType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.displayName) },
                    leadingIcon = {
                        Text(
                            text = type.badge ?: "#",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (type == setType) scheme.primary else scheme.onSurfaceVariant,
                        )
                    },
                    onClick = {
                        open = false
                        if (type != setType) onSelect(type)
                    },
                )
            }
        }
    }
}

/** 28dp check circle: 1dp outline when incomplete, primary fill when complete. */
@Composable
private fun CompletionCircle(
    isCompleted: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val bg by animateColorAsState(
        targetValue = if (isCompleted) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "completeBtnBg",
    )
    val outline = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.4f)
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(bg)
            .then(if (!isCompleted) Modifier.border(1.dp, outline, CircleShape) else Modifier)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isCompleted) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Mark set incomplete",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/**
 * Compact filled numeric input: 44dp tall, centered text, no label. Built on
 * BasicTextField + the Material decoration box so we can shrink the content
 * padding below the 56dp default while keeping [fitTextFieldColors].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val colors = fitTextFieldColors()
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.height(44.dp),
        singleLine = true,
        textStyle = MaterialTheme.typography.titleMedium.copy(
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        interactionSource = interactionSource,
    ) { innerTextField ->
        TextFieldDefaults.DecorationBox(
            value = value,
            innerTextField = innerTextField,
            enabled = true,
            singleLine = true,
            visualTransformation = VisualTransformation.None,
            interactionSource = interactionSource,
            placeholder = {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            shape = MaterialTheme.shapes.small,
            colors = colors,
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
        )
    }
}

@Composable
private fun RpeChip(value: Int, selected: Boolean, onClick: () -> Unit) {
    val container = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceContainerHigh
    val content = if (selected) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .size(width = 36.dp, height = 26.dp)
            .clip(CircleShape)
            .background(container)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            color = content,
        )
    }
}
