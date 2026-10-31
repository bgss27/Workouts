package com.fittrack.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.fittrack.app.data.entity.Exercise
import com.fittrack.app.data.entity.SyncTombstone
import com.fittrack.app.data.entity.Workout
import com.fittrack.app.data.entity.WorkoutExercise
import com.fittrack.app.data.entity.WorkoutSet

/** Queries the sync engine needs across tables. */
@Dao
interface SyncDao {
    // Tombstones
    @Insert suspend fun insertTombstone(t: SyncTombstone)
    @Query("SELECT * FROM sync_tombstones ORDER BY id ASC") suspend fun tombstones(): List<SyncTombstone>
    @Query("DELETE FROM sync_tombstones WHERE id IN (:ids)") suspend fun clearTombstones(ids: List<Long>)

    // Dirty rows (never-synced rows have dirty = NULL)
    @Query("SELECT * FROM exercises WHERE isCustom = 1 AND (dirty = 1 OR dirty IS NULL)") suspend fun dirtyCustomExercises(): List<Exercise>
    @Query("SELECT * FROM workouts WHERE endTime IS NOT NULL AND (dirty = 1 OR dirty IS NULL)") suspend fun dirtyWorkouts(): List<Workout>
    @Query("""
        SELECT we.* FROM workout_exercises we JOIN workouts w ON w.id = we.workoutId
        WHERE w.endTime IS NOT NULL AND (we.dirty = 1 OR we.dirty IS NULL)
    """) suspend fun dirtyWorkoutExercises(): List<WorkoutExercise>
    @Query("""
        SELECT ws.* FROM workout_sets ws
        JOIN workout_exercises we ON we.id = ws.workoutExerciseId
        JOIN workouts w ON w.id = we.workoutId
        WHERE w.endTime IS NOT NULL AND (ws.dirty = 1 OR ws.dirty IS NULL)
    """) suspend fun dirtySets(): List<WorkoutSet>

    @Query("UPDATE exercises SET dirty = 0 WHERE id IN (:ids)") suspend fun cleanExercises(ids: List<Long>)
    @Query("UPDATE workouts SET dirty = 0 WHERE id IN (:ids)") suspend fun cleanWorkouts(ids: List<Long>)
    @Query("UPDATE workout_exercises SET dirty = 0 WHERE id IN (:ids)") suspend fun cleanWorkoutExercises(ids: List<Long>)
    @Query("UPDATE workout_sets SET dirty = 0 WHERE id IN (:ids)") suspend fun cleanSets(ids: List<Long>)

    // Assign ids to rows that predate sync
    @Query("UPDATE exercises SET remoteId = :remoteId WHERE id = :id") suspend fun setExerciseRemoteId(id: Long, remoteId: String)
    @Query("UPDATE workouts SET remoteId = :remoteId, updatedAt = :now WHERE id = :id") suspend fun setWorkoutRemoteId(id: Long, remoteId: String, now: Long)
    @Query("UPDATE workout_exercises SET remoteId = :remoteId, updatedAt = :now WHERE id = :id") suspend fun setWorkoutExerciseRemoteId(id: Long, remoteId: String, now: Long)
    @Query("UPDATE workout_sets SET remoteId = :remoteId, updatedAt = :now WHERE id = :id") suspend fun setSetRemoteId(id: Long, remoteId: String, now: Long)
    @Query("SELECT * FROM exercises WHERE remoteId IS NULL") suspend fun exercisesWithoutRemoteId(): List<Exercise>
    @Query("SELECT * FROM workouts WHERE remoteId IS NULL") suspend fun workoutsWithoutRemoteId(): List<Workout>
    @Query("SELECT * FROM workout_exercises WHERE remoteId IS NULL") suspend fun workoutExercisesWithoutRemoteId(): List<WorkoutExercise>
    @Query("SELECT * FROM workout_sets WHERE remoteId IS NULL") suspend fun setsWithoutRemoteId(): List<WorkoutSet>

    // Lookups for pull
    @Query("SELECT * FROM exercises WHERE remoteId = :remoteId LIMIT 1") suspend fun exerciseByRemoteId(remoteId: String): Exercise?
    @Query("SELECT * FROM workouts WHERE remoteId = :remoteId LIMIT 1") suspend fun workoutByRemoteId(remoteId: String): Workout?
    @Query("SELECT * FROM workout_exercises WHERE remoteId = :remoteId LIMIT 1") suspend fun workoutExerciseByRemoteId(remoteId: String): WorkoutExercise?
    @Query("SELECT * FROM workout_sets WHERE remoteId = :remoteId LIMIT 1") suspend fun setByRemoteId(remoteId: String): WorkoutSet?

    // Upserts used by pull (insert with explicit local ids preserved via replace-by-remoteId handled in engine)
    @Insert suspend fun insertExercise(e: Exercise): Long
    @Insert suspend fun insertWorkout(w: Workout): Long
    @Insert suspend fun insertWorkoutExercise(we: WorkoutExercise): Long
    @Insert suspend fun insertSet(s: WorkoutSet): Long
    @androidx.room.Update suspend fun updateExercise(e: Exercise)
    @androidx.room.Update suspend fun updateWorkout(w: Workout)
    @androidx.room.Update suspend fun updateWorkoutExercise(we: WorkoutExercise)
    @androidx.room.Update suspend fun updateSet(s: WorkoutSet)
    @Query("DELETE FROM workouts WHERE id = :id") suspend fun deleteWorkoutLocal(id: Long)
    @Query("DELETE FROM workout_exercises WHERE id = :id") suspend fun deleteWorkoutExerciseLocal(id: Long)
    @Query("DELETE FROM workout_sets WHERE id = :id") suspend fun deleteSetLocal(id: Long)
    @Query("DELETE FROM exercises WHERE id = :id AND isCustom = 1") suspend fun deleteCustomExerciseLocal(id: Long)

    @Query("SELECT COUNT(*) FROM workouts WHERE endTime IS NOT NULL AND (dirty = 1 OR dirty IS NULL)") suspend fun pendingWorkoutCount(): Int

    @Query("SELECT * FROM workouts WHERE id = :localId LIMIT 1") suspend fun workoutByRemoteIdOrLocal(localId: Long): Workout?
    @Query("SELECT * FROM workout_exercises WHERE id = :localId LIMIT 1") suspend fun workoutExerciseByLocalId(localId: Long): WorkoutExercise?
}
