package com.fittrack.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.fittrack.app.data.entity.ProgressPhoto
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressPhotoDao {
    @Query("SELECT * FROM progress_photos ORDER BY takenAt DESC, id DESC")
    fun getAll(): Flow<List<ProgressPhoto>>

    @Query("SELECT * FROM progress_photos WHERE id = :id")
    suspend fun getById(id: Long): ProgressPhoto?

    @Query("SELECT COUNT(*) FROM progress_photos")
    fun getCount(): Flow<Int>

    @Insert
    suspend fun insert(photo: ProgressPhoto): Long

    @Update
    suspend fun update(photo: ProgressPhoto)

    @Delete
    suspend fun delete(photo: ProgressPhoto)
}
