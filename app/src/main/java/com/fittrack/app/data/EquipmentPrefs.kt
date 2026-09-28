package com.fittrack.app.data

import android.content.Context
import com.fittrack.app.data.entity.Equipment

/**
 * Persists the user's last-used equipment selection for the "train at home"
 * flow so they don't have to re-tick the same boxes every time. Backed by
 * SharedPreferences; defaults to BODYWEIGHT-only on first run.
 */
object EquipmentPrefs {
    private const val PREFS = "fittrack_profile"
    private const val KEY = "homeEquipment"

    fun load(context: Context): Set<Equipment> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null)
        if (raw.isNullOrBlank()) return setOf(Equipment.BODYWEIGHT)
        return raw.split(',')
            .mapNotNull { runCatching { Equipment.valueOf(it.trim()) }.getOrNull() }
            .toSet()
            .ifEmpty { setOf(Equipment.BODYWEIGHT) }
    }

    fun save(context: Context, equipment: Set<Equipment>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, equipment.joinToString(",") { it.name })
            .apply()
    }
}
