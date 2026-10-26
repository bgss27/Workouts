package com.fittrack.app.billing

/**
 * Capabilities that may be gated behind the Pro tier.
 *
 * Source of truth for what Pro unlocks. Add a new entry here, then check it
 * with [ProManager.isUnlocked] at the call site.
 */
enum class FeatureGate(val displayName: String, val description: String) {
    /** Surface ML/insights cards beyond the first preview. */
    INSIGHTS(
        "ML Insights",
        "Plateau detection, frequency analysis, and personalized recommendations"
    ),

    /** Create more than the free tier of user-defined exercises. */
    CUSTOM_EXERCISES(
        "Unlimited Custom Exercises",
        "Create as many of your own exercises as you want (free includes ${FreeLimits.CUSTOM_EXERCISES})"
    ),

    /** Workout history and progress charts beyond the rolling 30-day window. */
    FULL_HISTORY(
        "Full History",
        "View workouts and progress charts beyond the last 30 days"
    ),

    /** Track body weight, body fat, and tape measurements over time. */
    BODY_METRICS(
        "Body Metrics",
        "Log body weight, body fat, and measurements, and track trends over time"
    ),

    /** CSV / data portability. */
    EXPORT(
        "Data Export",
        "Export workout history to CSV"
    ),

    /** Apple Health / Health Connect sync. Currently write-only. */
    INTEGRATIONS(
        "Health Sync",
        "Sync finished workouts to your phone's Health store"
    )
}

/**
 * Free-tier caps. Every number here must be enforced somewhere in the UI and
 * mirrored in the Upgrade screen copy; a gate that isn't enforced is a
 * promise the paywall can't keep.
 */
object FreeLimits {
    const val CUSTOM_EXERCISES = 7
}
