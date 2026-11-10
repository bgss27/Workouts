package com.fittrack.app.data.db

import androidx.room.TypeConverter
import com.fittrack.app.data.entity.Equipment
import com.fittrack.app.data.entity.MuscleGroup
import com.fittrack.app.data.entity.PhotoPose
import com.fittrack.app.data.entity.SetType
import com.fittrack.app.data.entity.TrackingMode

class Converters {
    @TypeConverter
    fun fromMuscleGroup(value: MuscleGroup?): String? = value?.name

    @TypeConverter
    fun toMuscleGroup(value: String?): MuscleGroup? = value?.let {
        try { MuscleGroup.valueOf(it) } catch (_: Exception) { null }
    }

    @TypeConverter
    fun fromEquipment(value: Equipment?): String? = value?.name

    @TypeConverter
    fun toEquipment(value: String?): Equipment = value?.let {
        try { Equipment.valueOf(it) } catch (_: Exception) { Equipment.BARBELL }
    } ?: Equipment.BARBELL

    @TypeConverter
    fun fromSetType(value: SetType?): String = (value ?: SetType.WORKING).name

    @TypeConverter
    fun toSetType(value: String?): SetType = SetType.fromName(value)

    @TypeConverter
    fun fromTrackingMode(value: TrackingMode?): String = (value ?: TrackingMode.WEIGHT_REPS).name

    @TypeConverter
    fun toTrackingMode(value: String?): TrackingMode = TrackingMode.fromName(value)

    @TypeConverter
    fun fromPhotoPose(value: PhotoPose?): String = (value ?: PhotoPose.FRONT).name

    @TypeConverter
    fun toPhotoPose(value: String?): PhotoPose = PhotoPose.fromName(value)
}
