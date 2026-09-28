package com.fittrack.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    data object Home : Screen("home", "Home", Icons.Default.Home)
    data object MyPlan : Screen("my_plan", "My Plan", Icons.Default.EventNote)
    data object Calendar : Screen("calendar", "Calendar", Icons.Default.CalendarMonth)
    data object ActiveWorkout : Screen(
        "active_workout?routineId={routineId}&exerciseIds={exerciseIds}",
        "Workout",
        Icons.Default.FitnessCenter,
    ) {
        fun createRoute(routineId: Long? = null): String {
            val id = routineId ?: -1L
            return "active_workout?routineId=$id&exerciseIds="
        }

        /**
         * Start a workout pre-populated with a hand-picked exercise list — used
         * by the "train at home" generator, which builds an ad-hoc routine
         * without persisting it. Empty list falls back to a blank workout.
         */
        fun createRouteWithExercises(exerciseIds: List<Long>): String {
            val ids = exerciseIds.joinToString(",")
            return "active_workout?routineId=-1&exerciseIds=$ids"
        }
    }
    data object WorkoutHistory : Screen("workout_history", "History")
    data object WorkoutDetail : Screen("workout_detail/{workoutId}", "Workout Detail") {
        fun createRoute(workoutId: Long): String = "workout_detail/$workoutId"
    }
    data object Progress : Screen("progress", "Progress", Icons.Default.ShowChart)
    data object Exercises : Screen("exercises", "Exercises", Icons.Default.FitnessCenter)
    data object Routines : Screen("routines", "Routines", Icons.Default.List)
    data object RoutineDetail : Screen("routine_detail/{routineId}", "Routine Detail") {
        fun createRoute(routineId: Long): String = "routine_detail/$routineId"
    }
    data object Suggestions : Screen("suggestions", "Tips")
    data object LogPastWorkout : Screen("log_past_workout?dateMillis={dateMillis}", "Log Past Workout") {
        /**
         * Open the manual-log form, optionally pre-seeded to [dateMillis]
         * (start-of-day epoch). -1 means "default to today".
         */
        fun createRoute(dateMillis: Long? = null): String =
            "log_past_workout?dateMillis=${dateMillis ?: -1L}"
    }
    data object MlInsights : Screen("ml_insights", "Insights", Icons.Default.Psychology)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    data object Upgrade : Screen("upgrade", "Upgrade")

    companion object {
        val bottomNavItems = listOf(Home, MyPlan, Calendar, Exercises, Settings)
    }
}
