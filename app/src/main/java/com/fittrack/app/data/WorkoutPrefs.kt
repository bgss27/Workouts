package com.fittrack.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Workout-time preferences: the default rest length and whether rest alerts
 * fire while the app is in the background. Lives in the same prefs file as
 * the profile so Settings and the active workout read one source.
 */
object WorkoutPrefs {
    private const val PREFS = "fittrack_profile"
    const val KEY_REST_DEFAULT = "rest_default_seconds"
    const val KEY_REST_ALERTS = "rest_alerts_enabled"
    const val DEFAULT_REST_SECONDS = 90

    /** Choices offered everywhere a rest length is picked. */
    val REST_CHOICES = listOf(30, 45, 60, 90, 120, 150, 180, 240, 300)

    private val _defaultRestSeconds = MutableStateFlow(DEFAULT_REST_SECONDS)
    val defaultRestSeconds: StateFlow<Int> = _defaultRestSeconds

    private val _restAlertsEnabled = MutableStateFlow(true)
    val restAlertsEnabled: StateFlow<Boolean> = _restAlertsEnabled

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(context: Context) {
        val p = prefs(context)
        _defaultRestSeconds.value = p.getInt(KEY_REST_DEFAULT, DEFAULT_REST_SECONDS)
        _restAlertsEnabled.value = p.getBoolean(KEY_REST_ALERTS, true)
    }

    fun defaultRest(context: Context): Int {
        load(context)
        return _defaultRestSeconds.value
    }

    fun restAlerts(context: Context): Boolean {
        load(context)
        return _restAlertsEnabled.value
    }

    fun setDefaultRest(context: Context, seconds: Int) {
        prefs(context).edit().putInt(KEY_REST_DEFAULT, seconds).apply()
        _defaultRestSeconds.value = seconds
    }

    fun setRestAlerts(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_REST_ALERTS, enabled).apply()
        _restAlertsEnabled.value = enabled
    }

    /** "45s" under a minute, otherwise "1:30". */
    fun formatRest(seconds: Int): String =
        if (seconds < 60) "${seconds}s" else "%d:%02d".format(seconds / 60, seconds % 60)
}
