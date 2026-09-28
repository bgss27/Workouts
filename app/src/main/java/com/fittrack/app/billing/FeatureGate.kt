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

    /** More than one active program at a time. */
    UNLIMITED_PROGRAM_SLOTS(
        "Multiple Active Programs",
        "Run more than one workout program at the same time"
    ),

    /** Save more than the free tier of custom routines. */
    UNLIMITED_ROUTINES(
        "Unlimited Custom Routines",
        "Build and save as many custom routines as you want"
    ),

    /** Workout history and progress charts beyond the rolling 30-day window. */
    FULL_HISTORY(
        "Full History",
        "View workouts and progress charts beyond the last 30 days"
    ),

    /** Track body weight, measurements, and progress photos. */
    BODY_METRICS(
        "Body Metrics",
        "Log body weight, measurements, and progress photos"
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
    ),

    /** Wear OS / Watch companion. */
    WATCH(
        "Watch Companion",
        "Quick-log sets and rest timer on your watch"
    ),
}
