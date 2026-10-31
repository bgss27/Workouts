package com.fittrack.app.di

import android.content.Context
import com.fittrack.app.data.db.FitTrackDatabase
import com.fittrack.app.data.export.CsvExporter
import com.fittrack.app.data.health.HealthConnectService
import com.fittrack.app.sync.AccountManager
import com.fittrack.app.sync.SyncEngine
import com.fittrack.app.sync.SyncPrefs
import com.fittrack.app.sync.SyncScheduler
import com.fittrack.app.data.repository.BodyMetricRepository
import com.fittrack.app.data.repository.ExerciseRepository
import com.fittrack.app.data.repository.RoutineRepository
import com.fittrack.app.data.repository.UserPlanRepository
import com.fittrack.app.data.repository.WorkoutRepository
import com.fittrack.app.domain.analysis.ProgressAnalyzer
import com.fittrack.app.domain.analysis.RoutineSuggestionEngine
import com.fittrack.app.domain.ml.MlAnalysisEngine

class AppModule(context: Context) {
    private val database = FitTrackDatabase.getInstance(context)

    val workoutRepository = WorkoutRepository(database.workoutDao(), database.workoutSetDao(), database.syncDao())
    val exerciseRepository = ExerciseRepository(database.exerciseDao(), database.syncDao())
    val routineRepository = RoutineRepository(database.routineDao())
    val userPlanRepository = UserPlanRepository(database.userPlanDao(), database.routineDao())
    val bodyMetricRepository = BodyMetricRepository(database.bodyMetricDao())

    val progressAnalyzer = ProgressAnalyzer(
        workoutRepository,
        exerciseRepository,
        userPlanRepository,
        routineRepository,
    )
    val routineSuggestionEngine = RoutineSuggestionEngine(exerciseRepository, routineRepository)
    val mlAnalysisEngine = MlAnalysisEngine(workoutRepository, exerciseRepository)

    val healthConnectService = HealthConnectService(context)
    val csvExporter = CsvExporter(workoutRepository, exerciseRepository)

    // Cloud sync (phase 1: workouts, sets, custom exercises)
    val accountManager = AccountManager()
    val syncPrefs = SyncPrefs(context)
    val syncEngine = SyncEngine(database.syncDao(), exerciseRepository, syncPrefs, accountManager)

    init {
        val appContext = context.applicationContext
        val trigger: () -> Unit = {
            if (accountManager.isSignedIn) SyncScheduler.requestSync(appContext)
        }
        workoutRepository.onChanged = trigger
        exerciseRepository.onChanged = trigger
    }
}
