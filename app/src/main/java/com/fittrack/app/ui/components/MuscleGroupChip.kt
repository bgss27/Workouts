package com.fittrack.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fittrack.app.data.entity.MuscleGroup

@Composable
fun MuscleGroupChipRow(
    selectedGroup: MuscleGroup?,
    onGroupSelected: (MuscleGroup?) -> Unit,
    modifier: Modifier = Modifier,
    showAll: Boolean = true
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (showAll) {
            FilterChip(
                selected = selectedGroup == null,
                onClick = { onGroupSelected(null) },
                label = { Text("All") }
            )
        }
        MuscleGroup.entries.forEach { group ->
            FilterChip(
                selected = selectedGroup == group,
                onClick = { onGroupSelected(if (selectedGroup == group) null else group) },
                label = { Text(group.displayName) }
            )
        }
    }
}
