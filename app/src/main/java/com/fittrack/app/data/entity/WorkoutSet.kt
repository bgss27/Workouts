package com.fittrack.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutExercise::class,
            parentColumns = ["id"],
            childColumns = ["workoutExerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("workoutExerciseId")]
)
data class WorkoutSet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutExerciseId: Long,
    val setNumber: Int,
    /** 0 when the exercise's tracking mode has no reps. */
    val reps: Int,
    /** 0 when the exercise's tracking mode has no weight. */
    val weightKg: Double,
    /** Legacy flag; always equals `setType == SetType.WARMUP`. */
    val isWarmup: Boolean = false,
    val rpe: Int? = null,
    @ColumnInfo(defaultValue = "WORKING")
    val setType: SetType = if (isWarmup) SetType.WARMUP else SetType.WORKING,
    /** Timed sets (planks, runs); null when the mode has no duration. */
    val durationSeconds: Int? = null,
    /** Distance in metres (carries, runs); null when the mode has no distance. */
    val distanceMeters: Double? = null,
)
