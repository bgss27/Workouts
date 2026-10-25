package com.fittrack.app.data

import android.content.Context
import com.fittrack.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Persisted appearance preference. Defaults to DARK (the app is designed
 * dark-first); Settings exposes System / Dark / Light.
 */
object ThemePrefs {
    private const val PREFS = "fittrack_theme"
    private const val KEY_MODE = "mode"

    private val _mode = MutableStateFlow(ThemeMode.DARK)
    val mode: StateFlow<ThemeMode> = _mode

    fun load(context: Context): ThemeMode {
        val name = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_MODE, null)
        val parsed = name?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.DARK
        _mode.value = parsed
        return parsed
    }

    fun set(context: Context, mode: ThemeMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_MODE, mode.name).apply()
        _mode.value = mode
    }
}
