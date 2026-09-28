package com.fittrack.app.data

import android.content.Context

/**
 * Weight is always stored canonically in kg; the user's preferred unit
 * (kg / lbs) is a display concern that lives here. Reads from the same
 * `fittrack_profile` SharedPreferences SettingsViewModel writes to.
 */
object WeightUnits {
    private const val PREFS = "fittrack_profile"
    private const val KEY = "weightUnit"
    private const val KG_TO_LBS = 2.2046226218
    private const val KG_PER_LB = 0.45359237

    const val KG = "kg"
    const val LBS = "lbs"

    /** Reads the user's current preference; defaults to kg. */
    fun preferred(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, KG) ?: KG

    /** Convert a stored kg value into the user's preferred display value. */
    fun fromKg(weightKg: Double, unit: String): Double =
        if (unit == LBS) weightKg * KG_TO_LBS else weightKg

    /** Convert a user-entered display value (in [unit]) into canonical kg. */
    fun toKg(displayValue: Double, unit: String): Double =
        if (unit == LBS) displayValue * KG_PER_LB else displayValue

    /**
     * Format a stored kg value as "100 kg" or "220.5 lbs" depending on
     * the user's preferred unit. Whole numbers drop trailing decimal.
     */
    fun format(weightKg: Double, unit: String): String {
        val v = fromKg(weightKg, unit)
        val s = if (v == v.toLong().toDouble()) v.toLong().toString()
        else "%.1f".format(v)
        return "$s $unit"
    }
}
