package com.fittrack.app.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fittrack.app.data.entity.Equipment
import com.fittrack.app.data.entity.MuscleGroup

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeWorkoutSheet(
    defaultGroup: MuscleGroup?,
    defaultEquipment: Set<Equipment>,
    onGenerate: (MuscleGroup, Set<Equipment>) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedGroup by remember { mutableStateOf(defaultGroup ?: MuscleGroup.CHEST) }
    var selectedEquipment by remember { mutableStateOf(defaultEquipment.ifEmpty { setOf(Equipment.BODYWEIGHT) }) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SelfImprovement, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Home Workout",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = "Pick the muscle group you want to train and what equipment you've got. We'll build a quick session from the catalog.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp),
            )

            Spacer(Modifier.height(16.dp))
            Text("Muscle group", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(MuscleGroup.entries.toTypedArray()) { group ->
                    FilterChip(
                        selected = selectedGroup == group,
                        onClick = { selectedGroup = group },
                        label = { Text(group.displayName) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Equipment available", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Equipment.entries.forEach { eq ->
                    val selected = eq in selectedEquipment
                    FilterChip(
                        selected = selected,
                        onClick = {
                            selectedEquipment = if (selected) selectedEquipment - eq
                            else selectedEquipment + eq
                        },
                        label = { Text(eq.displayName) },
                        leadingIcon = if (selected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                    )
                }
            }
            Text(
                text = "Tip: leaving everything off won't generate anything. Bodyweight covers most exercises at home.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                modifier = Modifier.padding(top = 6.dp),
            )

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { onGenerate(selectedGroup, selectedEquipment) },
                enabled = selectedEquipment.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Generate workout")
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}
