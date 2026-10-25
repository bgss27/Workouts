package com.fittrack.app.ui.bodymetrics

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.WeightUnits
import com.fittrack.app.data.entity.BodyMetric
import com.fittrack.app.data.entity.BodyMetricType
import com.fittrack.app.data.repository.BodyMetricPoint
import com.fittrack.app.data.repository.BodyMetricRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class BodyMetricsViewModel(
    private val appContext: Context,
    private val repository: BodyMetricRepository,
) : ViewModel() {

    /** Newest first. */
    val metrics: StateFlow<List<BodyMetric>> = repository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Most recent recorded value for each metric type, looking across all
     * check-ins (a weight-only entry on Monday shouldn't blank out the waist
     * measurement taken on Sunday).
     */
    val latestByType: StateFlow<Map<BodyMetricType, BodyMetricPoint>> = repository.getAll()
        .map { rows ->
            val out = mutableMapOf<BodyMetricType, BodyMetricPoint>()
            for (row in rows) { // rows are newest-first, so first hit wins
                for (type in BodyMetricType.entries) {
                    if (type in out) continue
                    type.valueOf(row)?.let { out[type] = BodyMetricPoint(row.recordedAt, it) }
                }
                if (out.size == BodyMetricType.entries.size) break
            }
            out
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val _selectedType = MutableStateFlow(BodyMetricType.WEIGHT)
    val selectedType: StateFlow<BodyMetricType> = _selectedType

    /** Oldest-first series for the chart. */
    val series: StateFlow<List<BodyMetricPoint>> = _selectedType
        .flatMapLatest { repository.getSeries(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectType(type: BodyMetricType) {
        _selectedType.value = type
    }

    fun save(metric: BodyMetric) {
        viewModelScope.launch {
            repository.save(metric)
            syncProfileWeight()
        }
    }

    fun delete(metric: BodyMetric) {
        viewModelScope.launch {
            repository.delete(metric)
            syncProfileWeight()
        }
    }

    /**
     * Keep the Settings → Profile weight field in step with the latest
     * logged weight. That field stores the user-typed string in their
     * display unit under `fittrack_profile/weightKg` (legacy key name), so
     * mirror that convention here rather than fixing it in two places.
     */
    private suspend fun syncProfileWeight() {
        val latestKg = repository.getAll().first()
            .firstNotNullOfOrNull { it.weightKg }
            ?: return
        val unit = WeightUnits.preferred(appContext)
        val display = BodyUnits(unit).formatNumber(WeightUnits.fromKg(latestKg, unit))
        appContext.getSharedPreferences("fittrack_profile", Context.MODE_PRIVATE)
            .edit()
            .putString("weightKg", display)
            .apply()
    }

    class Factory(
        private val context: Context,
        private val repository: BodyMetricRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BodyMetricsViewModel(context.applicationContext, repository) as T
        }
    }
}
