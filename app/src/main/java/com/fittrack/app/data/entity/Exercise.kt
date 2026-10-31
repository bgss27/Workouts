package com.fittrack.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val muscleGroup: MuscleGroup,
    val secondaryMuscleGroup: MuscleGroup? = null,
    val isCustom: Boolean = false,
    /**
     * Equipment required. Defaults to BARBELL — the most common gym staple —
     * so old code paths and user-created exercises don't accidentally land in
     * the bodyweight bucket and dilute the "train at home" generator.
     */
    val equipment: Equipment = Equipment.BARBELL,
    /**
     * Cloud sync. Catalog rows use a deterministic "catalog:<normalized name>"
     * so every device agrees on them without syncing; custom rows get a UUID.
     */
    val remoteId: String? = null,
    val updatedAt: Long? = null,
    val dirty: Boolean? = null,
)
