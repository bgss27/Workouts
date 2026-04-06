package com.fittrack.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fittrack.app.data.db.dao.ExerciseDao
import com.fittrack.app.data.db.dao.RoutineDao
import com.fittrack.app.data.db.dao.WorkoutDao
import com.fittrack.app.data.db.dao.WorkoutSetDao
import com.fittrack.app.data.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Exercise::class,
        Workout::class,
        WorkoutExercise::class,
        WorkoutSet::class,
        Routine::class,
        RoutineExercise::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FitTrackDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun workoutSetDao(): WorkoutSetDao
    abstract fun routineDao(): RoutineDao

    companion object {
        @Volatile
        private var INSTANCE: FitTrackDatabase? = null

        fun getInstance(context: Context): FitTrackDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FitTrackDatabase::class.java,
                    "fittrack_database"
                )
                    .addCallback(SeedCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    seedExercises(database.exerciseDao())
                    seedRoutines(database.routineDao())
                }
            }
        }

        private suspend fun seedExercises(dao: ExerciseDao) {
            val exercises = listOf(
                // Chest
                Exercise(name = "Barbell Bench Press", muscleGroup = MuscleGroup.CHEST, secondaryMuscleGroup = MuscleGroup.TRICEPS),
                Exercise(name = "Incline Dumbbell Press", muscleGroup = MuscleGroup.CHEST, secondaryMuscleGroup = MuscleGroup.SHOULDERS),
                Exercise(name = "Dumbbell Bench Press", muscleGroup = MuscleGroup.CHEST, secondaryMuscleGroup = MuscleGroup.TRICEPS),
                Exercise(name = "Cable Fly", muscleGroup = MuscleGroup.CHEST),
                Exercise(name = "Chest Dip", muscleGroup = MuscleGroup.CHEST, secondaryMuscleGroup = MuscleGroup.TRICEPS),
                Exercise(name = "Push-Up", muscleGroup = MuscleGroup.CHEST, secondaryMuscleGroup = MuscleGroup.TRICEPS),
                Exercise(name = "Incline Barbell Press", muscleGroup = MuscleGroup.CHEST, secondaryMuscleGroup = MuscleGroup.SHOULDERS),
                Exercise(name = "Pec Deck Machine", muscleGroup = MuscleGroup.CHEST),

                // Back
                Exercise(name = "Barbell Row", muscleGroup = MuscleGroup.BACK, secondaryMuscleGroup = MuscleGroup.BICEPS),
                Exercise(name = "Pull-Up", muscleGroup = MuscleGroup.BACK, secondaryMuscleGroup = MuscleGroup.BICEPS),
                Exercise(name = "Lat Pulldown", muscleGroup = MuscleGroup.BACK, secondaryMuscleGroup = MuscleGroup.BICEPS),
                Exercise(name = "Seated Cable Row", muscleGroup = MuscleGroup.BACK),
                Exercise(name = "Dumbbell Row", muscleGroup = MuscleGroup.BACK, secondaryMuscleGroup = MuscleGroup.BICEPS),
                Exercise(name = "T-Bar Row", muscleGroup = MuscleGroup.BACK),
                Exercise(name = "Face Pull", muscleGroup = MuscleGroup.BACK, secondaryMuscleGroup = MuscleGroup.SHOULDERS),
                Exercise(name = "Deadlift", muscleGroup = MuscleGroup.BACK, secondaryMuscleGroup = MuscleGroup.LEGS),

                // Shoulders
                Exercise(name = "Overhead Press", muscleGroup = MuscleGroup.SHOULDERS, secondaryMuscleGroup = MuscleGroup.TRICEPS),
                Exercise(name = "Lateral Raise", muscleGroup = MuscleGroup.SHOULDERS),
                Exercise(name = "Front Raise", muscleGroup = MuscleGroup.SHOULDERS),
                Exercise(name = "Rear Delt Fly", muscleGroup = MuscleGroup.SHOULDERS),
                Exercise(name = "Arnold Press", muscleGroup = MuscleGroup.SHOULDERS),
                Exercise(name = "Dumbbell Shoulder Press", muscleGroup = MuscleGroup.SHOULDERS, secondaryMuscleGroup = MuscleGroup.TRICEPS),
                Exercise(name = "Upright Row", muscleGroup = MuscleGroup.SHOULDERS),

                // Biceps
                Exercise(name = "Barbell Curl", muscleGroup = MuscleGroup.BICEPS),
                Exercise(name = "Dumbbell Curl", muscleGroup = MuscleGroup.BICEPS),
                Exercise(name = "Hammer Curl", muscleGroup = MuscleGroup.BICEPS, secondaryMuscleGroup = MuscleGroup.FOREARMS),
                Exercise(name = "Preacher Curl", muscleGroup = MuscleGroup.BICEPS),
                Exercise(name = "Incline Dumbbell Curl", muscleGroup = MuscleGroup.BICEPS),
                Exercise(name = "Cable Curl", muscleGroup = MuscleGroup.BICEPS),
                Exercise(name = "Concentration Curl", muscleGroup = MuscleGroup.BICEPS),

                // Triceps
                Exercise(name = "Tricep Pushdown", muscleGroup = MuscleGroup.TRICEPS),
                Exercise(name = "Skull Crushers", muscleGroup = MuscleGroup.TRICEPS),
                Exercise(name = "Overhead Tricep Extension", muscleGroup = MuscleGroup.TRICEPS),
                Exercise(name = "Close-Grip Bench Press", muscleGroup = MuscleGroup.TRICEPS, secondaryMuscleGroup = MuscleGroup.CHEST),
                Exercise(name = "Tricep Dip", muscleGroup = MuscleGroup.TRICEPS, secondaryMuscleGroup = MuscleGroup.CHEST),
                Exercise(name = "Cable Overhead Extension", muscleGroup = MuscleGroup.TRICEPS),

                // Legs
                Exercise(name = "Barbell Squat", muscleGroup = MuscleGroup.LEGS, secondaryMuscleGroup = MuscleGroup.GLUTES),
                Exercise(name = "Leg Press", muscleGroup = MuscleGroup.LEGS, secondaryMuscleGroup = MuscleGroup.GLUTES),
                Exercise(name = "Romanian Deadlift", muscleGroup = MuscleGroup.LEGS, secondaryMuscleGroup = MuscleGroup.GLUTES),
                Exercise(name = "Leg Extension", muscleGroup = MuscleGroup.LEGS),
                Exercise(name = "Leg Curl", muscleGroup = MuscleGroup.LEGS),
                Exercise(name = "Bulgarian Split Squat", muscleGroup = MuscleGroup.LEGS, secondaryMuscleGroup = MuscleGroup.GLUTES),
                Exercise(name = "Front Squat", muscleGroup = MuscleGroup.LEGS),
                Exercise(name = "Hack Squat", muscleGroup = MuscleGroup.LEGS),
                Exercise(name = "Walking Lunge", muscleGroup = MuscleGroup.LEGS, secondaryMuscleGroup = MuscleGroup.GLUTES),

                // Glutes
                Exercise(name = "Hip Thrust", muscleGroup = MuscleGroup.GLUTES),
                Exercise(name = "Glute Bridge", muscleGroup = MuscleGroup.GLUTES),
                Exercise(name = "Cable Kickback", muscleGroup = MuscleGroup.GLUTES),
                Exercise(name = "Sumo Deadlift", muscleGroup = MuscleGroup.GLUTES, secondaryMuscleGroup = MuscleGroup.LEGS),

                // Abs
                Exercise(name = "Plank", muscleGroup = MuscleGroup.ABS),
                Exercise(name = "Cable Crunch", muscleGroup = MuscleGroup.ABS),
                Exercise(name = "Hanging Leg Raise", muscleGroup = MuscleGroup.ABS),
                Exercise(name = "Ab Wheel Rollout", muscleGroup = MuscleGroup.ABS),
                Exercise(name = "Russian Twist", muscleGroup = MuscleGroup.ABS),

                // Calves
                Exercise(name = "Standing Calf Raise", muscleGroup = MuscleGroup.CALVES),
                Exercise(name = "Seated Calf Raise", muscleGroup = MuscleGroup.CALVES),

                // Forearms
                Exercise(name = "Wrist Curl", muscleGroup = MuscleGroup.FOREARMS),
                Exercise(name = "Reverse Wrist Curl", muscleGroup = MuscleGroup.FOREARMS),
                Exercise(name = "Farmer's Walk", muscleGroup = MuscleGroup.FOREARMS)
            )
            dao.insertAll(exercises)
        }

        private suspend fun seedRoutines(dao: RoutineDao) {
            // Push/Pull/Legs
            val pushId = dao.insertRoutine(Routine(
                name = "Push Day",
                description = "Chest, shoulders, and triceps focused workout",
                targetMuscleGroups = "CHEST,SHOULDERS,TRICEPS",
                difficulty = "intermediate"
            ))
            dao.insertRoutineExercises(listOf(
                RoutineExercise(routineId = pushId, exerciseId = 1, orderIndex = 0, suggestedSets = 4, suggestedReps = "6-8"),
                RoutineExercise(routineId = pushId, exerciseId = 2, orderIndex = 1, suggestedSets = 3, suggestedReps = "8-12"),
                RoutineExercise(routineId = pushId, exerciseId = 4, orderIndex = 2, suggestedSets = 3, suggestedReps = "12-15"),
                RoutineExercise(routineId = pushId, exerciseId = 17, orderIndex = 3, suggestedSets = 3, suggestedReps = "6-8"),
                RoutineExercise(routineId = pushId, exerciseId = 18, orderIndex = 4, suggestedSets = 3, suggestedReps = "12-15"),
                RoutineExercise(routineId = pushId, exerciseId = 31, orderIndex = 5, suggestedSets = 3, suggestedReps = "10-12")
            ))

            val pullId = dao.insertRoutine(Routine(
                name = "Pull Day",
                description = "Back and biceps focused workout",
                targetMuscleGroups = "BACK,BICEPS",
                difficulty = "intermediate"
            ))
            dao.insertRoutineExercises(listOf(
                RoutineExercise(routineId = pullId, exerciseId = 16, orderIndex = 0, suggestedSets = 3, suggestedReps = "5-6"),
                RoutineExercise(routineId = pullId, exerciseId = 9, orderIndex = 1, suggestedSets = 4, suggestedReps = "6-8"),
                RoutineExercise(routineId = pullId, exerciseId = 10, orderIndex = 2, suggestedSets = 3, suggestedReps = "6-10"),
                RoutineExercise(routineId = pullId, exerciseId = 12, orderIndex = 3, suggestedSets = 3, suggestedReps = "10-12"),
                RoutineExercise(routineId = pullId, exerciseId = 15, orderIndex = 4, suggestedSets = 3, suggestedReps = "15-20"),
                RoutineExercise(routineId = pullId, exerciseId = 24, orderIndex = 5, suggestedSets = 3, suggestedReps = "8-12")
            ))

            val legId = dao.insertRoutine(Routine(
                name = "Leg Day",
                description = "Quads, hamstrings, and glutes focused workout",
                targetMuscleGroups = "LEGS,GLUTES,CALVES",
                difficulty = "intermediate"
            ))
            dao.insertRoutineExercises(listOf(
                RoutineExercise(routineId = legId, exerciseId = 37, orderIndex = 0, suggestedSets = 4, suggestedReps = "5-8"),
                RoutineExercise(routineId = legId, exerciseId = 38, orderIndex = 1, suggestedSets = 3, suggestedReps = "8-12"),
                RoutineExercise(routineId = legId, exerciseId = 39, orderIndex = 2, suggestedSets = 3, suggestedReps = "8-12"),
                RoutineExercise(routineId = legId, exerciseId = 40, orderIndex = 3, suggestedSets = 3, suggestedReps = "12-15"),
                RoutineExercise(routineId = legId, exerciseId = 41, orderIndex = 4, suggestedSets = 3, suggestedReps = "10-12"),
                RoutineExercise(routineId = legId, exerciseId = 46, orderIndex = 5, suggestedSets = 3, suggestedReps = "8-12"),
                RoutineExercise(routineId = legId, exerciseId = 53, orderIndex = 6, suggestedSets = 4, suggestedReps = "12-15")
            ))

            // Upper/Lower
            val upperId = dao.insertRoutine(Routine(
                name = "Upper Body",
                description = "Complete upper body workout targeting chest, back, shoulders, and arms",
                targetMuscleGroups = "CHEST,BACK,SHOULDERS,BICEPS,TRICEPS",
                difficulty = "intermediate"
            ))
            dao.insertRoutineExercises(listOf(
                RoutineExercise(routineId = upperId, exerciseId = 1, orderIndex = 0, suggestedSets = 4, suggestedReps = "6-8"),
                RoutineExercise(routineId = upperId, exerciseId = 9, orderIndex = 1, suggestedSets = 4, suggestedReps = "6-8"),
                RoutineExercise(routineId = upperId, exerciseId = 17, orderIndex = 2, suggestedSets = 3, suggestedReps = "8-10"),
                RoutineExercise(routineId = upperId, exerciseId = 11, orderIndex = 3, suggestedSets = 3, suggestedReps = "8-12"),
                RoutineExercise(routineId = upperId, exerciseId = 24, orderIndex = 4, suggestedSets = 3, suggestedReps = "10-12"),
                RoutineExercise(routineId = upperId, exerciseId = 31, orderIndex = 5, suggestedSets = 3, suggestedReps = "10-12")
            ))

            val lowerId = dao.insertRoutine(Routine(
                name = "Lower Body",
                description = "Complete lower body workout targeting quads, hamstrings, glutes, and calves",
                targetMuscleGroups = "LEGS,GLUTES,CALVES",
                difficulty = "intermediate"
            ))
            dao.insertRoutineExercises(listOf(
                RoutineExercise(routineId = lowerId, exerciseId = 37, orderIndex = 0, suggestedSets = 4, suggestedReps = "5-8"),
                RoutineExercise(routineId = lowerId, exerciseId = 39, orderIndex = 1, suggestedSets = 3, suggestedReps = "8-12"),
                RoutineExercise(routineId = lowerId, exerciseId = 38, orderIndex = 2, suggestedSets = 3, suggestedReps = "8-12"),
                RoutineExercise(routineId = lowerId, exerciseId = 42, orderIndex = 3, suggestedSets = 3, suggestedReps = "8-12"),
                RoutineExercise(routineId = lowerId, exerciseId = 46, orderIndex = 4, suggestedSets = 3, suggestedReps = "8-12"),
                RoutineExercise(routineId = lowerId, exerciseId = 53, orderIndex = 5, suggestedSets = 4, suggestedReps = "12-15"),
                RoutineExercise(routineId = lowerId, exerciseId = 54, orderIndex = 6, suggestedSets = 4, suggestedReps = "15-20")
            ))

            // Full Body
            val fullBodyId = dao.insertRoutine(Routine(
                name = "Full Body",
                description = "Complete full body workout for overall strength and muscle building",
                targetMuscleGroups = "CHEST,BACK,LEGS,SHOULDERS,BICEPS,TRICEPS",
                difficulty = "beginner"
            ))
            dao.insertRoutineExercises(listOf(
                RoutineExercise(routineId = fullBodyId, exerciseId = 37, orderIndex = 0, suggestedSets = 3, suggestedReps = "6-8"),
                RoutineExercise(routineId = fullBodyId, exerciseId = 1, orderIndex = 1, suggestedSets = 3, suggestedReps = "6-8"),
                RoutineExercise(routineId = fullBodyId, exerciseId = 9, orderIndex = 2, suggestedSets = 3, suggestedReps = "8-10"),
                RoutineExercise(routineId = fullBodyId, exerciseId = 17, orderIndex = 3, suggestedSets = 3, suggestedReps = "8-10"),
                RoutineExercise(routineId = fullBodyId, exerciseId = 24, orderIndex = 4, suggestedSets = 2, suggestedReps = "10-12"),
                RoutineExercise(routineId = fullBodyId, exerciseId = 31, orderIndex = 5, suggestedSets = 2, suggestedReps = "10-12")
            ))

            // Chest & Triceps
            dao.insertRoutine(Routine(
                name = "Chest & Triceps",
                description = "Focused chest and triceps session for bro split training",
                targetMuscleGroups = "CHEST,TRICEPS",
                difficulty = "intermediate"
            )).also { id ->
                dao.insertRoutineExercises(listOf(
                    RoutineExercise(routineId = id, exerciseId = 1, orderIndex = 0, suggestedSets = 4, suggestedReps = "6-8"),
                    RoutineExercise(routineId = id, exerciseId = 2, orderIndex = 1, suggestedSets = 3, suggestedReps = "8-12"),
                    RoutineExercise(routineId = id, exerciseId = 3, orderIndex = 2, suggestedSets = 3, suggestedReps = "8-12"),
                    RoutineExercise(routineId = id, exerciseId = 4, orderIndex = 3, suggestedSets = 3, suggestedReps = "12-15"),
                    RoutineExercise(routineId = id, exerciseId = 32, orderIndex = 4, suggestedSets = 3, suggestedReps = "10-12"),
                    RoutineExercise(routineId = id, exerciseId = 31, orderIndex = 5, suggestedSets = 3, suggestedReps = "10-12"),
                    RoutineExercise(routineId = id, exerciseId = 33, orderIndex = 6, suggestedSets = 3, suggestedReps = "12-15")
                ))
            }

            // Back & Biceps
            dao.insertRoutine(Routine(
                name = "Back & Biceps",
                description = "Focused back and biceps session for bro split training",
                targetMuscleGroups = "BACK,BICEPS",
                difficulty = "intermediate"
            )).also { id ->
                dao.insertRoutineExercises(listOf(
                    RoutineExercise(routineId = id, exerciseId = 16, orderIndex = 0, suggestedSets = 3, suggestedReps = "5-6"),
                    RoutineExercise(routineId = id, exerciseId = 10, orderIndex = 1, suggestedSets = 4, suggestedReps = "6-10"),
                    RoutineExercise(routineId = id, exerciseId = 9, orderIndex = 2, suggestedSets = 3, suggestedReps = "8-10"),
                    RoutineExercise(routineId = id, exerciseId = 12, orderIndex = 3, suggestedSets = 3, suggestedReps = "10-12"),
                    RoutineExercise(routineId = id, exerciseId = 15, orderIndex = 4, suggestedSets = 3, suggestedReps = "15-20"),
                    RoutineExercise(routineId = id, exerciseId = 24, orderIndex = 5, suggestedSets = 3, suggestedReps = "8-12"),
                    RoutineExercise(routineId = id, exerciseId = 26, orderIndex = 6, suggestedSets = 3, suggestedReps = "10-12")
                ))
            }
        }
    }
}
