package com.fittrack.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fittrack.app.data.WorkoutPrefs

/**
 * Pick a rest length. [current] null means "use the default"; selecting the
 * first row returns null so callers can store "no override".
 */
@Composable
fun RestPickerDialog(
    current: Int?,
    defaultSeconds: Int,
    onSelect: (Int?) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Rest between sets",
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                RestOption(
                    label = "Default (${WorkoutPrefs.formatRest(defaultSeconds)})",
                    selected = current == null,
                    onClick = { onSelect(null); onDismiss() },
                )
                WorkoutPrefs.REST_CHOICES.forEach { seconds ->
                    RestOption(
                        label = WorkoutPrefs.formatRest(seconds),
                        selected = current == seconds,
                        onClick = { onSelect(seconds); onDismiss() },
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun RestOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
