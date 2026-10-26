package com.fittrack.app.data.db.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.fittrack.app.data.entity.WorkoutExercise
import com.fittrack.app.data.entity.WorkoutSet
import kotlinx.coroutines.flow.Flow

/**
 * A working set joined to the exercise it belongs to and the start time of
 * its workout. One query over this shape replaces per-exercise fan-outs in
 * the analyzers and gives progress points a real date.
 */
data class ExerciseSetRow(
    @Embedded val set: WorkoutSet,
    val exerciseId: Long,
    val workoutStartTime: Long,
)

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

    @Query("UPDATE workout_exercises SET supersetGroup = :groupId WHERE id = :workoutExerciseId")
    suspend fun updateSupersetGroup(workoutExerciseId: Long, groupId: Long?)

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
        SELECT ws.* FROM workout_sets ws
        INNER JOIN workout_exercises we ON ws.workoutExerciseId = we.id
        INNER JOIN workouts w ON we.workoutId = w.id
        WHERE we.exerciseId = :exerciseId AND ws.isWarmup = 0 AND w.endTime IS NOT NULL
        ORDER BY w.startTime ASC
    """)
    fun getAllSetsForExercise(exerciseId: Long): Flow<List<WorkoutSet>>

    /** Working sets of every completed workout, with exercise id and workout date. */
    @Query("""
        SELECT ws.*, we.exerciseId AS exerciseId, w.startTime AS workoutStartTime
        FROM workout_sets ws
        INNER JOIN workout_exercises we ON ws.workoutExerciseId = we.id
        INNER JOIN workouts w ON we.workoutId = w.id
        WHERE ws.isWarmup = 0 AND w.endTime IS NOT NULL
        ORDER BY w.startTime ASC, ws.setNumber ASC
    """)
    suspend fun getAllWorkingSetRows(): List<ExerciseSetRow>

    /** Same shape for one exercise, all time. */
    @Query("""
        SELECT ws.*, we.exerciseId AS exerciseId, w.startTime AS workoutStartTime
        FROM workout_sets ws
        INNER JOIN workout_exercises we ON ws.workoutExerciseId = we.id
        INNER JOIN workouts w ON we.workoutId = w.id
        WHERE we.exerciseId = :exerciseId AND w.endTime IS NOT NULL
        ORDER BY w.startTime ASC, ws.setNumber ASC
    """)
    fun getSetRowsForExercise(exerciseId: Long): Flow<List<ExerciseSetRow>>

    /** Same shape for one exercise within a workout start-time window. */
    @Query("""
        SELECT ws.*, we.exerciseId AS exerciseId, w.startTime AS workoutStartTime
        FROM workout_sets ws
        INNER JOIN workout_exercises we ON ws.workoutExerciseId = we.id
        INNER JOIN workouts w ON we.workoutId = w.id
        WHERE we.exerciseId = :exerciseId AND w.startTime BETWEEN :startTime AND :endTime
        ORDER BY w.startTime ASC, ws.setNumber ASC
    """)
    fun getSetRowsForExerciseInRange(exerciseId: Long, startTime: Long, endTime: Long): Flow<List<ExerciseSetRow>>
}
