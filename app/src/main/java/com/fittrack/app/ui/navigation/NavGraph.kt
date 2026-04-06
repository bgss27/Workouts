package com.fittrack.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.fittrack.app.di.AppModule
import com.fittrack.app.ui.home.HomeScreen
import com.fittrack.app.ui.insights.MuscleInsightScreen
import com.fittrack.app.ui.myplan.MyPlanScreen
import com.fittrack.app.ui.progress.ProgressScreen
import com.fittrack.app.ui.routines.RoutineDetailScreen
import com.fittrack.app.ui.routines.RoutineListScreen
import com.fittrack.app.ui.suggestions.SuggestionsScreen
import com.fittrack.app.ui.workout.ActiveWorkoutScreen
import com.fittrack.app.ui.workout.WorkoutDetailScreen
import com.fittrack.app.ui.workout.WorkoutHistoryScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    appModule: AppModule
) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                appModule = appModule,
                onStartWorkout = { navController.navigate(Screen.ActiveWorkout.createRoute()) },
                onViewHistory = { navController.navigate(Screen.WorkoutHistory.route) },
                onViewProgress = { navController.navigate(Screen.Progress.route) },
                onViewSuggestions = { navController.navigate(Screen.Suggestions.route) },
                onViewWorkoutDetail = { navController.navigate(Screen.WorkoutDetail.createRoute(it)) },
                onViewMyPlan = { navController.navigate(Screen.MyPlan.route) },
                onStartRoutine = { routineId ->
                    navController.navigate(Screen.ActiveWorkout.createRoute(routineId))
                },
                onViewMlInsights = { navController.navigate(Screen.MlInsights.route) }
            )
        }

        composable(Screen.MyPlan.route) {
            MyPlanScreen(
                appModule = appModule,
                onStartRoutine = { routineId ->
                    navController.navigate(Screen.ActiveWorkout.createRoute(routineId))
                },
                onBrowseRoutines = { navController.navigate(Screen.Routines.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ActiveWorkout.route,
            arguments = listOf(navArgument("routineId") {
                type = NavType.LongType
                defaultValue = -1L
            })
        ) { backStackEntry ->
            val routineId = backStackEntry.arguments?.getLong("routineId") ?: -1L
            ActiveWorkoutScreen(
                appModule = appModule,
                routineId = if (routineId == -1L) null else routineId,
                onWorkoutComplete = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.WorkoutHistory.route) {
            WorkoutHistoryScreen(
                appModule = appModule,
                onWorkoutClick = { navController.navigate(Screen.WorkoutDetail.createRoute(it)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.WorkoutDetail.route,
            arguments = listOf(navArgument("workoutId") { type = NavType.LongType })
        ) { backStackEntry ->
            val workoutId = backStackEntry.arguments?.getLong("workoutId") ?: return@composable
            WorkoutDetailScreen(
                appModule = appModule,
                workoutId = workoutId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Progress.route) {
            ProgressScreen(
                appModule = appModule,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Routines.route) {
            RoutineListScreen(
                appModule = appModule,
                onRoutineClick = { navController.navigate(Screen.RoutineDetail.createRoute(it)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.RoutineDetail.route,
            arguments = listOf(navArgument("routineId") { type = NavType.LongType })
        ) { backStackEntry ->
            val routineId = backStackEntry.arguments?.getLong("routineId") ?: return@composable
            RoutineDetailScreen(
                appModule = appModule,
                routineId = routineId,
                onStartRoutine = {
                    navController.navigate(Screen.ActiveWorkout.createRoute(routineId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Suggestions.route) {
            SuggestionsScreen(
                appModule = appModule,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.MlInsights.route) {
            MuscleInsightScreen(
                appModule = appModule,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
