package com.fittrack.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.fittrack.app.data.entity.MuscleGroup

@Composable
fun MuscleGroupChipRow(
    selectedGroup: MuscleGroup?,
    onGroupSelected: (MuscleGroup?) -> Unit,
    modifier: Modifier = Modifier,
    showAll: Boolean = true
) {
    PillChipRow(modifier = modifier) {
        if (showAll) {
            PillChip(label = "All", selected = selectedGroup == null, onClick = { onGroupSelected(null) })
        }
        MuscleGroup.entries.forEach { group ->
            PillChip(
                label = group.displayName,
                selected = selectedGroup == group,
                onClick = { onGroupSelected(if (selectedGroup == group) null else group) },
            )
        }
    }
}
