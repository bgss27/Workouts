package com.fittrack.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workouts")
data class Workout(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val notes: String? = null,
    /** Cloud sync: global id, last local change (epoch ms), pending push. */
    val remoteId: String? = null,
    val updatedAt: Long? = null,
    val dirty: Boolean? = null,
)
