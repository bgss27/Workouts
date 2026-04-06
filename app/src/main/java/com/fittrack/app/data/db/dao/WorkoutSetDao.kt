package com.fittrack.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.fittrack.app.data.entity.WorkoutExercise
import com.fittrack.app.data.entity.WorkoutSet
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSetDao {
    @Insert
    suspend fun insertWorkoutExercise(workoutExercise: WorkoutExercise): Long

    @Insert
    suspend fun insertSet(set: WorkoutSet): Long

    @Insert
    suspend fun insertSets(sets: List<WorkoutSet>)

    @Update
    suspend fun updateSet(set: WorkoutSet)

    @Query("DELETE FROM workout_sets WHERE id = :setId")
    suspend fun deleteSet(setId: Long)

    @Query("DELETE FROM workout_exercises WHERE id = :workoutExerciseId")
    suspend fun deleteWorkoutExercise(workoutExerciseId: Long)

    @Query("""
        SELECT ws.* FROM workout_sets ws
        INNER JOIN workout_exercises we ON ws.workoutExerciseId = we.id
        INNER JOIN workouts w ON we.workoutId = w.id
        WHERE we.exerciseId = :exerciseId AND w.startTime BETWEEN :startTime AND :endTime
        ORDER BY w.startTime ASC, ws.setNumber ASC
    """)
    fun getSetsForExerciseInRange(exerciseId: Long, startTime: Long, endTime: Long): Flow<List<WorkoutSet>>

    @Query("""
        SELECT ws.* FROM workout_sets ws
        INNER JOIN workout_exercises we ON ws.workoutExerciseId = we.id
        WHERE we.exerciseId = :exerciseId AND ws.isWarmup = 0
        ORDER BY ws.weightKg DESC, ws.reps DESC
        LIMIT 1
    """)
    fun getPersonalBest(exerciseId: Long): Flow<WorkoutSet?>

    @Query("""
        SELECT ws.* FROM workout_sets ws
        INNER JOIN workout_exercises we ON ws.workoutExerciseId = we.id
        WHERE we.workoutId = :workoutId
        ORDER BY we.orderIndex ASC, ws.setNumber ASC
    """)
    fun getSetsForWorkout(workoutId: Long): Flow<List<WorkoutSet>>

    @Query("""
        SELECT we.* FROM workout_exercises we
        WHERE we.workoutId = :workoutId
        ORDER BY we.orderIndex ASC
    """)
    fun getWorkoutExercises(workoutId: Long): Flow<List<WorkoutExercise>>

    @Query("""
        SELECT ws.* FROM workout_sets ws
        WHERE ws.workoutExerciseId = :workoutExerciseId
        ORDER BY ws.setNumber ASC
    """)
    fun getSetsForWorkoutExercise(workoutExerciseId: Long): Flow<List<WorkoutSet>>

    @Query("""
        SELECT ws.*, w.startTime as workoutStartTime FROM workout_sets ws
        INNER JOIN workout_exercises we ON ws.workoutExerciseId = we.id
        INNER JOIN workouts w ON we.workoutId = w.id
        WHERE we.exerciseId = :exerciseId AND ws.isWarmup = 0 AND w.endTime IS NOT NULL
        ORDER BY w.startTime ASC
    """)
    fun getAllSetsForExercise(exerciseId: Long): Flow<List<WorkoutSet>>
}
