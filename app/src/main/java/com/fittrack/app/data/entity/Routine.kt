package com.fittrack.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routines")
data class Routine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val targetMuscleGroups: String, // Comma-separated MuscleGroup names
    val isPreBuilt: Boolean = true,
    val difficulty: String = "intermediate", // beginner, intermediate, advanced
    val daysPerWeek: Int = 0, // 0 = standalone day, 3/4/5/6 = part of a multi-day program
    val programName: String? = null, // Groups routines into a program (e.g. "PPL 3-Day")
    val dayOrder: Int = 0 // Order within the program (Day 1, Day 2, etc.)
)
