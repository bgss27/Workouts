package com.fittrack.app.data.repository

import com.fittrack.app.data.db.dao.ExerciseSetRow
import com.fittrack.app.data.db.dao.SyncDao
import com.fittrack.app.data.entity.SyncTombstone
import com.fittrack.app.data.db.dao.WorkoutDao
import com.fittrack.app.data.db.dao.WorkoutSetDao
import com.fittrack.app.data.entity.Workout
import com.fittrack.app.data.entity.WorkoutExercise
import com.fittrack.app.data.entity.WorkoutSet
import com.fittrack.app.data.relation.WorkoutWithExercises
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class WorkoutRepository(
    private val workoutDao: WorkoutDao,
    private val workoutSetDao: WorkoutSetDao,
    private val syncDao: SyncDao? = null,
) {
    /** Called after any write that the cloud should learn about. */
    var onChanged: (() -> Unit)? = null
    private fun changed() { onChanged?.invoke() }
    private fun now() = System.currentTimeMillis()
    private fun newId() = java.util.UUID.randomUUID().toString()

    fun getAllWorkouts(): Flow<List<WorkoutWithExercises>> = workoutDao.getAllWorkouts()

    fun getWorkoutsInRange(startTime: Long, endTime: Long): Flow<List<WorkoutWithExercises>> =
        workoutDao.getWorkoutsInRange(startTime, endTime)

    fun getLatestWorkout(): Flow<WorkoutWithExercises?> = workoutDao.getLatestWorkout()

    fun getWorkoutById(id: Long): Flow<WorkoutWithExercises?> = workoutDao.getWorkoutById(id)

    fun getCompletedWorkoutCount(): Flow<Int> = workoutDao.getCompletedWorkoutCount()

    fun getCompletedWorkoutCountSince(sinceMs: Long): Flow<Int> =
        workoutDao.getCompletedWorkoutCountSince(sinceMs)

    fun getTotalVolumeKg(): Flow<Double> = workoutDao.getTotalVolumeKg()

    suspend fun getActiveWorkout(): Workout? = workoutDao.getActiveWorkout()

    /**
     * Start a new workout. Any previously-orphaned in-progress workouts
     * (rows with endTime = null left by a force-quit / system-back exit) are
     * cleaned up first so they don't accumulate.
     */
    suspend fun startWorkout(): Long {
        workoutDao.deleteStaleActiveWorkouts()
        return workoutDao.insert(Workout(remoteId = newId(), updatedAt = now(), dirty = true))
    }

    suspend fun finishWorkout(workoutId: Long, notes: String? = null) {
        // Target by id directly. The previous implementation looked up the
        // "active" workout and silently no-op'd if a stale row had the same
        // endTime IS NULL state — that's the cause of the missing-count bug.
        workoutDao.finishWorkoutById(workoutId, System.currentTimeMillis(), notes)
        changed()
    }

    /**
     * Insert a workout that was completed in the past (manually logged after
     * the fact, e.g. the user trained without opening the app). [startTime]
     * and [endTime] are explicit epoch millis. Returns the new workout id so
     * the caller can attach exercises + sets.
     */
    suspend fun insertPastWorkout(
        startTime: Long,
        endTime: Long,
        notes: String? = null,
    ): Long = workoutDao.insert(
        Workout(startTime = startTime, endTime = endTime, notes = notes, remoteId = newId(), updatedAt = now(), dirty = true)
    ).also { changed() }

    /**
     * Delete a workout. If it was ever synced, record a tombstone so the
     * cloud copy (and other devices) delete it too. Child rows cascade
     * server-side when the engine pushes the parent tombstone.
     */
    suspend fun deleteWorkout(id: Long) {
        val existing = workoutDao.getWorkoutById(id).first()
        val remote = existing?.workout?.remoteId
        if (remote != null && existing.workout.dirty == false && syncDao != null) {
            syncDao.insertTombstone(SyncTombstone(tableName = "workouts", remoteId = remote))
        }
        workoutDao.delete(id)
        changed()
    }

    suspend fun addExerciseToWorkout(workoutId: Long, exerciseId: Long, orderIndex: Int): Long =
        workoutSetDao.insertWorkoutExercise(
            WorkoutExercise(workoutId = workoutId, exerciseId = exerciseId, orderIndex = orderIndex, remoteId = newId(), updatedAt = now(), dirty = true)
        )

    suspend fun addSet(workoutExerciseId: Long, setNumber: Int, reps: Int, weightKg: Double, isWarmup: Boolean = false, rpe: Int? = null): Long =
        workoutSetDao.insertSet(
            WorkoutSet(
                workoutExerciseId = workoutExerciseId,
                setNumber = setNumber,
                reps = reps,
                weightKg = weightKg,
                isWarmup = isWarmup,
                rpe = rpe,
                remoteId = newId(),
                updatedAt = now(),
                dirty = true,
            )
        )

    suspend fun updateSet(set: WorkoutSet) =
        workoutSetDao.updateSet(set.copy(remoteId = set.remoteId ?: newId(), updatedAt = now(), dirty = true)).also { changed() }

    suspend fun deleteSet(setId: Long) {
        workoutSetDao.deleteSet(setId)
        changed()
    }

    suspend fun removeExerciseFromWorkout(workoutExerciseId: Long) =
        workoutSetDao.deleteWorkoutExercise(workoutExerciseId)

    suspend fun updateSupersetGroup(workoutExerciseId: Long, groupId: Long?) =
        workoutSetDao.updateSupersetGroup(workoutExerciseId, groupId, now())

    fun getSetsForExerciseInRange(exerciseId: Long, startTime: Long, endTime: Long): Flow<List<WorkoutSet>> =
        workoutSetDao.getSetsForExerciseInRange(exerciseId, startTime, endTime)

    fun getAllSetsForExercise(exerciseId: Long): Flow<List<WorkoutSet>> =
        workoutSetDao.getAllSetsForExercise(exerciseId)

    /** Every working set of every completed workout, grouped by exercise id. */
    suspend fun getWorkingSetsByExercise(): Map<Long, List<ExerciseSetRow>> =
        workoutSetDao.getAllWorkingSetRows().groupBy { it.exerciseId }

    fun getSetRowsForExercise(exerciseId: Long): Flow<List<ExerciseSetRow>> =
        workoutSetDao.getSetRowsForExercise(exerciseId)

    fun getSetRowsForExerciseInRange(exerciseId: Long, startTime: Long, endTime: Long): Flow<List<ExerciseSetRow>> =
        workoutSetDao.getSetRowsForExerciseInRange(exerciseId, startTime, endTime)

    fun getPersonalBest(exerciseId: Long): Flow<WorkoutSet?> =
        workoutSetDao.getPersonalBest(exerciseId)

    fun getWorkoutExercises(workoutId: Long): Flow<List<WorkoutExercise>> =
        workoutSetDao.getWorkoutExercises(workoutId)

    fun getSetsForWorkoutExercise(workoutExerciseId: Long): Flow<List<WorkoutSet>> =
        workoutSetDao.getSetsForWorkoutExercise(workoutExerciseId)
}
