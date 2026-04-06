package com.fittrack.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_plan",
    foreignKeys = [
        ForeignKey(
            entity = Routine::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("routineId")]
)
data class UserPlan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long,
    val programName: String?, // null for standalone routines
    val dayOrder: Int = 0, // ordering within the plan
    val addedAt: Long = System.currentTimeMillis()
)
