package com.fittrack.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_exercises",
    foreignKeys = [
        ForeignKey(
            entity = Workout::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("workoutId"),
        Index("exerciseId")
    ]
)
data class WorkoutExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val exerciseId: Long,
    val orderIndex: Int,
    /**
     * Identifies a superset group within this workout. All workout-exercises
     * sharing the same non-null value are performed back-to-back as a single
     * superset. Null = standalone (default). Group IDs are arbitrary — by
     * convention we use the [id] of the first member of the group.
     */
    val supersetGroup: Long? = null,
)
