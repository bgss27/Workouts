package com.fittrack.app.data.db

import android.content.Context
import com.fittrack.app.data.db.dao.ExerciseDao
import com.fittrack.app.data.entity.TrackingMode

/**
 * One-shot upgrade step: built-in exercises that are really timed holds,
 * loaded carries or cardio get the matching [TrackingMode] by exact name.
 * Custom rows are never touched. A prefs stamp keeps it from re-running, so
 * a user who deliberately switched Plank back to reps keeps that choice.
 */
object TrackingModeRetag {
    private const val PREFS = "fittrack_catalog"
    private const val KEY = "tracking_retag_v1"

    suspend fun runOnce(context: Context, dao: ExerciseDao) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY, false)) return
        for ((mode, names) in TrackingMode.CATALOG_RETAG) {
            dao.retagTrackingMode(names, mode.name)
        }
        prefs.edit().putBoolean(KEY, true).apply()
    }
}
