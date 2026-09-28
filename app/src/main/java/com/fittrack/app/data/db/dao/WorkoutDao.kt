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

    @Query("SELECT COUNT(*) FROM workouts WHERE endTime IS NOT NULL AND startTime >= :sinceMs")
    fun getCompletedWorkoutCountSince(sinceMs: Long): Flow<Int>

    /**
     * Lifetime total volume in kg across all completed working sets. Warmups
     * are excluded so the number tracks actual training load.
     */
    @Query(
        """
        SELECT COALESCE(SUM(s.weightKg * s.reps), 0)
        FROM workout_sets s
        JOIN workout_exercises we ON we.id = s.workoutExerciseId
        JOIN workouts w ON w.id = we.workoutId
        WHERE w.endTime IS NOT NULL AND s.isWarmup = 0
        """
    )
    fun getTotalVolumeKg(): Flow<Double>

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun delete(id: Long)

    /**
     * Finish a workout by id rather than via getActiveWorkout(). Targeting by id
     * is the right primitive — if the user opened an earlier workout and never
     * tapped Finish/Discard, that orphan would otherwise mask this one.
     */
    @Query("UPDATE workouts SET endTime = :endTime, notes = :notes WHERE id = :id")
    suspend fun finishWorkoutById(id: Long, endTime: Long, notes: String?)

    /**
     * Wipe out any in-progress workouts (rows with no endTime). Called at the
     * start of a new workout to clean up orphans left by force-quit /
     * system-back exits.
     */
    @Query("DELETE FROM workouts WHERE endTime IS NULL")
    suspend fun deleteStaleActiveWorkouts(): Int
}
