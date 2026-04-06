package com.fittrack.app.data.repository

import com.fittrack.app.data.db.dao.RoutineDao
import com.fittrack.app.data.relation.RoutineWithExercises
import kotlinx.coroutines.flow.Flow

class RoutineRepository(private val routineDao: RoutineDao) {
    fun getAllRoutines(): Flow<List<RoutineWithExercises>> = routineDao.getAllRoutines()

    fun getPreBuiltRoutines(): Flow<List<RoutineWithExercises>> = routineDao.getPreBuiltRoutines()

    fun getRoutinesForMuscleGroup(group: String): Flow<List<RoutineWithExercises>> =
        routineDao.getRoutinesForMuscleGroup(group)

    fun getRoutineById(id: Long): Flow<RoutineWithExercises?> = routineDao.getRoutineById(id)

    fun getRoutinesByDaysPerWeek(days: Int): Flow<List<RoutineWithExercises>> =
        routineDao.getRoutinesByDaysPerWeek(days)

    fun getRoutinesByProgram(programName: String): Flow<List<RoutineWithExercises>> =
        routineDao.getRoutinesByProgram(programName)

    fun getAllProgramNames(): Flow<List<String>> = routineDao.getAllProgramNames()
}
