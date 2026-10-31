package com.fittrack.app.sync

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Cursor + status for the sync engine. */
class SyncPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("fittrack_sync", Context.MODE_PRIVATE)

    var cursor: Long
        get() = prefs.getLong("cursor", 0L)
        set(v) = prefs.edit().putLong("cursor", v).apply()

    var lastSyncedAt: Long
        get() = prefs.getLong("last_synced_at", 0L)
        set(v) = prefs.edit().putLong("last_synced_at", v).apply()

    /** Set when the signed-in user changes so the cursor restarts from zero. */
    var userId: String?
        get() = prefs.getString("user_id", null)
        set(v) = prefs.edit().putString("user_id", v).apply()

    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val status: StateFlow<SyncStatus> = _status
    fun setStatus(s: SyncStatus) { _status.value = s }

    fun reset() {
        cursor = 0L
        lastSyncedAt = 0L
        userId = null
    }
}

sealed class SyncStatus {
    object Idle : SyncStatus()
    object Syncing : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}
