package com.fittrack.app.data.db

import androidx.room.TypeConverter
import com.fittrack.app.data.entity.MuscleGroup

class Converters {
    @TypeConverter
    fun fromMuscleGroup(value: MuscleGroup?): String? = value?.name

    @TypeConverter
    fun toMuscleGroup(value: String?): MuscleGroup? = value?.let {
        try { MuscleGroup.valueOf(it) } catch (_: Exception) { null }
    }
}
