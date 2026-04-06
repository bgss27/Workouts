package com.fittrack.app.data.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.fittrack.app.data.entity.Exercise
import com.fittrack.app.data.entity.WorkoutExercise
import com.fittrack.app.data.entity.WorkoutSet

data class WorkoutExerciseWithSets(
    @Embedded val workoutExercise: WorkoutExercise,
    @Relation(
        parentColumn = "id",
        entityColumn = "workoutExerciseId"
    )
    val sets: List<WorkoutSet>,
    @Relation(
        parentColumn = "exerciseId",
        entityColumn = "id"
    )
    val exercise: Exercise
)
