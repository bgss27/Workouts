package com.fittrack.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    data object Home : Screen("home", "Home", Icons.Default.Home)
    data object ActiveWorkout : Screen("active_workout?routineId={routineId}", "Workout", Icons.Default.FitnessCenter) {
        fun createRoute(routineId: Long? = null): String {
            return if (routineId != null) "active_workout?routineId=$routineId"
            else "active_workout?routineId=-1"
        }
    }
    data object WorkoutHistory : Screen("workout_history", "History")
    data object WorkoutDetail : Screen("workout_detail/{workoutId}", "Workout Detail") {
        fun createRoute(workoutId: Long): String = "workout_detail/$workoutId"
    }
    data object Progress : Screen("progress", "Progress", Icons.Default.ShowChart)
    data object Routines : Screen("routines", "Routines", Icons.Default.List)
    data object RoutineDetail : Screen("routine_detail/{routineId}", "Routine Detail") {
        fun createRoute(routineId: Long): String = "routine_detail/$routineId"
    }
    data object Suggestions : Screen("suggestions", "Tips")

    companion object {
        val bottomNavItems = listOf(Home, ActiveWorkout, Progress, Routines)
    }
}
