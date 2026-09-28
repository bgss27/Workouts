package com.fittrack.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private val PRESET_SECONDS = listOf(60, 90, 120, 180, 300)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestTimerSheet(
    endsAtMillis: Long?,
    onStartTimer: (Long) -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit,
) {
    val remaining = rememberCountdown(endsAtMillis)
    val isRunning = endsAtMillis != null && remaining > 0
    val isFinished = endsAtMillis != null && remaining == 0

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Rest Timer", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = formatRestTime(remaining),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = if (isFinished) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text("Quick start", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                PRESET_SECONDS.forEach { seconds ->
                    PresetChip(
                        seconds = seconds,
                        modifier = Modifier.weight(1f),
                        onClick = { onStartTimer(System.currentTimeMillis() + seconds * 1000L) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            if (isRunning) {
                FilledTonalButton(
                    onClick = onCancelTimer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cancel timer")
                }
            } else if (isFinished) {
                Button(
                    onClick = onCancelTimer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun RestTimerChip(
    endsAtMillis: Long,
    onClick: () -> Unit,
) {
    val remaining = rememberCountdown(endsAtMillis)
    AssistChip(
        onClick = onClick,
        leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(18.dp)) },
        label = { Text(formatRestTime(remaining), style = MaterialTheme.typography.labelMedium) }
    )
}

/**
 * Counts down from now until [endsAtMillis] (epoch). Returns the remaining
 * whole seconds, recomposing while the timer is running. When [endsAtMillis]
 * is null, returns 0.
 */
@Composable
fun rememberCountdown(endsAtMillis: Long?): Int {
    if (endsAtMillis == null) return 0
    var now by remember(endsAtMillis) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(endsAtMillis) {
        while (System.currentTimeMillis() < endsAtMillis) {
            now = System.currentTimeMillis()
            delay(200)
        }
        now = endsAtMillis
    }
    return ((endsAtMillis - now) / 1000L).coerceAtLeast(0L).toInt()
}

@Composable
private fun PresetChip(seconds: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp)
    ) {
        Text(
            text = formatPreset(seconds),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

private fun formatPreset(seconds: Int): String = when {
    seconds < 60 -> "${seconds}s"
    seconds % 60 == 0 -> "${seconds / 60}m"
    else -> "${seconds / 60}m ${seconds % 60}s"
}

private fun formatRestTime(totalSeconds: Int): String {
    val mm = totalSeconds / 60
    val ss = totalSeconds % 60
    return "%d:%02d".format(mm, ss)
}
