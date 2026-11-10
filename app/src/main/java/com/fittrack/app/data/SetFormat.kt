package com.fittrack.app.data

import com.fittrack.app.data.entity.TrackingMode
import java.util.Locale

/** Parse and print set durations ("90", "1:30", "1:02:03"). */
object DurationFormat {
    /** Seconds from free text; null when it isn't a duration. */
    fun parse(text: String): Int? {
        val t = text.trim()
        if (t.isEmpty()) return null
        val parts = t.split(':')
        if (parts.size > 3 || parts.any { it.isEmpty() && parts.size > 1 }) return null
        return try {
            when (parts.size) {
                1 -> parts[0].toInt()
                2 -> parts[0].toInt() * 60 + parts[1].toInt()
                else -> parts[0].toInt() * 3600 + parts[1].toInt() * 60 + parts[2].toInt()
            }.takeIf { it >= 0 }
        } catch (_: NumberFormatException) {
            null
        }
    }

    /** "m:ss" under an hour, otherwise "h:mm:ss". */
    fun format(seconds: Int): String {
        val s = seconds.coerceAtLeast(0)
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
    }

    /** Keep only characters a duration can contain while typing. */
    fun sanitize(input: String): String = input.filter { it.isDigit() || it == ':' }
}

/**
 * Distances are stored in metres. The typed/displayed unit follows the
 * tracking mode (short carries vs long cardio) and the weight unit pref
 * (metric vs imperial).
 */
object DistanceUnits {
    const val M = "m"
    const val KM = "km"
    const val YD = "yd"
    const val MI = "mi"

    private const val YARD_M = 0.9144
    private const val MILE_M = 1609.344

    /** Unit label for typing/displaying a distance of this mode. */
    fun label(mode: TrackingMode, weightUnit: String): String {
        val imperial = weightUnit == WeightUnits.LBS
        return if (mode.distanceIsShort) (if (imperial) YD else M) else (if (imperial) MI else KM)
    }

    fun toMeters(value: Double, unit: String): Double = when (unit) {
        KM -> value * 1000.0
        YD -> value * YARD_M
        MI -> value * MILE_M
        else -> value
    }

    fun fromMeters(meters: Double, unit: String): Double = when (unit) {
        KM -> meters / 1000.0
        YD -> meters / YARD_M
        MI -> meters / MILE_M
        else -> meters
    }

    /** "5.2 km", "30 m" — at most two decimals, trailing zeros trimmed. */
    fun format(meters: Double, unit: String): String = "${formatNumber(fromMeters(meters, unit))} $unit"

    fun formatNumber(value: Double): String {
        if (value == value.toLong().toDouble()) return value.toLong().toString()
        return String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
    }
}

/** One-line descriptions of a logged set, shared by hints, history and detail. */
object SetFormat {
    /**
     * "60 kg × 8", "1:30", "40 kg × 1:00", "5.2 km in 28:10", "40 kg × 30 m".
     * With [weightWithUnit] false the weight is a bare number ("60 × 8"),
     * which is how the "Prev …" hints read.
     */
    fun describe(
        mode: TrackingMode,
        weightKg: Double,
        reps: Int,
        durationSeconds: Int?,
        distanceMeters: Double?,
        weightUnit: String,
        weightWithUnit: Boolean = true,
    ): String {
        val weight = if (weightWithUnit) WeightUnits.format(weightKg, weightUnit)
        else WeightUnits.formatNumber(weightKg, weightUnit)
        val duration = durationSeconds?.let { DurationFormat.format(it) } ?: "0:00"
        val distance = distanceMeters?.let { DistanceUnits.format(it, DistanceUnits.label(mode, weightUnit)) } ?: "0 m"
        return when (mode) {
            TrackingMode.WEIGHT_REPS -> "$weight × $reps"
            TrackingMode.DURATION -> duration
            TrackingMode.WEIGHT_DURATION -> "$weight × $duration"
            TrackingMode.DISTANCE_DURATION -> "$distance in $duration"
            TrackingMode.WEIGHT_DISTANCE -> "$weight × $distance"
        }
    }
}
