package com.fittrack.app.ui.bodymetrics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fittrack.app.data.WeightUnits
import com.fittrack.app.data.entity.BodyMetric
import com.fittrack.app.data.entity.BodyMetricType
import com.fittrack.app.di.AppModule
import com.fittrack.app.ui.components.DetailHeader
import com.fittrack.app.ui.components.EmptyState
import com.fittrack.app.ui.components.IconBadge
import com.fittrack.app.ui.components.PillChip
import com.fittrack.app.ui.components.PrimaryButton
import com.fittrack.app.ui.components.ScreenGutter
import com.fittrack.app.ui.components.SectionHeader
import com.fittrack.app.ui.components.Tile
import androidx.compose.foundation.background
import com.fittrack.app.ui.components.HeroCard
import com.fittrack.app.ui.components.HeroStat
import com.fittrack.app.ui.components.LineTrendChart
import com.fittrack.app.ui.components.TrendStats
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyMetricsScreen(
    appModule: AppModule,
    isPro: Boolean,
    onUpgrade: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: BodyMetricsViewModel = viewModel(
        factory = BodyMetricsViewModel.Factory(context, appModule.bodyMetricRepository)
    )
    val units = remember { BodyUnits(WeightUnits.preferred(context)) }

    val metrics by viewModel.metrics.collectAsState()
    val latestByType by viewModel.latestByType.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val series by viewModel.series.collectAsState()

    // null = sheet closed; BodyMetric(id = 0) = new entry; otherwise editing.
    var editing by remember { mutableStateOf<BodyMetric?>(null) }
    var pendingDelete by remember { mutableStateOf<BodyMetric?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { DetailHeader(title = "Body Metrics", onBack = onBack) },
        bottomBar = {
            if (isPro && metrics.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .navigationBarsPadding()
                        .padding(horizontal = ScreenGutter, vertical = 12.dp)
                ) {
                    PrimaryButton(
                        text = "Log check-in",
                        icon = Icons.Default.Add,
                        onClick = { editing = BodyMetric() },
                    )
                }
            }
        }
    ) { padding ->
        if (!isPro) {
            BodyMetricsPaywall(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                onUpgrade = onUpgrade,
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = ScreenGutter, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (metrics.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.MonitorWeight,
                        title = "No check-ins yet",
                        subtitle = "Log your weight and measurements to start tracking how your body changes alongside your training.",
                        actionLabel = "Log first check-in",
                        onAction = { editing = BodyMetric() },
                    )
                }
                return@LazyColumn
            }

            // Latest snapshot
            item {
                LatestSnapshotCard(
                    latestByType = latestByType,
                    units = units,
                )
            }

            // Metric selector
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(BodyMetricType.entries) { type ->
                        PillChip(
                            label = type.displayName,
                            selected = type == selectedType,
                            onClick = { viewModel.selectType(type) },
                        )
                    }
                }
            }

            // Trend chart
            item {
                Tile(contentPadding = PaddingValues(0.dp)) {
                    if (series.isEmpty()) {
                        Text(
                            text = "No ${selectedType.displayName.lowercase()} logged yet. Add it to your next check-in to see a trend here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    } else {
                        LineTrendChart(
                            values = series.map { units.toDisplay(selectedType, it.value) },
                            label = "${selectedType.displayName} (${units.unitLabel(selectedType)})",
                            stats = TrendStats.CHANGE,
                            formatValue = { units.formatNumber(it) },
                        )
                    }
                }
            }

            // History
            item {
                SectionHeader(title = "History", modifier = Modifier.padding(top = 8.dp))
            }
            items(metrics, key = { it.id }) { metric ->
                CheckInRow(
                    metric = metric,
                    units = units,
                    onClick = { editing = metric },
                    onDelete = { pendingDelete = metric },
                )
            }
        }
    }

    editing?.let { current ->
        CheckInSheet(
            initial = current,
            units = units,
            onDismiss = { editing = null },
            onSave = { viewModel.save(it); editing = null },
            onDelete = if (current.id != 0L) {
                { pendingDelete = current; editing = null }
            } else null,
        )
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete check-in?") },
            text = { Text("This removes the ${formatDate(target.recordedAt)} entry. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(target); pendingDelete = null }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
        )
    }
}

// region Pieces

