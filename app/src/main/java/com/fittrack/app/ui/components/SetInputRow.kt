package com.fittrack.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

private val RPE_VALUES = listOf(6, 7, 8, 9, 10)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetInputRow(
    setNumber: Int,
    reps: String,
    weight: String,
    isWarmup: Boolean,
    rpe: Int?,
    isCompleted: Boolean,
    onRepsChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onWarmupToggle: (Boolean) -> Unit,
    onRpeChange: (Int?) -> Unit,
    onCompletedToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    weightUnit: String = "kg",
    /** Fires once when a set transitions from incomplete → complete. */
    onSetCompleted: () -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current

    // Auto-mark complete the first time a non-warmup set has both fields
    // populated with valid positive numbers. Auto-mark incomplete if the
    // user clears one of them. Manual toggling via the leading button
    // still works on top of this.
    val canBeComplete = !isWarmup &&
        (reps.toIntOrNull() ?: 0) > 0 &&
        (weight.toDoubleOrNull() ?: 0.0) > 0.0
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
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
        else Color.Transparent,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "setRowBg",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(rowBg)
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SetCompleteButton(
                setNumber = setNumber,
                isWarmup = isWarmup,
                isCompleted = isCompleted,
                onClick = { onCompletedToggle(!isCompleted) }
            )

            OutlinedTextField(
                value = weight,
                onValueChange = { onWeightChange(it.filter { c -> c.isDigit() || c == '.' }) },
                label = { Text(weightUnit) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )

            OutlinedTextField(
                value = reps,
                onValueChange = { onRepsChange(it.filter { c -> c.isDigit() }) },
                label = { Text("Reps") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            // Compact warmup toggle. Tapping flips the warmup state; the
            // completion logic above re-evaluates automatically.
            FilledIconToggleButton(
                checked = isWarmup,
                onCheckedChange = { onWarmupToggle(it) },
                colors = IconButtonDefaults.filledIconToggleButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    checkedContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                    checkedContentColor = MaterialTheme.colorScheme.tertiary,
                ),
                modifier = Modifier.size(36.dp),
            ) {
                Text(text = "W", style = MaterialTheme.typography.labelMedium)
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete set",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        // RPE row — hidden for warmup sets (rating warmups isn't conventional)
        if (!isWarmup) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 44.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "RPE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
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
 * Leading completion affordance. Renders the set number (or "W" for warmups)
 * inside an outlined circle; on completion it fills with primary color and
 * shows a check. Tappable when not a warmup — warmups can't be "completed".
 */
@Composable
private fun SetCompleteButton(
    setNumber: Int,
    isWarmup: Boolean,
    isCompleted: Boolean,
    onClick: () -> Unit,
) {
    val bg by animateColorAsState(
        targetValue = if (isCompleted) MaterialTheme.colorScheme.primary
        else Color.Transparent,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "completeBtnBg",
    )
    val borderColor =
        if (isWarmup) MaterialTheme.colorScheme.tertiary
        else MaterialTheme.colorScheme.outline
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(bg)
            .then(
                if (!isCompleted) Modifier.border(1.5.dp, borderColor, CircleShape)
                else Modifier
            )
            .clickable(enabled = !isWarmup, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isCompleted) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Mark set incomplete",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp),
            )
        } else {
            Text(
                text = if (isWarmup) "W" else "$setNumber",
                style = MaterialTheme.typography.titleSmall,
                color = if (isWarmup) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun RpeChip(value: Int, selected: Boolean, onClick: () -> Unit) {
    val container = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceVariant
    val content = if (selected) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = container,
        contentColor = content,
        modifier = Modifier.size(width = 36.dp, height = 28.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
