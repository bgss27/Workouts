package com.fittrack.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.fittrack.app.billing.ProManager
import com.fittrack.app.di.AppModule
import com.fittrack.app.ui.calendar.CalendarScreen
import com.fittrack.app.ui.workout.LogPastWorkoutScreen
import com.fittrack.app.ui.home.HomeScreen
import com.fittrack.app.ui.exercises.ExercisesScreen
import com.fittrack.app.ui.insights.MuscleInsightScreen
import com.fittrack.app.ui.myplan.MyPlanScreen
import com.fittrack.app.ui.paywall.UpgradeScreen
import com.fittrack.app.ui.settings.SettingsScreen
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
    appModule: AppModule,
    proManager: ProManager
) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                appModule = appModule,
                proManager = proManager,
                onStartWorkout = { navController.navigate(Screen.ActiveWorkout.createRoute()) },
                onViewHistory = { navController.navigate(Screen.WorkoutHistory.route) },
                onViewProgress = { navController.navigate(Screen.Progress.route) },
                onViewSuggestions = { navController.navigate(Screen.Suggestions.route) },
                onViewWorkoutDetail = { navController.navigate(Screen.WorkoutDetail.createRoute(it)) },
                onViewMyPlan = { navController.navigate(Screen.MyPlan.route) },
                onStartRoutine = { routineId ->
                    navController.navigate(Screen.ActiveWorkout.createRoute(routineId))
                },
                onViewMlInsights = { navController.navigate(Screen.MlInsights.route) },
                onUpgrade = { navController.navigate(Screen.Upgrade.route) },
                onStartHomeWorkout = { ids ->
                    navController.navigate(Screen.ActiveWorkout.createRouteWithExercises(ids))
                },
            )
        }

        composable(Screen.Exercises.route) {
            ExercisesScreen(appModule = appModule)
        }

        composable(Screen.Calendar.route) {
            CalendarScreen(
                appModule = appModule,
                onViewWorkoutDetail = { navController.navigate(Screen.WorkoutDetail.createRoute(it)) },
                onStartWorkout = { navController.navigate(Screen.ActiveWorkout.createRoute()) },
                onLogPastWorkout = { dateMillis ->
                    navController.navigate(Screen.LogPastWorkout.createRoute(dateMillis))
                },
            )
        }
        composable(
            route = Screen.LogPastWorkout.route,
            arguments = listOf(navArgument("dateMillis") {
                type = NavType.LongType
                defaultValue = -1L
            }),
        ) { backStackEntry ->
            val raw = backStackEntry.arguments?.getLong("dateMillis") ?: -1L
            LogPastWorkoutScreen(
                appModule = appModule,
                initialDateMillis = if (raw == -1L) null else raw,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
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
            arguments = listOf(
                navArgument("routineId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument("exerciseIds") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = true
                },
            )
        ) { backStackEntry ->
            val routineId = backStackEntry.arguments?.getLong("routineId") ?: -1L
            val rawIds = backStackEntry.arguments?.getString("exerciseIds").orEmpty()
            val initialExerciseIds = rawIds
                .split(',')
                .mapNotNull { it.trim().toLongOrNull() }
            ActiveWorkoutScreen(
                appModule = appModule,
                routineId = if (routineId == -1L) null else routineId,
                initialExerciseIds = initialExerciseIds,
                onWorkoutComplete = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.WorkoutHistory.route) {
            val isPro by proManager.isPro.collectAsState()
            WorkoutHistoryScreen(
                appModule = appModule,
                isPro = isPro,
                onUpgrade = { navController.navigate(Screen.Upgrade.route) },
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
            val isPro by proManager.isPro.collectAsState()
            ProgressScreen(
                appModule = appModule,
                isPro = isPro,
                onUpgrade = { navController.navigate(Screen.Upgrade.route) },
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
            val isPro by proManager.isPro.collectAsState()
            MuscleInsightScreen(
                appModule = appModule,
                isPro = isPro,
                onUpgrade = { navController.navigate(Screen.Upgrade.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                appModule = appModule,
                proManager = proManager,
                onNavigateToUpgrade = { navController.navigate(Screen.Upgrade.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Upgrade.route) {
            UpgradeScreen(
                proManager = proManager,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
