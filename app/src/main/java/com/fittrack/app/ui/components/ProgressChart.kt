package com.fittrack.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.fittrack.app.domain.model.ProgressDataPoint

@Composable
fun ProgressChart(
    dataPoints: List<ProgressDataPoint>,
    label: String,
    valueSelector: (ProgressDataPoint) -> Double,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary
) {
    if (dataPoints.isEmpty()) return

    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val values = dataPoints.map { valueSelector(it) }
        val minValue = values.minOrNull() ?: 0.0
        val maxValue = values.maxOrNull() ?: 1.0
        val range = if (maxValue - minValue == 0.0) 1.0 else maxValue - minValue

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "%.1f".format(maxValue),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                text = "%.1f".format(minValue),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            val width = size.width
            val height = size.height
            val padding = 8f

            if (values.size == 1) {
                drawCircle(
                    color = lineColor,
                    radius = 6f,
                    center = Offset(width / 2, height / 2)
                )
                return@Canvas
            }

            val stepX = (width - 2 * padding) / (values.size - 1)
            val path = Path()

            values.forEachIndexed { index, value ->
                val x = padding + index * stepX
                val y = height - padding - ((value - minValue) / range * (height - 2 * padding)).toFloat()

                if (index == 0) path.moveTo(x, y)
                else path.lineTo(x, y)

                // Draw data point
                drawCircle(
                    color = lineColor,
                    radius = 4f,
                    center = Offset(x, y)
                )
            }

            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 3f, cap = StrokeCap.Round)
            )
        }

        // Stats row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val latest = values.lastOrNull() ?: 0.0
            val first = values.firstOrNull() ?: 0.0
            val change = if (first > 0) ((latest - first) / first * 100) else 0.0

            StatItem("Current", "%.1f".format(latest))
            StatItem("Best", "%.1f".format(values.maxOrNull() ?: 0.0))
            StatItem("Change", "%+.1f%%".format(change))
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}
