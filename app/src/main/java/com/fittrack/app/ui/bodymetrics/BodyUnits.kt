package com.fittrack.app.ui.bodymetrics

import com.fittrack.app.data.WeightUnits
import com.fittrack.app.data.entity.BodyMetricType

/**
 * Display-unit handling for body metrics. Follows the user's weight-unit
 * preference: kg users measure in centimetres, lbs users in inches. Storage
 * is always kg / cm / percent (see [com.fittrack.app.data.entity.BodyMetric]).
 */
class BodyUnits(val weightUnit: String) {
    private val imperial get() = weightUnit == WeightUnits.LBS

    val lengthUnit: String get() = if (imperial) "in" else "cm"

    fun unitLabel(type: BodyMetricType): String = when (type.kind) {
        BodyMetricType.Kind.MASS -> weightUnit
        BodyMetricType.Kind.LENGTH -> lengthUnit
        BodyMetricType.Kind.PERCENT -> "%"
    }

    /** Canonical (stored) → display value. */
    fun toDisplay(type: BodyMetricType, canonical: Double): Double = when (type.kind) {
        BodyMetricType.Kind.MASS -> WeightUnits.fromKg(canonical, weightUnit)
        BodyMetricType.Kind.LENGTH -> if (imperial) canonical / CM_PER_INCH else canonical
        BodyMetricType.Kind.PERCENT -> canonical
    }

    /** Display value (as typed by the user) → canonical for storage. */
    fun toCanonical(type: BodyMetricType, display: Double): Double = when (type.kind) {
        BodyMetricType.Kind.MASS -> WeightUnits.toKg(display, weightUnit)
        BodyMetricType.Kind.LENGTH -> if (imperial) display * CM_PER_INCH else display
        BodyMetricType.Kind.PERCENT -> display
    }

    /** "72.4 kg", "84 cm", "18.5 %". Whole numbers drop the decimal. */
    fun format(type: BodyMetricType, canonical: Double): String =
        formatNumber(toDisplay(type, canonical)) + " " + unitLabel(type)

    /** Number only, for text fields and chart axes. */
    fun formatNumber(v: Double): String =
        if (v == v.toLong().toDouble()) v.toLong().toString() else "%.1f".format(v)

    private companion object {
        const val CM_PER_INCH = 2.54
    }
}
