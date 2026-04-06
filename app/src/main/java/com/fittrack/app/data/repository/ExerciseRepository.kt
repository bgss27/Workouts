package com.fittrack.app.data.repository

import com.fittrack.app.data.db.dao.ExerciseDao
import com.fittrack.app.data.entity.Exercise
import com.fittrack.app.data.entity.MuscleGroup
import kotlinx.coroutines.flow.Flow

class ExerciseRepository(private val exerciseDao: ExerciseDao) {
    fun getAllExercises(): Flow<List<Exercise>> = exerciseDao.getAllExercises()

    fun getByMuscleGroup(group: MuscleGroup): Flow<List<Exercise>> = exerciseDao.getByMuscleGroup(group)

    fun searchByName(query: String): Flow<List<Exercise>> = exerciseDao.searchByName(query)

    suspend fun getById(id: Long): Exercise? = exerciseDao.getById(id)

    suspend fun insert(exercise: Exercise): Long = exerciseDao.insert(exercise)
}
