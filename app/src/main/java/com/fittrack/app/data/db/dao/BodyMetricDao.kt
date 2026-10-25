package com.fittrack.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.fittrack.app.data.entity.BodyMetric
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyMetricDao {
    @Query("SELECT * FROM body_metrics ORDER BY recordedAt DESC")
    fun getAll(): Flow<List<BodyMetric>>

    @Query("SELECT * FROM body_metrics ORDER BY recordedAt DESC LIMIT 1")
    fun getLatest(): Flow<BodyMetric?>

    @Query("SELECT * FROM body_metrics WHERE id = :id")
    suspend fun getById(id: Long): BodyMetric?

    @Query("SELECT COUNT(*) FROM body_metrics")
    fun getCount(): Flow<Int>

    @Insert
    suspend fun insert(metric: BodyMetric): Long

    @Update
    suspend fun update(metric: BodyMetric)

    @Delete
    suspend fun delete(metric: BodyMetric)
}
