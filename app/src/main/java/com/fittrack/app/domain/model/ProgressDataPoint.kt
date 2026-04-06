package com.fittrack.app.domain.model

data class ProgressDataPoint(
    val date: Long, // epoch millis
    val estimated1RM: Double,
    val totalVolume: Double, // sets * reps * weight
    val maxWeight: Double,
    val totalSets: Int,
    val totalReps: Int
)
