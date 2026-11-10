package com.fittrack.app.data.entity

/**
 * Which fields a set of this exercise records. Stored by name in
 * `exercises.trackingMode`; raw values are shared with the iOS app and the
 * CSV export, so never rename them.
 */
enum class TrackingMode(
    val displayName: String,
    val usesWeight: Boolean,
    val usesReps: Boolean,
    val usesDuration: Boolean,
    val usesDistance: Boolean,
) {
    WEIGHT_REPS("Weight & reps", usesWeight = true, usesReps = true, usesDuration = false, usesDistance = false),
    DURATION("Duration", usesWeight = false, usesReps = false, usesDuration = true, usesDistance = false),
    WEIGHT_DURATION("Weight & duration", usesWeight = true, usesReps = false, usesDuration = true, usesDistance = false),
    DISTANCE_DURATION("Distance & duration", usesWeight = false, usesReps = false, usesDuration = true, usesDistance = true),
    WEIGHT_DISTANCE("Weight & distance", usesWeight = true, usesReps = false, usesDuration = false, usesDistance = true);

    /** Carries are measured in metres/yards; cardio in km/miles. */
    val distanceIsShort: Boolean get() = this == WEIGHT_DISTANCE

    companion object {
        fun fromName(name: String?): TrackingMode =
            entries.firstOrNull { it.name == name } ?: WEIGHT_REPS

        /** Built-in catalog names retagged once on upgrade (spec §1). */
        val CATALOG_RETAG: Map<TrackingMode, List<String>> = mapOf(
            DURATION to listOf(
                "Plank", "Side Plank", "Hollow Hold", "Wall Sit", "Dead Hang", "Superman Hold",
                "Wall Handstand Hold", "Side Biceps Hold", "L-Sit", "Handstand Hold",
            ),
            WEIGHT_DISTANCE to listOf(
                "Farmer's Walk", "Rickshaw Carry", "Suitcase Carry", "Sled Push",
                "Sled Drag - Harness", "Prowler Sprint", "Sled Overhead Backward Walk",
            ),
            DISTANCE_DURATION to listOf(
                "Bicycling", "Bicycling, Stationary", "Elliptical Trainer", "Jogging, Treadmill",
                "Recumbent Bike", "Rowing, Stationary", "Running, Treadmill", "Skating", "Stairmaster",
                "Step Mill", "Trail Running/Walking", "Walking, Treadmill", "Running", "Rowing",
                "Cycling", "Swimming",
            ),
        )
    }
}
