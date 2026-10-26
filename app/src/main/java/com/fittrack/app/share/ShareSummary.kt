package com.fittrack.app.share

/** Everything the share card needs, already formatted for display. */
data class ShareSummary(
    val headline: String,          // "Workout complete" or "New PR!"
    val dateText: String,          // "Mon, Oct 26"
    val durationMinutes: Long,
    val exerciseCount: Int,
    val setCount: Int,
    val volumeText: String,        // "4.2k kg"
    val exerciseLines: List<String>, // "Barbell Bench Press · 4 sets", up to 6
    val prLines: List<String>,     // "Bench Press 102 kg", up to 3
    val streakWeeks: Int,          // 0 = omit
)
