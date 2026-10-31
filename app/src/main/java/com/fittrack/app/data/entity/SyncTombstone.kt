package com.fittrack.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A row deleted locally that the server hasn't been told about yet. Pushed
 * as `deleted = true`, then removed. Only rows that had a remoteId are
 * recorded — never-synced rows can just vanish.
 */
@Entity(tableName = "sync_tombstones")
data class SyncTombstone(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tableName: String,
    val remoteId: String,
    val deletedAt: Long = System.currentTimeMillis(),
)
