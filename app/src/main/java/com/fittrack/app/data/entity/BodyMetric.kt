package com.fittrack.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One body-metrics check-in. Every measurement is optional so the user can
 * log just a weight on a busy morning and a full tape-measure session on
 * the weekend. Values are stored canonically (kg / cm / percent); unit
 * conversion for display lives in the UI layer, mirroring how workout set
 * weights are handled via [com.fittrack.app.data.WeightUnits].
 */
@Entity(
    tableName = "body_metrics",
    indices = [Index("recordedAt")]
)
data class BodyMetric(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Epoch millis of the check-in. Defaults to now; editable for backfill. */
    val recordedAt: Long = System.currentTimeMillis(),
    val weightKg: Double? = null,
    val bodyFatPercent: Double? = null,
    val waistCm: Double? = null,
    val chestCm: Double? = null,
    val hipsCm: Double? = null,
    val armCm: Double? = null,
    val thighCm: Double? = null,
    val notes: String? = null,
) {
    /** True when the row carries at least one measurement worth saving. */
    val hasAnyValue: Boolean
        get() = BodyMetricType.entries.any { it.valueOf(this) != null }
}

/** Which measurement a chart or summary is looking at. */
enum class BodyMetricType(
    val displayName: String,
    val kind: Kind,
) {
    WEIGHT("Weight", Kind.MASS),
    BODY_FAT("Body Fat", Kind.PERCENT),
    WAIST("Waist", Kind.LENGTH),
    CHEST("Chest", Kind.LENGTH),
    HIPS("Hips", Kind.LENGTH),
    ARM("Arm", Kind.LENGTH),
    THIGH("Thigh", Kind.LENGTH);

    enum class Kind { MASS, LENGTH, PERCENT }

    /** Pull this metric's canonical value out of a [BodyMetric] row. */
    fun valueOf(metric: BodyMetric): Double? = when (this) {
        WEIGHT -> metric.weightKg
        BODY_FAT -> metric.bodyFatPercent
        WAIST -> metric.waistCm
        CHEST -> metric.chestCm
        HIPS -> metric.hipsCm
        ARM -> metric.armCm
        THIGH -> metric.thighCm
    }

    /** Return a copy of [metric] with this metric's canonical value replaced. */
    fun copyWith(metric: BodyMetric, value: Double?): BodyMetric = when (this) {
        WEIGHT -> metric.copy(weightKg = value)
        BODY_FAT -> metric.copy(bodyFatPercent = value)
        WAIST -> metric.copy(waistCm = value)
        CHEST -> metric.copy(chestCm = value)
        HIPS -> metric.copy(hipsCm = value)
        ARM -> metric.copy(armCm = value)
        THIGH -> metric.copy(thighCm = value)
    }
}
