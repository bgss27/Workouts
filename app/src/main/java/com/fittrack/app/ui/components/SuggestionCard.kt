package com.fittrack.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fittrack.app.domain.model.Suggestion
import com.fittrack.app.ui.theme.*

/**
 * Improvement tip row: icon badge tinted by the suggestion's semantic color,
 * title, muted description, and a trailing arrow when the tip is actionable.
 */
@Composable
fun SuggestionCard(
    suggestion: Suggestion,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val c = FitTrackTheme.colors
    val (icon, color) = when (suggestion) {
        is Suggestion.IncreaseWeight -> Icons.Default.TrendingUp to c.success
        is Suggestion.IncreaseVolume -> Icons.Default.Add to c.warning
        is Suggestion.IncreaseFrequency -> Icons.Default.Repeat to Blue40
        is Suggestion.Deload -> Icons.Default.TrendingDown to c.danger
        is Suggestion.TryExercise -> Icons.Default.FitnessCenter to Teal40
        is Suggestion.MuscleGroupSummary -> when (suggestion.trend) {
            "improving" -> Icons.Default.TrendingUp to c.success
            "plateaued" -> Icons.Default.TrendingFlat to c.warning
            else -> Icons.Default.TrendingDown to c.danger
        }
    }

    Tile(modifier = modifier, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            IconBadge(icon = icon, tint = color)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = suggestion.title,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = suggestion.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (onClick != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Apply suggestion",
                    tint = color,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
