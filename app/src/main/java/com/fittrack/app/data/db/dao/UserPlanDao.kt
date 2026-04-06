package com.fittrack.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.fittrack.app.data.entity.UserPlan
import kotlinx.coroutines.flow.Flow

@Dao
interface UserPlanDao {
    @Query("SELECT * FROM user_plan ORDER BY dayOrder ASC")
    fun getAll(): Flow<List<UserPlan>>

    @Query("SELECT * FROM user_plan WHERE programName = :programName ORDER BY dayOrder ASC")
    fun getByProgram(programName: String): Flow<List<UserPlan>>

    @Query("SELECT COUNT(*) FROM user_plan")
    fun getCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM user_plan WHERE routineId = :routineId")
    suspend fun isRoutineInPlan(routineId: Long): Int

    @Query("SELECT COUNT(*) FROM user_plan WHERE programName = :programName")
    suspend fun isProgramInPlan(programName: String): Int

    @Insert
    suspend fun insert(userPlan: UserPlan): Long

    @Insert
    suspend fun insertAll(plans: List<UserPlan>)

    @Query("DELETE FROM user_plan WHERE routineId = :routineId")
    suspend fun removeByRoutineId(routineId: Long)

    @Query("DELETE FROM user_plan WHERE programName = :programName")
    suspend fun removeByProgram(programName: String)

    @Query("DELETE FROM user_plan")
    suspend fun clearAll()
}
