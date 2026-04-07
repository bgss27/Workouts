package com.fittrack.app.billing

/**
 * Features that require a Pro subscription.
 */
enum class ProFeature(val displayName: String, val description: String) {
    ML_INSIGHTS(
        "ML Insights",
        "AI-powered muscle analysis with plateau detection, trend predictions, and personalized recommendations"
    ),
    ALL_PROGRAMS(
        "All Workout Programs",
        "Access to all 3-day and 5-day programs including Bro Split, Upper/Lower/PPL, and more"
    ),
    UNLIMITED_PLAN(
        "Unlimited My Plan",
        "Save any program to your plan and switch between programs freely"
    ),
    ADVANCED_CHARTS(
        "Advanced Charts",
        "Detailed progress charts with volume tracking and estimated 1RM trends"
    ),
    DATA_EXPORT(
        "Data Export",
        "Export your workout history to CSV for external analysis"
    )
}
