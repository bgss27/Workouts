package com.fittrack.app.data.repository

import com.fittrack.app.data.db.dao.WorkoutDao
import com.fittrack.app.data.db.dao.WorkoutSetDao
import com.fittrack.app.data.entity.Workout
import com.fittrack.app.data.entity.WorkoutExercise
import com.fittrack.app.data.entity.WorkoutSet
import com.fittrack.app.data.relation.WorkoutWithExercises
import kotlinx.coroutines.flow.Flow

class WorkoutRepository(
    private val workoutDao: WorkoutDao,
    private val workoutSetDao: WorkoutSetDao
) {
    fun getAllWorkouts(): Flow<List<WorkoutWithExercises>> = workoutDao.getAllWorkouts()

    fun getWorkoutsInRange(startTime: Long, endTime: Long): Flow<List<WorkoutWithExercises>> =
        workoutDao.getWorkoutsInRange(startTime, endTime)

    fun getLatestWorkout(): Flow<WorkoutWithExercises?> = workoutDao.getLatestWorkout()

    fun getWorkoutById(id: Long): Flow<WorkoutWithExercises?> = workoutDao.getWorkoutById(id)

    fun getCompletedWorkoutCount(): Flow<Int> = workoutDao.getCompletedWorkoutCount()

    suspend fun getActiveWorkout(): Workout? = workoutDao.getActiveWorkout()

    suspend fun startWorkout(): Long = workoutDao.insert(Workout())

    suspend fun finishWorkout(workoutId: Long, notes: String? = null) {
        workoutDao.getActiveWorkout()?.let { workout ->
            if (workout.id == workoutId) {
                workoutDao.update(workout.copy(endTime = System.currentTimeMillis(), notes = notes))
            }
        }
    }

    suspend fun deleteWorkout(id: Long) = workoutDao.delete(id)

    suspend fun addExerciseToWorkout(workoutId: Long, exerciseId: Long, orderIndex: Int): Long =
        workoutSetDao.insertWorkoutExercise(
            WorkoutExercise(workoutId = workoutId, exerciseId = exerciseId, orderIndex = orderIndex)
        )

    suspend fun addSet(workoutExerciseId: Long, setNumber: Int, reps: Int, weightKg: Double, isWarmup: Boolean = false, rpe: Int? = null): Long =
        workoutSetDao.insertSet(
            WorkoutSet(
                workoutExerciseId = workoutExerciseId,
                setNumber = setNumber,
                reps = reps,
                weightKg = weightKg,
                isWarmup = isWarmup,
                rpe = rpe
            )
        )

    suspend fun updateSet(set: WorkoutSet) = workoutSetDao.updateSet(set)

    suspend fun deleteSet(setId: Long) = workoutSetDao.deleteSet(setId)

    suspend fun removeExerciseFromWorkout(workoutExerciseId: Long) =
        workoutSetDao.deleteWorkoutExercise(workoutExerciseId)

    fun getSetsForExerciseInRange(exerciseId: Long, startTime: Long, endTime: Long): Flow<List<WorkoutSet>> =
        workoutSetDao.getSetsForExerciseInRange(exerciseId, startTime, endTime)

    fun getAllSetsForExercise(exerciseId: Long): Flow<List<WorkoutSet>> =
        workoutSetDao.getAllSetsForExercise(exerciseId)

    fun getPersonalBest(exerciseId: Long): Flow<WorkoutSet?> =
        workoutSetDao.getPersonalBest(exerciseId)

    fun getWorkoutExercises(workoutId: Long): Flow<List<WorkoutExercise>> =
        workoutSetDao.getWorkoutExercises(workoutId)

    fun getSetsForWorkoutExercise(workoutExerciseId: Long): Flow<List<WorkoutSet>> =
        workoutSetDao.getSetsForWorkoutExercise(workoutExerciseId)
}
