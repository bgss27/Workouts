package com.fittrack.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val muscleGroup: MuscleGroup,
    val secondaryMuscleGroup: MuscleGroup? = null,
    val isCustom: Boolean = false
)
