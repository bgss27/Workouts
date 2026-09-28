package com.fittrack.app.data.entity

/**
 * Equipment requirement for an exercise. Used to filter the catalog when the
 * user wants a workout they can do with what they have on hand — at home with
 * nothing, with dumbbells, with a pull-up bar, etc.
 *
 * Single equipment per exercise: variants that need different equipment
 * (e.g. Barbell Curl vs Dumbbell Curl) are separate exercises in the catalog.
 */
enum class Equipment(val displayName: String) {
    BODYWEIGHT("Bodyweight"),
    DUMBBELL("Dumbbell"),
    BARBELL("Barbell"),
    MACHINE("Machine / Cable"),
    BAND("Resistance Band"),
    PULL_UP_BAR("Pull-Up Bar"),
}