@Composable
private fun LatestSnapshotCard(
    latestByType: Map<BodyMetricType, com.fittrack.app.data.repository.BodyMetricPoint>,
    units: BodyUnits,
    modifier: Modifier = Modifier,
) {
    HeroCard(modifier = modifier) {
        Text(
            text = "Latest",
            style = MaterialTheme.typography.labelLarge,
            color = com.fittrack.app.ui.theme.VoltInk.copy(alpha = 0.8f),
        )
        Spacer(modifier = Modifier.height(12.dp))
        val present = BodyMetricType.entries.filter { it in latestByType }
        // Rows of up to three stats keep the big numbers readable on phones.
        present.chunked(3).forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                row.forEach { type ->
                    val point = latestByType.getValue(type)
                    HeroStat(
                        value = units.formatNumber(units.toDisplay(type, point.value)),
                        label = "${type.displayName} ${units.unitLabel(type)}",
                        modifier = Modifier.weight(1f),
                    )
                }
                // Pad short rows so stats stay left-aligned in a grid.
                repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun CheckInRow(
    metric: BodyMetric,
    units: BodyUnits,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val summary = remember(metric, units) {
        BodyMetricType.entries
            .mapNotNull { type -> type.valueOf(metric)?.let { "${type.displayName} ${units.format(type, it)}" } }
            .joinToString(" · ")
    }
    Tile(onClick = onClick, contentPadding = PaddingValues(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon = Icons.Default.MonitorWeight, size = 36.dp)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(formatDate(metric.recordedAt), style = MaterialTheme.typography.titleSmall)
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                metric.notes?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete check-in",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CheckInSheet(
    initial: BodyMetric,
    units: BodyUnits,
    onDismiss: () -> Unit,
    onSave: (BodyMetric) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val zone = remember { ZoneId.systemDefault() }

    var date by remember(initial) {
        mutableStateOf(Instant.ofEpochMilli(initial.recordedAt).atZone(zone).toLocalDate())
    }
    val fields = remember(initial) {
        mutableStateMapOf<BodyMetricType, String>().apply {
            BodyMetricType.entries.forEach { type ->
                put(type, type.valueOf(initial)?.let { units.formatNumber(units.toDisplay(type, it)) } ?: "")
            }
        }
    }
    var notes by remember(initial) { mutableStateOf(initial.notes.orEmpty()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (initial.id == 0L) "Log check-in" else "Edit check-in",
                style = MaterialTheme.typography.titleLarge,
            )

            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(date.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy")))
            }

            MetricField(BodyMetricType.WEIGHT, fields, units)
            MetricField(BodyMetricType.BODY_FAT, fields, units)

            Text("Measurements", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricField(BodyMetricType.WAIST, fields, units, Modifier.weight(1f))
                MetricField(BodyMetricType.CHEST, fields, units, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricField(BodyMetricType.HIPS, fields, units, Modifier.weight(1f))
                MetricField(BodyMetricType.ARM, fields, units, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricField(BodyMetricType.THIGH, fields, units, Modifier.weight(1f))
                Spacer(modifier = Modifier.weight(1f))
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
            )

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (onDelete != null) {
                    OutlinedButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete")
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Button(onClick = {
                    var built = initial.copy(
                        recordedAt = recordedAtFor(date, initial, zone),
                        notes = notes.trim().ifBlank { null },
                    )
                    var invalid: BodyMetricType? = null
                    for (type in BodyMetricType.entries) {
                        val raw = fields[type].orEmpty().trim()
                        val value = if (raw.isEmpty()) null else raw.toDoubleOrNull()
                        if (raw.isNotEmpty() && (value == null || value <= 0.0)) {
                            invalid = type
                            break
                        }
                        built = type.copyWith(built, value?.let { units.toCanonical(type, it) })
                    }
                    when {
                        invalid != null -> error = "${invalid.displayName} must be a positive number."
                        !built.hasAnyValue -> error = "Enter at least one measurement."
                        else -> onSave(built)
                    }
                }) { Text("Save") }
            }
        }
    }

    if (showDatePicker) {
        CheckInDatePicker(
            initial = date,
            onDismiss = { showDatePicker = false },
            onConfirm = { date = it; showDatePicker = false },
        )
    }
}

@Composable
private fun MetricField(
    type: BodyMetricType,
    fields: MutableMap<BodyMetricType, String>,
    units: BodyUnits,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = fields[type].orEmpty(),
        onValueChange = { fields[type] = it.filter { c -> c.isDigit() || c == '.' } },
        label = { Text("${type.displayName} (${units.unitLabel(type)})") },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CheckInDatePicker(
    initial: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    // Material's DatePicker works in UTC-midnight millis; convert via UTC on
    // both ends so negative-offset zones don't land a day early.
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val today = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                return utcTimeMillis <= today
            }
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                pickerState.selectedDateMillis?.let { ms ->
                    onConfirm(Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate())
                } ?: onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state = pickerState)
    }
}

@Composable
private fun BodyMetricsPaywall(
    modifier: Modifier = Modifier,
    onUpgrade: () -> Unit,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        IconBadge(icon = Icons.Default.MonitorWeight, size = 72.dp)
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            "Track your body, not just your lifts",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Log weight, body fat, and tape measurements, then watch the trend lines move alongside your training.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Tile {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text("Body Metrics is a Pro feature", style = MaterialTheme.typography.titleSmall)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Includes: " + BodyMetricType.entries.joinToString(", ") { it.displayName.lowercase() } + ".",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                PrimaryButton(text = "Unlock with Pro", icon = Icons.Default.Star, onClick = onUpgrade)
            }
        }
    }
}

// endregion

// region Helpers

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")

private fun formatDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)

/**
 * Keep the original time-of-day when the date is unchanged (so edits don't
 * reorder same-day entries); otherwise, use now for today and noon for
 * backfilled days so the row sorts sensibly within that date.
 */
private fun recordedAtFor(date: LocalDate, initial: BodyMetric, zone: ZoneId): Long {
    val originalDate = Instant.ofEpochMilli(initial.recordedAt).atZone(zone).toLocalDate()
    if (initial.id != 0L && date == originalDate) return initial.recordedAt
    val time = if (date == LocalDate.now()) LocalTime.now() else LocalTime.NOON
    return date.atTime(time).atZone(zone).toInstant().toEpochMilli()
}

// endregion
