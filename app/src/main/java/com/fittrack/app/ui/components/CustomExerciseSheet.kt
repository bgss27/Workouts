package com.fittrack.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fittrack.app.billing.FreeLimits
import com.fittrack.app.data.entity.Equipment
import com.fittrack.app.data.entity.Exercise
import com.fittrack.app.data.entity.MuscleGroup

/**
 * Create / edit form for a user-defined exercise.
 *
 * - [initial] null = creating; otherwise editing that exercise.
 * - [limitReached] true = the free tier cap is hit and the user isn't Pro;
 *   the sheet shows an upgrade prompt instead of the form (editing an
 *   existing exercise is always allowed).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomExerciseSheet(
    initial: Exercise?,
    limitReached: Boolean,
    onSave: (Exercise) -> Unit,
    onDismiss: () -> Unit,
    onUpgrade: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenGutter)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
        ) {
            if (initial == null && limitReached) {
                LimitReachedBody(onUpgrade = onUpgrade, onDismiss = onDismiss)
            } else {
                FormBody(
                    initial = initial,
                    onSave = onSave,
                    onDismiss = onDismiss,
                    onDelete = onDelete,
                )
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.LimitReachedBody(onUpgrade: () -> Unit, onDismiss: () -> Unit) {
    Text("Custom exercise limit", style = MaterialTheme.typography.titleLarge)
    Spacer(modifier = Modifier.height(12.dp))
    Tile {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon = Icons.Default.Lock, tint = MaterialTheme.colorScheme.tertiary)
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text("Free includes ${FreeLimits.CUSTOM_EXERCISES} custom exercises", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Upgrade to Pro to create as many as you want.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        PrimaryButton(text = "Unlock with Pro", icon = Icons.Default.Star, onClick = onUpgrade)
    }
    Spacer(modifier = Modifier.height(8.dp))
    TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Not now") }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.FormBody(
    initial: Exercise?,
    onSave: (Exercise) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var primary by remember { mutableStateOf(initial?.muscleGroup ?: MuscleGroup.CHEST) }
    var secondary by remember { mutableStateOf(initial?.secondaryMuscleGroup) }
    var equipment by remember { mutableStateOf(initial?.equipment ?: Equipment.DUMBBELL) }
    val canSave = name.trim().length >= 2

    Text(
        if (initial == null) "New custom exercise" else "Edit exercise",
        style = MaterialTheme.typography.titleLarge,
    )
    Spacer(modifier = Modifier.height(16.dp))

    TextField(
        value = name,
        onValueChange = { name = it },
        placeholder = { Text("Exercise name") },
        singleLine = true,
        shape = MaterialTheme.shapes.small,
        colors = fitTextFieldColors(),
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(modifier = Modifier.height(16.dp))
    Overline("Primary muscle", modifier = Modifier.padding(horizontal = 0.dp))
    GroupPills(selected = primary, onSelect = { g -> if (g != null) primary = g })

    Spacer(modifier = Modifier.height(12.dp))
    Overline("Secondary muscle (optional)", modifier = Modifier.padding(horizontal = 0.dp))
    GroupPills(selected = secondary, onSelect = { secondary = it }, allowNone = true)

    Spacer(modifier = Modifier.height(12.dp))
    Overline("Equipment", modifier = Modifier.padding(horizontal = 0.dp))
    PillChipRow(horizontalPadding = 0.dp) {
        Equipment.entries.forEach { e ->
            PillChip(label = e.displayName, selected = equipment == e, onClick = { equipment = e })
        }
    }

    Spacer(modifier = Modifier.height(20.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onDelete != null) {
            OutlinedButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.width(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Delete")
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = onDismiss) { Text("Cancel") }
        Spacer(modifier = Modifier.width(8.dp))
        PrimaryButton(
            text = "Save",
            enabled = canSave,
            onClick = {
                onSave(
                    (initial ?: Exercise(name = "", muscleGroup = primary, isCustom = true)).copy(
                        name = name.trim(),
                        muscleGroup = primary,
                        secondaryMuscleGroup = secondary?.takeIf { it != primary },
                        equipment = equipment,
                        isCustom = true,
                    )
                )
            },
            modifier = Modifier.width(120.dp),
        )
    }
}

@Composable
private fun GroupPills(
    selected: MuscleGroup?,
    onSelect: (MuscleGroup?) -> Unit,
    allowNone: Boolean = false,
) {
    PillChipRow(horizontalPadding = 0.dp) {
        if (allowNone) {
            PillChip(label = "None", selected = selected == null, onClick = { onSelect(null) })
        }
        MuscleGroup.entries.forEach { g ->
            PillChip(label = g.displayName, selected = selected == g, onClick = { onSelect(g) })
        }
    }
}
