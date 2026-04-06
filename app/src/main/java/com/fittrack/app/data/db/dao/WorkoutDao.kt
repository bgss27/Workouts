package com.fittrack.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.fittrack.app.data.entity.Workout
import com.fittrack.app.data.relation.WorkoutWithExercises
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Insert
    suspend fun insert(workout: Workout): Long

    @Update
    suspend fun update(workout: Workout)

    @Transaction
    @Query("SELECT * FROM workouts ORDER BY startTime DESC")
    fun getAllWorkouts(): Flow<List<WorkoutWithExercises>>

    @Transaction
    @Query("SELECT * FROM workouts WHERE startTime BETWEEN :startTime AND :endTime ORDER BY startTime DESC")
    fun getWorkoutsInRange(startTime: Long, endTime: Long): Flow<List<WorkoutWithExercises>>

    @Transaction
    @Query("SELECT * FROM workouts ORDER BY startTime DESC LIMIT 1")
    fun getLatestWorkout(): Flow<WorkoutWithExercises?>

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :id")
    fun getWorkoutById(id: Long): Flow<WorkoutWithExercises?>

    @Query("SELECT * FROM workouts WHERE endTime IS NULL LIMIT 1")
    suspend fun getActiveWorkout(): Workout?

    @Query("SELECT COUNT(*) FROM workouts WHERE endTime IS NOT NULL")
    fun getCompletedWorkoutCount(): Flow<Int>

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun delete(id: Long)
}
