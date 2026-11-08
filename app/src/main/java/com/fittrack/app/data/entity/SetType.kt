package com.fittrack.app.data.entity

/**
 * How a logged set should be interpreted. Stored by name in `workout_sets.setType`;
 * [WorkoutSet.isWarmup] is kept in sync for older queries.
 */
enum class SetType(val displayName: String, val badge: String?) {
    WORKING("Working", null),
    WARMUP("Warm-up", "W"),
    DROPSET("Drop set", "D"),
    FAILURE("Failure", "F");

    /** Warm-ups and drop sets never set a personal record. */
    val countsForPR: Boolean get() = this == WORKING || this == FAILURE

    /** Everything but a warm-up adds to volume and the working-set count. */
    val countsForVolume: Boolean get() = this != WARMUP

    companion object {
        fun fromName(name: String?): SetType =
            entries.firstOrNull { it.name == name } ?: WORKING
    }
}
