package com.fittrack.app.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fittrack.app.di.AppModule
import com.fittrack.app.domain.analysis.DeloadAdvice
import com.fittrack.app.domain.analysis.VolumeAnalyzer
import com.fittrack.app.domain.analysis.VolumeStatus
import com.fittrack.app.domain.analysis.WeeklyVolumeReport
import com.fittrack.app.domain.analysis.WeeklyVolumeRow
import com.fittrack.app.ui.components.DetailHeader
import com.fittrack.app.ui.components.IconBadge
import com.fittrack.app.ui.components.PillChip
import com.fittrack.app.ui.components.ScreenGutter
import com.fittrack.app.ui.components.Tile
import com.fittrack.app.ui.theme.FitTrackTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

class VolumeGuidanceViewModel(private val analyzer: VolumeAnalyzer) : ViewModel() {
    private val _weekOffset = MutableStateFlow(0)
    val weekOffset: StateFlow<Int> = _weekOffset

    private val _report = MutableStateFlow<WeeklyVolumeReport?>(null)
    val report: StateFlow<WeeklyVolumeReport?> = _report

    init { load() }

    fun selectWeek(offset: Int) {
        _weekOffset.value = offset
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _report.value = runCatching { analyzer.report(_weekOffset.value) }.getOrNull()
        }
    }

    class Factory(private val analyzer: VolumeAnalyzer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = VolumeGuidanceViewModel(analyzer) as T
    }
}

/** Hard sets per muscle group this week against the productive band, plus deload advice. */
@Composable
fun VolumeGuidanceScreen(appModule: AppModule, onBack: () -> Unit) {
    val viewModel: VolumeGuidanceViewModel = viewModel(
        factory = VolumeGuidanceViewModel.Factory(appModule.volumeAnalyzer)
    )
    val report by viewModel.report.collectAsState()
    val weekOffset by viewModel.weekOffset.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { DetailHeader(title = "Weekly Volume", onBack = onBack) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = ScreenGutter, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillChip(label = "This week", selected = weekOffset == 0, onClick = { viewModel.selectWeek(0) })
                    PillChip(label = "Last week", selected = weekOffset == 1, onClick = { viewModel.selectWeek(1) })
                }
            }
            val r = report
            if (r == null) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                return@LazyColumn
            }
            item {
                val fmt = DateTimeFormatter.ofPattern("MMM d")
                Text(
                    "${r.weekStart.format(fmt)} – ${r.weekEnd.format(fmt)} · ${r.groupsInRange} of ${r.rows.count { it.sets > 0 }} trained groups in range",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Tile(contentPadding = PaddingValues(vertical = 4.dp)) {
                    r.rows.forEachIndexed { i, row ->
                        VolumeRowView(row)
                        if (i < r.rows.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                        }
                    }
                }
            }
            if (r.advice.isNotEmpty()) {
                item {
                    Text(
                        "Deload advice",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                items(r.advice) { advice -> DeloadAdviceCard(advice) }
            }
            item {
                Text(
                    "Most research puts productive hypertrophy volume around 10–20 hard sets per muscle per week " +
                        "(6–12 for abs, forearms and calves). Below the band you're leaving growth on the table; " +
                        "well above it for several weeks, fatigue usually outruns recovery. Warm-ups aren't counted; " +
                        "every other set type is.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
                )
            }
        }
    }
}

@Composable
private fun VolumeRowView(row: WeeklyVolumeRow) {
    val c = FitTrackTheme.colors
    val tint = when (row.status) {
        VolumeStatus.BELOW -> c.warning
        VolumeStatus.IN_RANGE -> c.success
        VolumeStatus.ABOVE -> c.danger
    }
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                row.muscleGroup.displayName,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${row.sets} ${if (row.sets == 1) "set" else "sets"}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(10.dp))
            StatusPill(row.status.label, tint)
        }
        Spacer(Modifier.height(6.dp))
        BandBar(sets = row.sets, low = row.band.low, high = row.band.high, tint = tint)
        Spacer(Modifier.height(4.dp))
        Text(
            "Target ${row.band.low}–${row.band.high} · 4-week avg ${"%.1f".format(row.fourWeekAverage)}/wk",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Horizontal bar with the target band shaded; the fill marks this week's sets. */
@Composable
private fun BandBar(sets: Int, low: Int, high: Int, tint: Color) {
    val scaleMax = maxOf(high + high / 2, sets, 1)
    val track = MaterialTheme.colorScheme.surfaceContainerHigh
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(CircleShape)
            .background(track),
    ) {
        // Band
        Row(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.weight(low.toFloat().coerceAtLeast(0.01f)))
            Box(
                modifier = Modifier
                    .weight((high - low).toFloat().coerceAtLeast(0.01f))
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
            )
            Spacer(Modifier.weight((scaleMax - high).toFloat().coerceAtLeast(0.01f)))
        }
        // Fill
        if (sets > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (sets.toFloat() / scaleMax).coerceIn(0.02f, 1f))
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(tint)
            )
        }
    }
}

@Composable
private fun StatusPill(label: String, tint: Color) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = tint,
        modifier = Modifier
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
private fun DeloadAdviceCard(advice: DeloadAdvice) {
    val c = FitTrackTheme.colors
    Tile {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconBadge(icon = Icons.Default.TrendingDown, tint = c.warning)
            Column(modifier = Modifier.weight(1f)) {
                Text(advice.title, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(2.dp))
                Text(advice.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(advice.action, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
