package com.fittrack.app.di

import android.content.Context
import com.fittrack.app.data.db.FitTrackDatabase
import com.fittrack.app.data.repository.ExerciseRepository
import com.fittrack.app.data.repository.RoutineRepository
import com.fittrack.app.data.repository.UserPlanRepository
import com.fittrack.app.data.repository.WorkoutRepository
import com.fittrack.app.domain.analysis.ProgressAnalyzer
import com.fittrack.app.domain.analysis.RoutineSuggestionEngine

class AppModule(context: Context) {
    private val database = FitTrackDatabase.getInstance(context)

    val workoutRepository = WorkoutRepository(database.workoutDao(), database.workoutSetDao())
    val exerciseRepository = ExerciseRepository(database.exerciseDao())
    val routineRepository = RoutineRepository(database.routineDao())
    val userPlanRepository = UserPlanRepository(database.userPlanDao(), database.routineDao())

    val progressAnalyzer = ProgressAnalyzer(workoutRepository, exerciseRepository)
    val routineSuggestionEngine = RoutineSuggestionEngine(exerciseRepository, routineRepository)
}
