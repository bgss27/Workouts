package com.fittrack.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.math.abs

enum class PlateUnit(
    val label: String,
    val defaultBar: Double,
    val inventory: List<Double>
) {
    KG("kg", defaultBar = 20.0, inventory = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)),
    LB("lb", defaultBar = 45.0, inventory = listOf(45.0, 35.0, 25.0, 10.0, 5.0, 2.5)),
}

data class PlateBreakdown(
    val platesPerSide: List<Double>,
    val achievedTotal: Double,
    val isExact: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlateCalculatorSheet(
    onDismiss: () -> Unit,
    defaultUnit: PlateUnit = PlateUnit.KG,
) {
    var unit by rememberSaveable { mutableStateOf(defaultUnit) }
    var targetText by rememberSaveable { mutableStateOf("") }
    var barText by rememberSaveable(unit) { mutableStateOf(formatWeight(unit.defaultBar)) }

    val target = targetText.toDoubleOrNull()
    val bar = barText.toDoubleOrNull() ?: unit.defaultBar
    val breakdown = remember(target, bar, unit) {
        target?.let { calculatePlates(targetTotal = it, bar = bar, inventory = unit.inventory) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text("Plate Calculator", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))

            // Unit toggle
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                PlateUnit.entries.forEachIndexed { index, u ->
                    SegmentedButton(
                        selected = unit == u,
                        onClick = { unit = u },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = PlateUnit.entries.size
                        )
                    ) {
                        Text(u.label.uppercase())
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Target weight + bar weight
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Target (${unit.label})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = barText,
                    onValueChange = { barText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Bar (${unit.label})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when {
                target == null -> {
                    Text(
                        "Enter a target weight to see the plate breakdown.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                target < bar -> {
                    Text(
                        "Target is less than the bar weight (${formatWeight(bar)} ${unit.label}).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                breakdown != null -> {
                    PlateBreakdownView(breakdown = breakdown, unit = unit)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text("Close")
            }
        }
    }
}

@Composable
private fun PlateBreakdownView(breakdown: PlateBreakdown, unit: PlateUnit) {
    Column {
        Text("Plates per side", style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(8.dp))
        if (breakdown.platesPerSide.isEmpty()) {
            Text(
                "Just the bar — no plates needed.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            // Group by plate size and show count × size
            val grouped = breakdown.platesPerSide.groupBy { it }
                .toSortedMap(compareByDescending { it })
            grouped.forEach { (plate, copies) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${copies.size} × ${formatWeight(plate)} ${unit.label}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        val statusIcon = if (breakdown.isExact) Icons.Default.CheckCircle else Icons.Default.Info
        val statusTint = if (breakdown.isExact) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.tertiary
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = statusIcon,
                contentDescription = null,
                tint = statusTint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                if (breakdown.isExact)
                    "Total ${formatWeight(breakdown.achievedTotal)} ${unit.label}"
                else
                    "Closest available: ${formatWeight(breakdown.achievedTotal)} ${unit.label}",
                style = MaterialTheme.typography.bodyMedium,
                color = statusTint
            )
        }
    }
}

internal fun calculatePlates(
    targetTotal: Double,
    bar: Double,
    inventory: List<Double>,
): PlateBreakdown {
    val perSide = (targetTotal - bar) / 2.0
    if (perSide <= 0) {
        return PlateBreakdown(emptyList(), bar, isExact = abs(targetTotal - bar) < 1e-6)
    }
    val plates = mutableListOf<Double>()
    var remaining = perSide
    for (plate in inventory.sortedDescending()) {
        while (remaining + 1e-9 >= plate) {
            plates += plate
            remaining -= plate
        }
    }
    val achievedTotal = bar + 2 * plates.sum()
    return PlateBreakdown(
        platesPerSide = plates,
        achievedTotal = achievedTotal,
        isExact = abs(targetTotal - achievedTotal) < 1e-6
    )
}

private fun formatWeight(value: Double): String =
    if (abs(value - value.toInt()) < 1e-6) value.toInt().toString()
    else "%.2f".format(value).trimEnd('0').trimEnd('.')
