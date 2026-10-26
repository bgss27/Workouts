package com.fittrack.app.data.repository

import com.fittrack.app.data.db.dao.ExerciseDao
import com.fittrack.app.data.entity.Equipment
import com.fittrack.app.data.entity.Exercise
import com.fittrack.app.data.entity.MuscleGroup
import kotlinx.coroutines.flow.Flow

class ExerciseRepository(private val exerciseDao: ExerciseDao) {
    fun getAllExercises(): Flow<List<Exercise>> = exerciseDao.getAllExercises()

    fun getByMuscleGroup(group: MuscleGroup): Flow<List<Exercise>> = exerciseDao.getByMuscleGroup(group)

    fun searchByName(query: String): Flow<List<Exercise>> = exerciseDao.searchByName(query)

    suspend fun getById(id: Long): Exercise? = exerciseDao.getById(id)

    suspend fun insert(exercise: Exercise): Long = exerciseDao.insert(exercise)

    suspend fun update(exercise: Exercise) = exerciseDao.update(exercise)

    fun getCustomCount(): Flow<Int> = exerciseDao.getCustomCount()

    /**
     * Delete a custom exercise. Refuses (returns false) when the exercise is
     * referenced by any workout or routine, since removing it would orphan
     * that history; callers should tell the user to remove those uses first.
     */
    suspend fun deleteCustom(exercise: Exercise): Boolean {
        if (!exercise.isCustom) return false
        if (exerciseDao.usageCount(exercise.id) > 0) return false
        exerciseDao.delete(exercise)
        return true
    }

    /** Exercises hitting [group] (primary or secondary) using any of [equipment]. */
    suspend fun getForHomeWorkout(group: MuscleGroup, equipment: Set<Equipment>): List<Exercise> =
        if (equipment.isEmpty()) emptyList()
        else exerciseDao.getByMuscleGroupAndEquipment(group, equipment.map { it.name })
}
