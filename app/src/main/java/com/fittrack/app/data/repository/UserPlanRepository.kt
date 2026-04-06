package com.fittrack.app.data.repository

import com.fittrack.app.data.db.dao.RoutineDao
import com.fittrack.app.data.db.dao.UserPlanDao
import com.fittrack.app.data.entity.UserPlan
import com.fittrack.app.data.relation.RoutineWithExercises
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class PlanDay(
    val userPlan: UserPlan,
    val routine: RoutineWithExercises
)

class UserPlanRepository(
    private val userPlanDao: UserPlanDao,
    private val routineDao: RoutineDao
) {
    fun getAllPlans(): Flow<List<UserPlan>> = userPlanDao.getAll()

    fun getPlanCount(): Flow<Int> = userPlanDao.getCount()

    suspend fun isRoutineInPlan(routineId: Long): Boolean =
        userPlanDao.isRoutineInPlan(routineId) > 0

    suspend fun isProgramInPlan(programName: String): Boolean =
        userPlanDao.isProgramInPlan(programName) > 0

    suspend fun addRoutineToPlan(routineId: Long, programName: String?, dayOrder: Int) {
        userPlanDao.insert(
            UserPlan(routineId = routineId, programName = programName, dayOrder = dayOrder)
        )
    }

    suspend fun addProgramToPlan(programName: String, routineIds: List<Long>) {
        // Remove any existing plan first
        userPlanDao.clearAll()
        // Add all routines from the program
        val plans = routineIds.mapIndexed { index, routineId ->
            UserPlan(routineId = routineId, programName = programName, dayOrder = index + 1)
        }
        userPlanDao.insertAll(plans)
    }

    suspend fun addStandaloneRoutineToPlan(routineId: Long) {
        val currentCount = userPlanDao.isRoutineInPlan(routineId)
        if (currentCount == 0) {
            // Get the next order
            userPlanDao.insert(
                UserPlan(routineId = routineId, programName = null, dayOrder = 0)
            )
        }
    }

    suspend fun removeRoutineFromPlan(routineId: Long) {
        userPlanDao.removeByRoutineId(routineId)
    }

    suspend fun removeProgramFromPlan(programName: String) {
        userPlanDao.removeByProgram(programName)
    }

    suspend fun clearPlan() {
        userPlanDao.clearAll()
    }
}
