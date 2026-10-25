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
    // user clears one of them. Manual toggling via the check circle still
    // works on top of this.
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
            SetNumberCircle(
                setNumber = setNumber,
                isWarmup = isWarmup,
                isCompleted = isCompleted,
                modifier = Modifier.width(28.dp),
            )

            CompactNumberField(
                value = weight,
                onValueChange = { onWeightChange(it.filter { c -> c.isDigit() || c == '.' }) },
                placeholder = weightUnit,
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f),
            )

            CompactNumberField(
                value = reps,
                onValueChange = { onRepsChange(it.filter { c -> c.isDigit() }) },
                placeholder = "reps",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )

            // Compact warmup toggle pill. Tapping flips the warmup state; the
            // completion logic above re-evaluates automatically.
            WarmupPill(checked = isWarmup, onClick = { onWarmupToggle(!isWarmup) })

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
 * Leading set indicator: a 28dp circle with the set number. Container-high
 * normally; primary fill when the set is completed; tertiary tint with a "W"
 * for warmups.
 */
@Composable
private fun SetNumberCircle(
    setNumber: Int,
    isWarmup: Boolean,
    isCompleted: Boolean,
    modifier: Modifier = Modifier,
) {
    val bg by animateColorAsState(
        targetValue = when {
            isWarmup -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f)
            isCompleted -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "setNumberBg",
    )
    val fg = when {
        isWarmup -> MaterialTheme.colorScheme.tertiary
        isCompleted -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (isWarmup) "W" else "$setNumber",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = fg,
        )
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

/** Small "W" pill toggling warmup state. Tertiary tint when checked. */
@Composable
private fun WarmupPill(checked: Boolean, onClick: () -> Unit) {
    val bg = if (checked) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f)
    else MaterialTheme.colorScheme.surfaceContainerHigh
    val fg = if (checked) MaterialTheme.colorScheme.tertiary
    else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .size(width = 32.dp, height = 28.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "W",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = fg,
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
