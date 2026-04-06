package com.fittrack.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.fittrack.app.data.entity.Routine
import com.fittrack.app.data.entity.RoutineExercise
import com.fittrack.app.data.relation.RoutineWithExercises
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Transaction
    @Query("SELECT * FROM routines ORDER BY name ASC")
    fun getAllRoutines(): Flow<List<RoutineWithExercises>>

    @Transaction
    @Query("SELECT * FROM routines WHERE isPreBuilt = 1 ORDER BY name ASC")
    fun getPreBuiltRoutines(): Flow<List<RoutineWithExercises>>

    @Transaction
    @Query("SELECT * FROM routines WHERE targetMuscleGroups LIKE '%' || :group || '%' ORDER BY name ASC")
    fun getRoutinesForMuscleGroup(group: String): Flow<List<RoutineWithExercises>>

    @Transaction
    @Query("SELECT * FROM routines WHERE id = :id")
    fun getRoutineById(id: Long): Flow<RoutineWithExercises?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRoutine(routine: Routine): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRoutineExercises(exercises: List<RoutineExercise>)
}
