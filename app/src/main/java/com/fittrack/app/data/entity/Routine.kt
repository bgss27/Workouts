package com.fittrack.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routines")
data class Routine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val targetMuscleGroups: String, // JSON list of MuscleGroup names
    val isPreBuilt: Boolean = true,
    val difficulty: String = "intermediate" // beginner, intermediate, advanced
)
