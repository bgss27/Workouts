package com.fittrack.app.data.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.fittrack.app.data.entity.Routine
import com.fittrack.app.data.entity.RoutineExercise

data class RoutineWithExercises(
    @Embedded val routine: Routine,
    @Relation(
        parentColumn = "id",
        entityColumn = "routineId"
    )
    val exercises: List<RoutineExercise>
)
