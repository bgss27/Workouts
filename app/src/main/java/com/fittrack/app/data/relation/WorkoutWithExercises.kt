package com.fittrack.app.data.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.fittrack.app.data.entity.Workout
import com.fittrack.app.data.entity.WorkoutExercise

data class WorkoutWithExercises(
    @Embedded val workout: Workout,
    @Relation(
        parentColumn = "id",
        entityColumn = "workoutId"
    )
    val exercises: List<WorkoutExercise>
)
