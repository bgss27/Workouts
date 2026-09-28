package com.fittrack.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fittrack.app.data.entity.Exercise
import com.fittrack.app.data.entity.MuscleGroup
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getAllExercises(): Flow<List<Exercise>>

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun getCount(): Int

    @Query("SELECT * FROM exercises WHERE muscleGroup = :group ORDER BY name ASC")
    fun getByMuscleGroup(group: MuscleGroup): Flow<List<Exercise>>

    /**
     * Exercises that train [group] (as primary OR secondary muscle) and use
     * one of the [equipmentNames] the user has on hand. Names are passed as
     * uppercase strings to match the [Equipment] enum's name() representation.
     */
    @Query(
        """
        SELECT * FROM exercises
        WHERE (muscleGroup = :group OR secondaryMuscleGroup = :group)
          AND equipment IN (:equipmentNames)
        ORDER BY
          CASE WHEN muscleGroup = :group THEN 0 ELSE 1 END,
          name ASC
        """
    )
    suspend fun getByMuscleGroupAndEquipment(
        group: MuscleGroup,
        equipmentNames: List<String>,
    ): List<Exercise>

    @Query("SELECT * FROM exercises WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchByName(query: String): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getById(id: Long): Exercise?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(exercises: List<Exercise>)

    @Insert
    suspend fun insert(exercise: Exercise): Long
}
