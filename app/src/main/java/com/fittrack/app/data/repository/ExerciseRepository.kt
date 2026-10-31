package com.fittrack.app.data.repository

import com.fittrack.app.data.db.dao.ExerciseDao
import com.fittrack.app.data.db.dao.SyncDao
import com.fittrack.app.data.entity.SyncTombstone
import com.fittrack.app.data.entity.Equipment
import com.fittrack.app.data.entity.Exercise
import com.fittrack.app.data.entity.MuscleGroup
import kotlinx.coroutines.flow.Flow

class ExerciseRepository(
    private val exerciseDao: ExerciseDao,
    private val syncDao: SyncDao? = null,
) {
    var onChanged: (() -> Unit)? = null
    private fun changed() { onChanged?.invoke() }

    fun getAllExercises(): Flow<List<Exercise>> = exerciseDao.getAllExercises()

    fun getByMuscleGroup(group: MuscleGroup): Flow<List<Exercise>> = exerciseDao.getByMuscleGroup(group)

    fun searchByName(query: String): Flow<List<Exercise>> = exerciseDao.searchByName(query)

    suspend fun getById(id: Long): Exercise? = exerciseDao.getById(id)

    suspend fun insert(exercise: Exercise): Long = exerciseDao.insert(
        if (exercise.isCustom) exercise.copy(
            remoteId = exercise.remoteId ?: java.util.UUID.randomUUID().toString(),
            updatedAt = System.currentTimeMillis(),
            dirty = true,
        ) else exercise
    ).also { if (exercise.isCustom) changed() }

    suspend fun update(exercise: Exercise) {
        exerciseDao.update(
            if (exercise.isCustom) exercise.copy(
                remoteId = exercise.remoteId ?: java.util.UUID.randomUUID().toString(),
                updatedAt = System.currentTimeMillis(),
                dirty = true,
            ) else exercise
        )
        if (exercise.isCustom) changed()
    }

    fun getCustomCount(): Flow<Int> = exerciseDao.getCustomCount()

    /**
     * Delete a custom exercise. Refuses (returns false) when the exercise is
     * referenced by any workout or routine, since removing it would orphan
     * that history; callers should tell the user to remove those uses first.
     */
    suspend fun deleteCustom(exercise: Exercise): Boolean {
        if (!exercise.isCustom) return false
        if (exerciseDao.usageCount(exercise.id) > 0) return false
        val remote = exercise.remoteId
        if (remote != null && exercise.dirty == false && syncDao != null) {
            syncDao.insertTombstone(SyncTombstone(tableName = "exercises", remoteId = remote))
        }
        exerciseDao.delete(exercise)
        changed()
        return true
    }

    /** Exercises hitting [group] (primary or secondary) using any of [equipment]. */
    suspend fun getForHomeWorkout(group: MuscleGroup, equipment: Set<Equipment>): List<Exercise> =
        if (equipment.isEmpty()) emptyList()
        else exerciseDao.getByMuscleGroupAndEquipment(group, equipment.map { it.name })
}
