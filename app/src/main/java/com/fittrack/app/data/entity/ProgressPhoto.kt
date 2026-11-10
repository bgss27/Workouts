package com.fittrack.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Which angle a progress photo was taken from. Stored by name. */
enum class PhotoPose(val displayName: String) {
    FRONT("Front"),
    SIDE("Side"),
    BACK("Back"),
    OTHER("Other");

    companion object {
        fun fromName(name: String?): PhotoPose = entries.firstOrNull { it.name == name } ?: FRONT
    }
}

/**
 * One progress photo. The image itself lives as a JPEG in the app's private
 * `files/progress_photos/` directory; [fileName] is its name inside that
 * folder. Photos never leave the device.
 */
@Entity(
    tableName = "progress_photos",
    indices = [Index("takenAt")]
)
data class ProgressPhoto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Epoch millis the photo represents (defaults to the day it was added). */
    val takenAt: Long = System.currentTimeMillis(),
    val fileName: String,
    @ColumnInfo(defaultValue = "FRONT")
    val pose: PhotoPose = PhotoPose.FRONT,
    val notes: String? = null,
)
