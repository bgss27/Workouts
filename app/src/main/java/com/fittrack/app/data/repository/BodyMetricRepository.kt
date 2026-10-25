package com.fittrack.app.data.repository

import com.fittrack.app.data.db.dao.BodyMetricDao
import com.fittrack.app.data.entity.BodyMetric
import com.fittrack.app.data.entity.BodyMetricType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** A single (time, value) sample for one [BodyMetricType], oldest-first when listed. */
data class BodyMetricPoint(val date: Long, val value: Double)

class BodyMetricRepository(private val dao: BodyMetricDao) {

    /** Newest first. */
    fun getAll(): Flow<List<BodyMetric>> = dao.getAll()

    fun getLatest(): Flow<BodyMetric?> = dao.getLatest()

    fun getCount(): Flow<Int> = dao.getCount()

    suspend fun getById(id: Long): BodyMetric? = dao.getById(id)

    /**
     * Oldest-first series for a single metric, skipping check-ins where that
     * metric wasn't recorded. Feeds the trend chart.
     */
    fun getSeries(type: BodyMetricType): Flow<List<BodyMetricPoint>> =
        dao.getAll().map { rows ->
            rows.asReversed().mapNotNull { row ->
                type.valueOf(row)?.let { BodyMetricPoint(row.recordedAt, it) }
            }
        }

    /** Insert when new (id == 0), otherwise update in place. Returns the row id. */
    suspend fun save(metric: BodyMetric): Long {
        return if (metric.id == 0L) {
            dao.insert(metric)
        } else {
            dao.update(metric)
            metric.id
        }
    }

    suspend fun delete(metric: BodyMetric) = dao.delete(metric)
}
