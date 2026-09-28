package com.fittrack.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fittrack.app.domain.model.Suggestion
import com.fittrack.app.ui.theme.*

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

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = suggestion.title,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = suggestion.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
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
