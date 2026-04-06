package com.fittrack.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun SetInputRow(
    setNumber: Int,
    reps: String,
    weight: String,
    isWarmup: Boolean,
    onRepsChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onWarmupToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = if (isWarmup) "W" else "$setNumber",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(24.dp),
            color = if (isWarmup) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
        )

        OutlinedTextField(
            value = weight,
            onValueChange = { onWeightChange(it.filter { c -> c.isDigit() || c == '.' }) },
            label = { Text("kg") },
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

        IconButton(onClick = { onWarmupToggle(!isWarmup) }) {
            Text(
                text = "W",
                color = if (isWarmup) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                style = MaterialTheme.typography.labelMedium
            )
        }

        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete set",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}
