package com.fittrack.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fittrack.app.data.db.dao.ExerciseDao
import com.fittrack.app.data.db.dao.RoutineDao
import com.fittrack.app.data.db.dao.UserPlanDao
import com.fittrack.app.data.db.dao.WorkoutDao
import com.fittrack.app.data.db.dao.WorkoutSetDao
import com.fittrack.app.data.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Single source of truth for the built-in exercise catalog. Used by both the
 * onCreate seed (fresh installs) and the v4→v5 migration (upgrading users).
 * Keep this list in sync with the equipment tagging in MIGRATION_4_5.
 */
internal val SEED_EXERCISES: List<Exercise> = listOf(
    // Chest — barbells, dumbbells, machines
    Exercise(name = "Barbell Bench Press", muscleGroup = MuscleGroup.CHEST, secondaryMuscleGroup = MuscleGroup.TRICEPS, equipment = Equipment.BARBELL),
    Exercise(name = "Incline Dumbbell Press", muscleGroup = MuscleGroup.CHEST, secondaryMuscleGroup = MuscleGroup.SHOULDERS, equipment = Equipment.DUMBBELL),
    Exercise(name = "Dumbbell Bench Press", muscleGroup = MuscleGroup.CHEST, secondaryMuscleGroup = MuscleGroup.TRICEPS, equipment = Equipment.DUMBBELL),
    Exercise(name = "Cable Fly", muscleGroup = MuscleGroup.CHEST, equipment = Equipment.MACHINE),
    Exercise(name = "Chest Dip", muscleGroup = MuscleGroup.CHEST, secondaryMuscleGroup = MuscleGroup.TRICEPS, equipment = Equipment.BODYWEIGHT),
    Exercise(name = "Push-Up", muscleGroup = MuscleGroup.CHEST, secondaryMuscleGroup = MuscleGroup.TRICEPS, equipment = Equipment.BODYWEIGHT),
    Exercise(name = "Incline Barbell Press", muscleGroup = MuscleGroup.CHEST, secondaryMuscleGroup = MuscleGroup.SHOULDERS, equipment = Equipment.BARBELL),
    Exercise(name = "Pec Deck Machine", muscleGroup = MuscleGroup.CHEST, equipment = Equipment.MACHINE),
    // Chest — bodyweight additions for at-home flow
    Exercise(name = "Wide Push-Up", muscleGroup = MuscleGroup.CHEST, equipment = Equipment.BODYWEIGHT),
    Exercise(name = "Decline Push-Up", muscleGroup = MuscleGroup.CHEST, secondaryMuscleGroup = MuscleGroup.TRICEPS, equipment = Equipment.BODYWEIGHT),
    Exercise(name = "Incline Push-Up", muscleGroup = MuscleGroup.CHEST, equipment = Equipment.BODYWEIGHT),
    Exercise(name = "Diamond Push-Up", muscleGroup = MuscleGroup.TRICEPS, secondaryMuscleGroup = MuscleGroup.CHEST, equipment = Equipment.BODYWEIGHT),
    Exercise(name = "Pseudo-Planche Push-Up", muscleGroup = MuscleGroup.CHEST, secondaryMuscleGroup = MuscleGroup.SHOULDERS, equipment = Equipment.BODYWEIGHT),

    // Back
    Exercise(name = "Barbell Row", muscleGroup = MuscleGroup.BACK, secondaryMuscleGroup = MuscleGroup.BICEPS, equipment = Equipment.BARBELL),
    Exercise(name = "Pull-Up", muscleGroup = MuscleGroup.BACK, secondaryMuscleGroup = MuscleGroup.BICEPS, equipment = Equipment.PULL_UP_BAR),
    Exercise(name = "Lat Pulldown", muscleGroup = MuscleGroup.BACK, secondaryMuscleGroup = MuscleGroup.BICEPS, equipment = Equipment.MACHINE),
    Exercise(name = "Seated Cable Row", muscleGroup = MuscleGroup.BACK, equipment = Equipment.MACHINE),
    Exercise(name = "Dumbbell Row", muscleGroup = MuscleGroup.BACK, secondaryMuscleGroup = MuscleGroup.BICEPS, equipment = Equipment.DUMBBELL),
    Exercise(name = "T-Bar Row", muscleGroup = MuscleGroup.BACK, equipment = Equipment.BARBELL),
    Exercise(name = "Face Pull", muscleGroup = MuscleGroup.BACK, secondaryMuscleGroup = MuscleGroup.SHOULDERS, equipment = Equipment.MACHINE),
    Exercise(name = "Deadlift", muscleGroup = MuscleGroup.BACK, secondaryMuscleGroup = MuscleGroup.LEGS, equipment = Equipment.BARBELL),
    // Back — bodyweight additions (limited; back is the hardest to train at home).
    // Inverted Row tagged PULL_UP_BAR because it needs SOMETHING to grip — a
    // low bar or a sturdy table you can fit under. Pure bodyweight "no
    // equipment" users can't actually do it without furniture.
    Exercise(name = "Inverted Row", muscleGroup = MuscleGroup.BACK, secondaryMuscleGroup = MuscleGroup.BICEPS, equipment = Equipment.PULL_UP_BAR),
    Exercise(name = "Superman Hold", muscleGroup = MuscleGroup.BACK, equipment = Equipment.BODYWEIGHT),
    Exercise(name = "Reverse Snow Angel", muscleGroup = MuscleGroup.BACK, equipment = Equipment.BODYWEIGHT),
    Exercise(name = "Band Pull-Apart", muscleGroup = MuscleGroup.BACK, secondaryMuscleGroup = MuscleGroup.SHOULDERS, equipment = Equipment.BAND),
    // v7 additions sourced from wger Workout Manager (wger.de, CC-BY-SA 3.0).
    // These are the genuinely-bodyweight back/posterior-chain exercises wger
    // has that free-exercise-db doesn't.
    Exercise(name = "Arm Raises T/Y/I", muscleGroup = MuscleGroup.BACK, secondaryMuscleGroup = MuscleGroup.SHOULDERS, equipment = Equipment.BODYWEIGHT),
    Exercise(name = "Quadruped Arm and Leg Raise", muscleGroup = MuscleGroup.BACK, secondaryMuscleGroup = MuscleGroup.GLUTES, equipment = Equipment.BODYWEIGHT),
    Exercise(name = "Trap-3 Raise", muscleGroup = MuscleGroup.BACK, secondaryMuscleGroup = MuscleGroup.SHOULDERS, equipment = Equipment.BODYWEIGHT),
    Exercise(name = "Wall Slides", muscleGroup = MuscleGroup.BACK, secondaryMuscleGroup = MuscleGroup.SHOULDERS, equipment = Equipment.BODYWEIGHT),

    // Shoulders
    Exercise(name = "Overhead Press", muscleGroup = MuscleGroup.SHOULDERS, secondaryMuscleGroup = MuscleGroup.TRICEPS, equipment = Equipment.BARBELL),
    Exercise(name = "Lateral Raise", muscleGroup = MuscleGroup.SHOULDERS, equipment = Equipment.DUMBBELL),
    Exercise(name = "Front Raise", muscleGroup = MuscleGroup.SHOULDERS, equipment = Equipment.DUMBBELL),
    Exercise(name = "Rear Delt Fly", muscleGroup = MuscleGroup.SHOULDERS, equipment = Equipment.DUMBBELL),
    Exercise(name = "Arnold Press", muscleGroup = MuscleGroup.SHOULDERS, equipment = Equipment.DUMBBELL),
    Exercise(name = "Dumbbell Shoulder Press", muscleGroup = MuscleGroup.SHOULDERS, secondaryMuscleGroup = MuscleGroup.TRICEPS, equipment = Equipment.DUMBBELL),
    Exercise(name = "Upright Row", muscleGroup = MuscleGroup.SHOULDERS, equipment = Equipment.BARBELL),
    // Shoulders — bodyweight additions
    Exercise(name = "Pike Push-Up", muscleGroup = MuscleGroup.SHOULDERS, secondaryMuscleGroup = MuscleGroup.TRICEPS, equipment = Equipment.BODYWEIGHT),
    Exercise(name = "Decline Pike Push-Up", muscleGroup = MuscleGroup.SHOULDERS, secondaryMuscleGroup = MuscleGroup.TRICEPS, equipment = Equipment.BODYWEIGHT),
    Exercise(name = "Wall Handstand Hold", muscleGroup = MuscleGroup.SHOULDERS, equipment = Equipment.BODYWEIGHT),

    // Biceps
    Exercise(name = "Barbell Curl", muscleGroup = MuscleGroup.BICEPS, equipment = Equipment.BARBELL),
    Exercise(name = "Dumbbell Curl", muscleGroup = MuscleGroup.BICEPS, equipment = Equipment.DUMBBELL),
    Exercise(name = "Hammer Curl", muscleGroup = MuscleGroup.BICEPS, secondaryMuscleGroup = MuscleGroup.FOREARMS, equipment = Equipment.DUMBBELL),
    Exercise(name = "Preacher Curl", muscleGroup = MuscleGroup.BICEPS, equipment = Equipment.DUMBBELL),
    Exercise(name = "Incline Dumbbell Curl", muscleGroup = MuscleGroup.BICEPS, equipment = Equipment.DUMBBELL),
    Exercise(name = "Cable Curl", muscleGroup = MuscleGroup.BICEPS, equipment = Equipment.MACHINE),
    Exercise(name = "Concentration Curl", muscleGroup = MuscleGroup.BICEPS, equipment = Equipment.DUMBBELL),
    Exercise(name = "Band Curl", muscleGroup = MuscleGroup.BICEPS, equipment = Equipment.BAND),
    Exercise(name = "Chin-Up", muscleGroup = MuscleGroup.BICEPS, secondaryMuscleGroup = MuscleGroup.BACK, equipment = Equipment.PULL_UP_BAR),
    // Biceps — true no-equipment options. Isometric and self-resistance work
    // is the honest answer to "biceps without weights"; they build strength
    // even if hypertrophy stimulus is limited.
    Exercise(name = "Isometric Biceps Flex", muscleGroup = MuscleGroup.BICEPS, equipment = Equipment.BODYWEIGHT),
    Exercise(name = "Self-Resistance Curl", muscleGroup = MuscleGroup.BICEPS, equipment = Equipment.BODYWEIGHT),
    Exercise(name = "Towel Biceps Curl", muscleGroup = MuscleGroup.BICEPS, secondaryMuscleGroup = MuscleGroup.FOREARMS, equipment = Equipment.BODYWEIGHT),
    Exercise(name = "Side Biceps Hold", muscleGroup = MuscleGroup.BICEPS, secondaryMuscleGroup = MuscleGroup.SHOULDERS, equipment = Equipment.BODYWEIGHT),

    // Triceps
    Exercise(name = "Tricep Pushdown", muscleGroup = MuscleGroup.TRICEPS, equipment = Equipment.MACHINE),
    Exercise(name = "Skull Crushers", muscleGroup = MuscleGroup.TRICEPS, equipment = Equipment.BARBELL),
    Exercise(name = "Overhead Tricep Extension", muscleGroup = MuscleGroup.TRICEPS, equipment = Equipment.DUMBBELL),
    Exercise(name = "Close-Grip Bench Press", muscleGroup = MuscleGroup.TRICEPS, secondaryMuscleGroup = MuscleGroup.CHEST, equipment = Equipment.BARBELL),
    Exercise(name = "Tricep Dip", muscleGroup = MuscleGroup.TRICEPS, secondaryMuscleGroup = MuscleGroup.CHEST, equipment = Equipment.BODYWEIGHT),
    Exercise(name = "Cable Overhead Extension", muscleGroup = MuscleGroup.TRICEPS, equipment = Equipment.MACHINE),
    Exercise(name = "Chair Dip", muscleGroup = MuscleGroup.TRICEPS, secondaryMuscleGroup = MuscleGroup.CHEST, equipment = Equipment.BODYWEIGHT),

    // Legs
    Exercise(name = "Barbell Squat", muscleGroup = MuscleGroup.LEGS, secondaryMuscleGroup = MuscleGroup.GLUTES, equipment = Equipment.BARBELL),
    Exercise(name = "Leg Press", muscleGroup = MuscleGroup.LEGS, secondaryMuscleGroup = MuscleGroup.GLUTES, equipment = Equipment.MACHINE),
    Exercise(name = "Romanian Deadlift", muscleGroup = MuscleGroup.LEGS, secondaryMuscleGroup = MuscleGroup.GLUTES, equipment = Equipment.BARBELL),
    Exercise(name = "Leg Extension", muscleGroup = MuscleGroup.LEGS, equipment = Equipment.MACHINE),
    Exercise(name = "Leg Curl", muscleGroup = MuscleGroup.LEGS, equipment = Equipment.MACHINE),
    Exercise(name = "Bulgarian Split Squat", muscleGroup = MuscleGroup.LEGS, secondaryMuscleGroup = MuscleGroup.GLUTES, equipment = Equipment.BODYWEIGHT),
    Exercise(name = "Front Squat", muscleGroup = MuscleGroup.LEGS, equipment = Equipment.BARBELL),
    Exercise(name = "Hack Squat", muscleGroup = MuscleGroup.LEGS, equipment = Equipment.MACHINE),
    Exercise(name = "Walking Lunge", muscleGroup = MuscleGroup.LEGS, secondaryMuscleGroup = MuscleGroup.GLUTES, equipment = Equipment.BODYWEIGHT),
    // Legs — bodyweight additions
    Exercise(name = "Bodyweight Squat", muscleGroup = MuscleGroup.LEGS, secondaryMuscleGroup = MuscleGroup.GLUTES, equipment = Equipment.BODYWEIGHT),
    Exercise(name = "Jump Squat", muscleGroup = MuscleGroup.LEGS, secondaryMuscleGroup = MuscleGroup.GLUTES, equipment = Equipment.BODYWEIGHT),
    Exercise(name = "Pistol Squat", muscleGroup = MuscleGroup.LEGS, secondaryMuscleGroup = MuscleGroup.GLUTES, equipment = Equipment.BODYWEIGHT),
    Exercise(name = "Wall Sit", muscleGroup = MuscleGroup.LEGS, equipment = Equipment.BODYWEIGHT),
    Exercise(name = "Reverse Lunge", muscleGroup = MuscleGroup.LEGS, secondaryMuscleGroup = MuscleGroup.GLUTES, equipment = Equipment.BODYWEIGHT),

    // Glutes
    Exercise(name = "Hip Thrust", muscleGroup = MuscleGroup.GLUTES, equipment = Equipment.BARBELL),
    Exercise(name = "Glute Bridge", muscleGroup = MuscleGroup.GLUTES, equipment = Equipment.BODYWEIGHT),
    Exercise(name = "Cable Kickback", muscleGroup = MuscleGroup.GLUTES, equipment = Equipment.MACHINE),
    Exercise(name = "Sumo Deadlift", muscleGroup = MuscleGroup.GLUTES, secondaryMuscleGroup = MuscleGroup.LEGS, equipment = Equipment.BARBELL),
    Exercise(name = "Single-Leg Glute Bridge", muscleGroup = MuscleGroup.GLUTES, equipment = Equipment.BODYWEIGHT),
    Exercise(name = "Fire Hydrant", muscleGroup = MuscleGroup.GLUTES, equipment = Equipment.BODYWEIGHT),

    // Abs
    Exercise(name = "Plank", muscleGroup = MuscleGroup.ABS, equipment = Equipment.BODYWEIGHT),
    Exercise(name = "Cable Crunch", muscleGroup = MuscleGroup.ABS, equipment = Equipment.MACHINE),
    Exercise(name = "Hanging Leg Raise", muscleGroup = MuscleGroup.ABS, equipment = Equipment.PULL_UP_BAR),
    Exercise(name = "Ab Wheel Rollout", muscleGroup = MuscleGroup.ABS, equipment = Equipment.BODYWEIGHT),
    Exercise(name = "Russian Twist", muscleGroup = MuscleGroup.ABS, equipment = Equipment.BODYWEIGHT),
    Exercise(name = "Bicycle Crunch", muscleGroup = MuscleGroup.ABS, equipment = Equipment.BODYWEIGHT),
    Exercise(name = "Mountain Climbers", muscleGroup = MuscleGroup.ABS, equipment = Equipment.BODYWEIGHT),
    Exercise(name = "Dead Bug", muscleGroup = MuscleGroup.ABS, equipment = Equipment.BODYWEIGHT),
    Exercise(name = "Hollow Hold", muscleGroup = MuscleGroup.ABS, equipment = Equipment.BODYWEIGHT),
    Exercise(name = "Lying Leg Raise", muscleGroup = MuscleGroup.ABS, equipment = Equipment.BODYWEIGHT),

    // Calves
    Exercise(name = "Standing Calf Raise", muscleGroup = MuscleGroup.CALVES, equipment = Equipment.MACHINE),
    Exercise(name = "Seated Calf Raise", muscleGroup = MuscleGroup.CALVES, equipment = Equipment.MACHINE),
    Exercise(name = "Bodyweight Calf Raise", muscleGroup = MuscleGroup.CALVES, equipment = Equipment.BODYWEIGHT),
    Exercise(name = "Single-Leg Calf Raise", muscleGroup = MuscleGroup.CALVES, equipment = Equipment.BODYWEIGHT),

    // Forearms
    Exercise(name = "Wrist Curl", muscleGroup = MuscleGroup.FOREARMS, equipment = Equipment.DUMBBELL),
    Exercise(name = "Reverse Wrist Curl", muscleGroup = MuscleGroup.FOREARMS, equipment = Equipment.DUMBBELL),
    Exercise(name = "Farmer's Walk", muscleGroup = MuscleGroup.FOREARMS, equipment = Equipment.DUMBBELL),
    Exercise(name = "Dead Hang", muscleGroup = MuscleGroup.FOREARMS, equipment = Equipment.PULL_UP_BAR),
)

@Database(
    entities = [
        Exercise::class,
        Workout::class,
        WorkoutExercise::class,
        WorkoutSet::class,
        Routine::class,
        RoutineExercise::class,
        UserPlan::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FitTrackDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun workoutSetDao(): WorkoutSetDao
    abstract fun routineDao(): RoutineDao
    abstract fun userPlanDao(): UserPlanDao

    companion object {
        @Volatile
        private var INSTANCE: FitTrackDatabase? = null

        /**
         * v3 → v4: add nullable `supersetGroup` column to workout_exercises.
         * Existing rows default to NULL (standalone), preserving user data.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_exercises ADD COLUMN supersetGroup INTEGER")
            }
        }

        /**
         * v4 → v5: add `equipment` column to `exercises`. Existing rows default
         * to BARBELL (the most common gym staple), then we UPDATE each known
         * built-in exercise by name to its real equipment. New bodyweight
         * exercises for the "train at home" flow are also inserted so upgrading
         * users get them without a reseed. User-created exercises stay BARBELL
         * — they can edit them later.
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE exercises ADD COLUMN equipment TEXT NOT NULL DEFAULT 'BARBELL'"
                )
                // Re-tag every built-in exercise by name. Names match the seed
                // list — any drift here means upgrade users see wrong tags.
                for (ex in SEED_EXERCISES) {
                    db.execSQL(
                        "UPDATE exercises SET equipment = ? WHERE name = ?",
                        arrayOf(ex.equipment.name, ex.name)
                    )
                }
                // Insert the new bodyweight/band/pull-up-bar exercises that
                // didn't exist before v5. INSERT OR IGNORE so a user who
                // happened to create one with the same name doesn't get a
                // unique-constraint crash. Names are unique enough here that
                // collisions are very unlikely.
                val existingNamesCursor = db.query("SELECT name FROM exercises")
                val existingNames = mutableSetOf<String>()
                existingNamesCursor.use { c ->
                    while (c.moveToNext()) existingNames += c.getString(0)
                }
                for (ex in SEED_EXERCISES) {
                    if (ex.name in existingNames) continue
                    db.execSQL(
                        """
                        INSERT INTO exercises (name, muscleGroup, secondaryMuscleGroup, isCustom, equipment)
                        VALUES (?, ?, ?, 0, ?)
                        """.trimIndent(),
                        arrayOf(
                            ex.name,
                            ex.muscleGroup.name,
                            ex.secondaryMuscleGroup?.name,
                            ex.equipment.name,
                        )
                    )
                }
            }
        }

        /**
         * v5 → v6: fix the "Inverted Row tagged as BODYWEIGHT" bug — it needs
         * something to grip, so move it under PULL_UP_BAR. Also add real
         * no-equipment biceps exercises (isometric flex, self-resistance curl,
         * towel curl, side hold) so picking "Biceps + Bodyweight" returns
         * useful primary-muscle options instead of falling back to back rows.
         */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "UPDATE exercises SET equipment = 'PULL_UP_BAR' WHERE name = 'Inverted Row'"
                )
                val newBicepsExercises = listOf(
                    Triple("Isometric Biceps Flex", "BICEPS", null),
                    Triple("Self-Resistance Curl", "BICEPS", null),
                    Triple("Towel Biceps Curl", "BICEPS", "FOREARMS"),
                    Triple("Side Biceps Hold", "BICEPS", "SHOULDERS"),
                )
                val existingCursor = db.query("SELECT name FROM exercises")
                val existing = mutableSetOf<String>()
                existingCursor.use { c -> while (c.moveToNext()) existing += c.getString(0) }
                for ((name, primary, secondary) in newBicepsExercises) {
                    if (name in existing) continue
                    db.execSQL(
                        """
                        INSERT INTO exercises (name, muscleGroup, secondaryMuscleGroup, isCustom, equipment)
                        VALUES (?, ?, ?, 0, 'BODYWEIGHT')
                        """.trimIndent(),
                        arrayOf(name, primary, secondary)
                    )
                }
            }
        }

        /**
         * v6 → v7: pull in the genuine no-equipment back/posterior-chain
         * exercises from wger Workout Manager (wger.de, CC-BY-SA 3.0) that
         * free-exercise-db doesn't cover. Helps fill the "back at home with
         * nothing" gap that existing users were running into.
         */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val newWgerExercises = listOf(
                    Triple("Arm Raises T/Y/I", "BACK", "SHOULDERS"),
                    Triple("Quadruped Arm and Leg Raise", "BACK", "GLUTES"),
                    Triple("Trap-3 Raise", "BACK", "SHOULDERS"),
                    Triple("Wall Slides", "BACK", "SHOULDERS"),
                )
                val existingCursor = db.query("SELECT name FROM exercises")
                val existing = mutableSetOf<String>()
                existingCursor.use { c -> while (c.moveToNext()) existing += c.getString(0) }
                for ((name, primary, secondary) in newWgerExercises) {
                    if (name in existing) continue
                    db.execSQL(
                        """
                        INSERT INTO exercises (name, muscleGroup, secondaryMuscleGroup, isCustom, equipment)
                        VALUES (?, ?, ?, 0, 'BODYWEIGHT')
                        """.trimIndent(),
                        arrayOf(name, primary, secondary)
                    )
                }
            }
        }

        fun getInstance(context: Context): FitTrackDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FitTrackDatabase::class.java,
                    "fittrack_database"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    // Kept as a development-time safety net only. Each new schema
                    // version MUST register a real migration above; relying on the
                    // fallback in production wipes user data silently.
                    .fallbackToDestructiveMigration()
                    .addCallback(SeedCallback())
                    .build()
                INSTANCE = instance
                // Safety net for installs whose seed partially ran in the
                // past (foreign-key ID mismatches, mid-seed crashes, etc.).
                // Both repair steps are name-based and idempotent — they're
                // no-ops unless something is actually missing. Skip on a
                // truly fresh install (onCreate handles that path).
                CoroutineScope(Dispatchers.IO).launch {
                    val exerciseCount = instance.exerciseDao().getCount()
                    if (exerciseCount > 0) {
                        SeedCallback.reseedMissingExercises(instance)
                        SeedCallback.reseedRoutinesOnly(instance)
                    }
                }
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
                    seedRoutines(database.routineDao(), database.exerciseDao())
                }
            }
        }

        companion object {
            /**
             * Walk [SEED_EXERCISES] by name and insert any that don't yet
             * exist in the user's catalog. Repairs installs that ended up
             * with a partial exercises table (e.g. a previous seed crashed
             * mid-`insertAll`). Name-based dedup; never deletes anything,
             * never touches custom user-created exercises.
             */
            suspend fun reseedMissingExercises(database: FitTrackDatabase) {
                val exerciseDao = database.exerciseDao()
                val existingNames = exerciseDao.getAllExercises().first()
                    .map { it.name }.toSet()
                val missing = SEED_EXERCISES.filter { it.name !in existingNames }
                if (missing.isNotEmpty()) exerciseDao.insertAll(missing)
            }

            /** Called from the startup safety net to repair an install whose
             *  exercises are present but routines are not. */
            suspend fun reseedRoutinesOnly(database: FitTrackDatabase) {
                SeedCallback().seedRoutines(database.routineDao(), database.exerciseDao())
            }
        }

        private suspend fun seedExercises(dao: ExerciseDao) {
            dao.insertAll(SEED_EXERCISES)
        }

        /**
         * The original 59-entry catalog order. Routine seed code below references
         * exercises by their position in this list (1-based). We resolve to the
         * CURRENT row id via a name lookup at seed time — that way the routine
         * data stays correct even after the catalog grew with v5+ bodyweight
         * additions (which would otherwise shift the auto-assigned IDs).
         */
        private val ORIGINAL_CATALOG_ORDER = listOf(
            // Chest 1-8
            "Barbell Bench Press", "Incline Dumbbell Press", "Dumbbell Bench Press",
            "Cable Fly", "Chest Dip", "Push-Up", "Incline Barbell Press", "Pec Deck Machine",
            // Back 9-16
            "Barbell Row", "Pull-Up", "Lat Pulldown", "Seated Cable Row", "Dumbbell Row",
            "T-Bar Row", "Face Pull", "Deadlift",
            // Shoulders 17-23
            "Overhead Press", "Lateral Raise", "Front Raise", "Rear Delt Fly", "Arnold Press",
            "Dumbbell Shoulder Press", "Upright Row",
            // Biceps 24-30
            "Barbell Curl", "Dumbbell Curl", "Hammer Curl", "Preacher Curl",
            "Incline Dumbbell Curl", "Cable Curl", "Concentration Curl",
            // Triceps 31-36
            "Tricep Pushdown", "Skull Crushers", "Overhead Tricep Extension",
            "Close-Grip Bench Press", "Tricep Dip", "Cable Overhead Extension",
            // Legs 37-45
            "Barbell Squat", "Leg Press", "Romanian Deadlift", "Leg Extension",
            "Leg Curl", "Bulgarian Split Squat", "Front Squat", "Hack Squat", "Walking Lunge",
            // Glutes 46-49
            "Hip Thrust", "Glute Bridge", "Cable Kickback", "Sumo Deadlift",
            // Abs 50-54
            "Plank", "Cable Crunch", "Hanging Leg Raise", "Ab Wheel Rollout", "Russian Twist",
            // Calves 55-56
            "Standing Calf Raise", "Seated Calf Raise",
            // Forearms 57-59
            "Wrist Curl", "Reverse Wrist Curl", "Farmer's Walk",
        )

        private suspend fun seedRoutines(realDao: RoutineDao, exerciseDao: ExerciseDao) {
            // Build a "old position → current row id" map. We look up actual
            // ids from the db by name rather than trusting that auto-generated
            // ids match the original 1..59 sequence — they don't, since the
            // catalog grew with v5 bodyweight additions.
            val nameToId = exerciseDao.getAllExercises().first()
                .associateBy({ it.name }, { it.id })
            // Translate "old position" (1-based, from when there were only 59
            // exercises) → current row id by name. Throws loud if the name
            // isn't in the catalog so we never silently insert broken refs.
            fun exId(oldPosition: Int): Long {
                val name = ORIGINAL_CATALOG_ORDER.getOrNull(oldPosition - 1)
                    ?: error("seedRoutines: oldPosition $oldPosition out of range")
                return nameToId[name]
                    ?: error("seedRoutines: exercise '$name' not found in catalog")
            }

            // Make this seed idempotent: routines whose `name` already exists
            // are skipped (along with their child exercises). The shim object
            // below is named `dao` so the 20 existing call sites below stay
            // unchanged. The safety net in `getInstance` calls this on every
            // launch, repairing installs that previously got a partial seed.
            val existingNames = realDao.getAllNames().toMutableSet()
            var skipLast = false
            suspend fun shimInsertRoutine(routine: Routine): Long {
                if (routine.name in existingNames) {
                    skipLast = true
                    return -1L
                }
                skipLast = false
                existingNames += routine.name
                return realDao.insertRoutine(routine)
            }
            suspend fun shimInsertExercises(exercises: List<RoutineExercise>) {
                if (skipLast) return
                realDao.insertRoutineExercises(exercises)
            }
            val dao = object {
                suspend fun insertRoutine(routine: Routine): Long = shimInsertRoutine(routine)
                suspend fun insertRoutineExercises(exercises: List<RoutineExercise>) =
                    shimInsertExercises(exercises)
            }

            // ===== 3-DAY PROGRAMS =====

            // 3-Day Push/Pull/Legs
            val ppl3 = "PPL 3-Day"
            dao.insertRoutine(Routine(
                name = "Day 1: Push",
                description = "Chest, shoulders, and triceps",
                targetMuscleGroups = "CHEST,SHOULDERS,TRICEPS",
                difficulty = "intermediate",
                daysPerWeek = 3, programName = ppl3, dayOrder = 1
            )).also { id ->
                dao.insertRoutineExercises(listOf(
                    RoutineExercise(routineId = id, exerciseId = exId(1), orderIndex = 0, suggestedSets = 4, suggestedReps = "6-8"),
                    RoutineExercise(routineId = id, exerciseId = exId(2), orderIndex = 1, suggestedSets = 3, suggestedReps = "8-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(4), orderIndex = 2, suggestedSets = 3, suggestedReps = "12-15"),
                    RoutineExercise(routineId = id, exerciseId = exId(17), orderIndex = 3, suggestedSets = 3, suggestedReps = "6-8"),
                    RoutineExercise(routineId = id, exerciseId = exId(18), orderIndex = 4, suggestedSets = 3, suggestedReps = "12-15"),
                    RoutineExercise(routineId = id, exerciseId = exId(31), orderIndex = 5, suggestedSets = 3, suggestedReps = "10-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(52), orderIndex = 6, suggestedSets = 3, suggestedReps = "8-12")
                ))
            }
            dao.insertRoutine(Routine(
                name = "Day 2: Pull",
                description = "Back and biceps",
                targetMuscleGroups = "BACK,BICEPS",
                difficulty = "intermediate",
                daysPerWeek = 3, programName = ppl3, dayOrder = 2
            )).also { id ->
                dao.insertRoutineExercises(listOf(
                    RoutineExercise(routineId = id, exerciseId = exId(16), orderIndex = 0, suggestedSets = 3, suggestedReps = "5-6"),
                    RoutineExercise(routineId = id, exerciseId = exId(9), orderIndex = 1, suggestedSets = 4, suggestedReps = "6-8"),
                    RoutineExercise(routineId = id, exerciseId = exId(10), orderIndex = 2, suggestedSets = 3, suggestedReps = "6-10"),
                    RoutineExercise(routineId = id, exerciseId = exId(12), orderIndex = 3, suggestedSets = 3, suggestedReps = "10-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(15), orderIndex = 4, suggestedSets = 3, suggestedReps = "15-20"),
                    RoutineExercise(routineId = id, exerciseId = exId(24), orderIndex = 5, suggestedSets = 3, suggestedReps = "8-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(51), orderIndex = 6, suggestedSets = 3, suggestedReps = "12-15")
                ))
            }
            dao.insertRoutine(Routine(
                name = "Day 3: Legs",
                description = "Quads, hamstrings, glutes, and calves",
                targetMuscleGroups = "LEGS,GLUTES,CALVES",
                difficulty = "intermediate",
                daysPerWeek = 3, programName = ppl3, dayOrder = 3
            )).also { id ->
                dao.insertRoutineExercises(listOf(
                    RoutineExercise(routineId = id, exerciseId = exId(37), orderIndex = 0, suggestedSets = 4, suggestedReps = "5-8"),
                    RoutineExercise(routineId = id, exerciseId = exId(38), orderIndex = 1, suggestedSets = 3, suggestedReps = "8-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(39), orderIndex = 2, suggestedSets = 3, suggestedReps = "8-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(40), orderIndex = 3, suggestedSets = 3, suggestedReps = "12-15"),
                    RoutineExercise(routineId = id, exerciseId = exId(41), orderIndex = 4, suggestedSets = 3, suggestedReps = "10-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(46), orderIndex = 5, suggestedSets = 3, suggestedReps = "8-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(53), orderIndex = 6, suggestedSets = 4, suggestedReps = "12-15")
                ))
            }

            // 3-Day Full Body
            val fb3 = "Full Body 3-Day"
            dao.insertRoutine(Routine(
                name = "Day 1: Full Body A",
                description = "Squat-focused full body with chest and back",
                targetMuscleGroups = "LEGS,CHEST,BACK,SHOULDERS",
                difficulty = "beginner",
                daysPerWeek = 3, programName = fb3, dayOrder = 1
            )).also { id ->
                dao.insertRoutineExercises(listOf(
                    RoutineExercise(routineId = id, exerciseId = exId(37), orderIndex = 0, suggestedSets = 3, suggestedReps = "5-8"),
                    RoutineExercise(routineId = id, exerciseId = exId(1), orderIndex = 1, suggestedSets = 3, suggestedReps = "6-8"),
                    RoutineExercise(routineId = id, exerciseId = exId(9), orderIndex = 2, suggestedSets = 3, suggestedReps = "8-10"),
                    RoutineExercise(routineId = id, exerciseId = exId(17), orderIndex = 3, suggestedSets = 3, suggestedReps = "8-10"),
                    RoutineExercise(routineId = id, exerciseId = exId(24), orderIndex = 4, suggestedSets = 2, suggestedReps = "10-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(31), orderIndex = 5, suggestedSets = 2, suggestedReps = "10-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(50), orderIndex = 6, suggestedSets = 3, suggestedReps = "30-60")
                ))
            }
            dao.insertRoutine(Routine(
                name = "Day 2: Full Body B",
                description = "Deadlift-focused full body with shoulders and arms",
                targetMuscleGroups = "BACK,LEGS,SHOULDERS,BICEPS,TRICEPS",
                difficulty = "beginner",
                daysPerWeek = 3, programName = fb3, dayOrder = 2
            )).also { id ->
                dao.insertRoutineExercises(listOf(
                    RoutineExercise(routineId = id, exerciseId = exId(16), orderIndex = 0, suggestedSets = 3, suggestedReps = "5-6"),
                    RoutineExercise(routineId = id, exerciseId = exId(38), orderIndex = 1, suggestedSets = 3, suggestedReps = "8-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(22), orderIndex = 2, suggestedSets = 3, suggestedReps = "8-10"),
                    RoutineExercise(routineId = id, exerciseId = exId(11), orderIndex = 3, suggestedSets = 3, suggestedReps = "8-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(25), orderIndex = 4, suggestedSets = 3, suggestedReps = "10-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(32), orderIndex = 5, suggestedSets = 3, suggestedReps = "10-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(51), orderIndex = 6, suggestedSets = 3, suggestedReps = "12-15")
                ))
            }
            dao.insertRoutine(Routine(
                name = "Day 3: Full Body C",
                description = "Front squat-focused full body with chest and back variations",
                targetMuscleGroups = "LEGS,CHEST,BACK,ABS",
                difficulty = "beginner",
                daysPerWeek = 3, programName = fb3, dayOrder = 3
            )).also { id ->
                dao.insertRoutineExercises(listOf(
                    RoutineExercise(routineId = id, exerciseId = exId(43), orderIndex = 0, suggestedSets = 3, suggestedReps = "6-8"),
                    RoutineExercise(routineId = id, exerciseId = exId(3), orderIndex = 1, suggestedSets = 3, suggestedReps = "8-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(10), orderIndex = 2, suggestedSets = 3, suggestedReps = "6-10"),
                    RoutineExercise(routineId = id, exerciseId = exId(18), orderIndex = 3, suggestedSets = 3, suggestedReps = "12-15"),
                    RoutineExercise(routineId = id, exerciseId = exId(46), orderIndex = 4, suggestedSets = 3, suggestedReps = "8-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(51), orderIndex = 5, suggestedSets = 3, suggestedReps = "10-15")
                ))
            }

            // ===== 5-DAY PROGRAMS =====

            // 5-Day Bro Split
            val bro5 = "Bro Split 5-Day"
            dao.insertRoutine(Routine(
                name = "Day 1: Chest",
                description = "Chest-focused with heavy pressing and isolation",
                targetMuscleGroups = "CHEST",
                difficulty = "intermediate",
                daysPerWeek = 5, programName = bro5, dayOrder = 1
            )).also { id ->
                dao.insertRoutineExercises(listOf(
                    RoutineExercise(routineId = id, exerciseId = exId(1), orderIndex = 0, suggestedSets = 4, suggestedReps = "6-8"),
                    RoutineExercise(routineId = id, exerciseId = exId(2), orderIndex = 1, suggestedSets = 3, suggestedReps = "8-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(7), orderIndex = 2, suggestedSets = 3, suggestedReps = "8-10"),
                    RoutineExercise(routineId = id, exerciseId = exId(4), orderIndex = 3, suggestedSets = 3, suggestedReps = "12-15"),
                    RoutineExercise(routineId = id, exerciseId = exId(8), orderIndex = 4, suggestedSets = 3, suggestedReps = "12-15"),
                    RoutineExercise(routineId = id, exerciseId = exId(5), orderIndex = 5, suggestedSets = 3, suggestedReps = "8-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(50), orderIndex = 6, suggestedSets = 3, suggestedReps = "30-60")
                ))
            }
            dao.insertRoutine(Routine(
                name = "Day 2: Back",
                description = "Back-focused with rows, pulldowns, and deadlifts",
                targetMuscleGroups = "BACK",
                difficulty = "intermediate",
                daysPerWeek = 5, programName = bro5, dayOrder = 2
            )).also { id ->
                dao.insertRoutineExercises(listOf(
                    RoutineExercise(routineId = id, exerciseId = exId(16), orderIndex = 0, suggestedSets = 3, suggestedReps = "5-6"),
                    RoutineExercise(routineId = id, exerciseId = exId(9), orderIndex = 1, suggestedSets = 4, suggestedReps = "6-8"),
                    RoutineExercise(routineId = id, exerciseId = exId(10), orderIndex = 2, suggestedSets = 3, suggestedReps = "6-10"),
                    RoutineExercise(routineId = id, exerciseId = exId(11), orderIndex = 3, suggestedSets = 3, suggestedReps = "8-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(14), orderIndex = 4, suggestedSets = 3, suggestedReps = "8-10"),
                    RoutineExercise(routineId = id, exerciseId = exId(15), orderIndex = 5, suggestedSets = 3, suggestedReps = "15-20"),
                    RoutineExercise(routineId = id, exerciseId = exId(52), orderIndex = 6, suggestedSets = 3, suggestedReps = "8-12")
                ))
            }
            dao.insertRoutine(Routine(
                name = "Day 3: Shoulders",
                description = "Shoulders with overhead pressing and all three delt heads",
                targetMuscleGroups = "SHOULDERS",
                difficulty = "intermediate",
                daysPerWeek = 5, programName = bro5, dayOrder = 3
            )).also { id ->
                dao.insertRoutineExercises(listOf(
                    RoutineExercise(routineId = id, exerciseId = exId(17), orderIndex = 0, suggestedSets = 4, suggestedReps = "6-8"),
                    RoutineExercise(routineId = id, exerciseId = exId(22), orderIndex = 1, suggestedSets = 3, suggestedReps = "8-10"),
                    RoutineExercise(routineId = id, exerciseId = exId(18), orderIndex = 2, suggestedSets = 4, suggestedReps = "12-15"),
                    RoutineExercise(routineId = id, exerciseId = exId(19), orderIndex = 3, suggestedSets = 3, suggestedReps = "10-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(20), orderIndex = 4, suggestedSets = 4, suggestedReps = "12-15"),
                    RoutineExercise(routineId = id, exerciseId = exId(23), orderIndex = 5, suggestedSets = 3, suggestedReps = "10-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(51), orderIndex = 6, suggestedSets = 3, suggestedReps = "12-15")
                ))
            }
            dao.insertRoutine(Routine(
                name = "Day 4: Legs",
                description = "Full leg day with quads, hamstrings, glutes, and calves",
                targetMuscleGroups = "LEGS,GLUTES,CALVES",
                difficulty = "intermediate",
                daysPerWeek = 5, programName = bro5, dayOrder = 4
            )).also { id ->
                dao.insertRoutineExercises(listOf(
                    RoutineExercise(routineId = id, exerciseId = exId(37), orderIndex = 0, suggestedSets = 4, suggestedReps = "5-8"),
                    RoutineExercise(routineId = id, exerciseId = exId(38), orderIndex = 1, suggestedSets = 3, suggestedReps = "8-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(39), orderIndex = 2, suggestedSets = 3, suggestedReps = "8-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(40), orderIndex = 3, suggestedSets = 3, suggestedReps = "12-15"),
                    RoutineExercise(routineId = id, exerciseId = exId(41), orderIndex = 4, suggestedSets = 3, suggestedReps = "10-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(46), orderIndex = 5, suggestedSets = 3, suggestedReps = "8-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(53), orderIndex = 6, suggestedSets = 4, suggestedReps = "12-15")
                ))
            }
            dao.insertRoutine(Routine(
                name = "Day 5: Arms",
                description = "Biceps, triceps, and forearms",
                targetMuscleGroups = "BICEPS,TRICEPS,FOREARMS",
                difficulty = "intermediate",
                daysPerWeek = 5, programName = bro5, dayOrder = 5
            )).also { id ->
                dao.insertRoutineExercises(listOf(
                    RoutineExercise(routineId = id, exerciseId = exId(24), orderIndex = 0, suggestedSets = 3, suggestedReps = "8-10"),
                    RoutineExercise(routineId = id, exerciseId = exId(32), orderIndex = 1, suggestedSets = 3, suggestedReps = "8-10"),
                    RoutineExercise(routineId = id, exerciseId = exId(26), orderIndex = 2, suggestedSets = 3, suggestedReps = "10-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(31), orderIndex = 3, suggestedSets = 3, suggestedReps = "10-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(28), orderIndex = 4, suggestedSets = 3, suggestedReps = "10-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(33), orderIndex = 5, suggestedSets = 3, suggestedReps = "12-15"),
                    RoutineExercise(routineId = id, exerciseId = exId(30), orderIndex = 6, suggestedSets = 3, suggestedReps = "10-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(55), orderIndex = 7, suggestedSets = 3, suggestedReps = "12-15")
                ))
            }

            // 5-Day Upper/Lower/Push/Pull/Legs
            val ulppl5 = "Upper/Lower/PPL 5-Day"
            dao.insertRoutine(Routine(
                name = "Day 1: Upper Body",
                description = "Heavy upper body compound movements",
                targetMuscleGroups = "CHEST,BACK,SHOULDERS",
                difficulty = "intermediate",
                daysPerWeek = 5, programName = ulppl5, dayOrder = 1
            )).also { id ->
                dao.insertRoutineExercises(listOf(
                    RoutineExercise(routineId = id, exerciseId = exId(1), orderIndex = 0, suggestedSets = 4, suggestedReps = "5-6"),
                    RoutineExercise(routineId = id, exerciseId = exId(9), orderIndex = 1, suggestedSets = 4, suggestedReps = "6-8"),
                    RoutineExercise(routineId = id, exerciseId = exId(17), orderIndex = 2, suggestedSets = 3, suggestedReps = "6-8"),
                    RoutineExercise(routineId = id, exerciseId = exId(11), orderIndex = 3, suggestedSets = 3, suggestedReps = "8-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(4), orderIndex = 4, suggestedSets = 3, suggestedReps = "12-15"),
                    RoutineExercise(routineId = id, exerciseId = exId(15), orderIndex = 5, suggestedSets = 3, suggestedReps = "15-20"),
                    RoutineExercise(routineId = id, exerciseId = exId(50), orderIndex = 6, suggestedSets = 3, suggestedReps = "30-60")
                ))
            }
            dao.insertRoutine(Routine(
                name = "Day 2: Lower Body",
                description = "Heavy lower body compound movements",
                targetMuscleGroups = "LEGS,GLUTES,CALVES",
                difficulty = "intermediate",
                daysPerWeek = 5, programName = ulppl5, dayOrder = 2
            )).also { id ->
                dao.insertRoutineExercises(listOf(
                    RoutineExercise(routineId = id, exerciseId = exId(37), orderIndex = 0, suggestedSets = 4, suggestedReps = "5-6"),
                    RoutineExercise(routineId = id, exerciseId = exId(39), orderIndex = 1, suggestedSets = 3, suggestedReps = "8-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(38), orderIndex = 2, suggestedSets = 3, suggestedReps = "8-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(46), orderIndex = 3, suggestedSets = 3, suggestedReps = "8-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(42), orderIndex = 4, suggestedSets = 3, suggestedReps = "8-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(53), orderIndex = 5, suggestedSets = 4, suggestedReps = "12-15")
                ))
            }
            dao.insertRoutine(Routine(
                name = "Day 3: Push",
                description = "Hypertrophy push day - chest, shoulders, triceps",
                targetMuscleGroups = "CHEST,SHOULDERS,TRICEPS",
                difficulty = "intermediate",
                daysPerWeek = 5, programName = ulppl5, dayOrder = 3
            )).also { id ->
                dao.insertRoutineExercises(listOf(
                    RoutineExercise(routineId = id, exerciseId = exId(2), orderIndex = 0, suggestedSets = 4, suggestedReps = "8-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(3), orderIndex = 1, suggestedSets = 3, suggestedReps = "10-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(4), orderIndex = 2, suggestedSets = 3, suggestedReps = "12-15"),
                    RoutineExercise(routineId = id, exerciseId = exId(22), orderIndex = 3, suggestedSets = 3, suggestedReps = "10-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(18), orderIndex = 4, suggestedSets = 4, suggestedReps = "12-15"),
                    RoutineExercise(routineId = id, exerciseId = exId(31), orderIndex = 5, suggestedSets = 3, suggestedReps = "10-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(33), orderIndex = 6, suggestedSets = 3, suggestedReps = "12-15"),
                    RoutineExercise(routineId = id, exerciseId = exId(52), orderIndex = 7, suggestedSets = 3, suggestedReps = "8-12")
                ))
            }
            dao.insertRoutine(Routine(
                name = "Day 4: Pull",
                description = "Hypertrophy pull day - back and biceps",
                targetMuscleGroups = "BACK,BICEPS",
                difficulty = "intermediate",
                daysPerWeek = 5, programName = ulppl5, dayOrder = 4
            )).also { id ->
                dao.insertRoutineExercises(listOf(
                    RoutineExercise(routineId = id, exerciseId = exId(10), orderIndex = 0, suggestedSets = 4, suggestedReps = "6-10"),
                    RoutineExercise(routineId = id, exerciseId = exId(12), orderIndex = 1, suggestedSets = 3, suggestedReps = "10-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(13), orderIndex = 2, suggestedSets = 3, suggestedReps = "10-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(15), orderIndex = 3, suggestedSets = 4, suggestedReps = "15-20"),
                    RoutineExercise(routineId = id, exerciseId = exId(25), orderIndex = 4, suggestedSets = 3, suggestedReps = "10-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(26), orderIndex = 5, suggestedSets = 3, suggestedReps = "10-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(30), orderIndex = 6, suggestedSets = 3, suggestedReps = "10-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(51), orderIndex = 7, suggestedSets = 3, suggestedReps = "12-15")
                ))
            }
            dao.insertRoutine(Routine(
                name = "Day 5: Legs",
                description = "Hypertrophy leg day with quad and hamstring focus",
                targetMuscleGroups = "LEGS,GLUTES,CALVES",
                difficulty = "intermediate",
                daysPerWeek = 5, programName = ulppl5, dayOrder = 5
            )).also { id ->
                dao.insertRoutineExercises(listOf(
                    RoutineExercise(routineId = id, exerciseId = exId(43), orderIndex = 0, suggestedSets = 4, suggestedReps = "8-10"),
                    RoutineExercise(routineId = id, exerciseId = exId(44), orderIndex = 1, suggestedSets = 3, suggestedReps = "8-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(40), orderIndex = 2, suggestedSets = 3, suggestedReps = "12-15"),
                    RoutineExercise(routineId = id, exerciseId = exId(41), orderIndex = 3, suggestedSets = 3, suggestedReps = "10-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(45), orderIndex = 4, suggestedSets = 3, suggestedReps = "10-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(53), orderIndex = 5, suggestedSets = 4, suggestedReps = "12-15"),
                    RoutineExercise(routineId = id, exerciseId = exId(54), orderIndex = 6, suggestedSets = 4, suggestedReps = "15-20")
                ))
            }

            // ===== STANDALONE ROUTINES (for individual sessions) =====

            // Upper/Lower standalone
            dao.insertRoutine(Routine(
                name = "Upper Body",
                description = "Complete upper body workout targeting chest, back, shoulders, and arms",
                targetMuscleGroups = "CHEST,BACK,SHOULDERS,BICEPS,TRICEPS",
                difficulty = "intermediate"
            )).also { id ->
                dao.insertRoutineExercises(listOf(
                    RoutineExercise(routineId = id, exerciseId = exId(1), orderIndex = 0, suggestedSets = 4, suggestedReps = "6-8"),
                    RoutineExercise(routineId = id, exerciseId = exId(9), orderIndex = 1, suggestedSets = 4, suggestedReps = "6-8"),
                    RoutineExercise(routineId = id, exerciseId = exId(17), orderIndex = 2, suggestedSets = 3, suggestedReps = "8-10"),
                    RoutineExercise(routineId = id, exerciseId = exId(11), orderIndex = 3, suggestedSets = 3, suggestedReps = "8-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(24), orderIndex = 4, suggestedSets = 3, suggestedReps = "10-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(31), orderIndex = 5, suggestedSets = 3, suggestedReps = "10-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(50), orderIndex = 6, suggestedSets = 3, suggestedReps = "30-60")
                ))
            }

            dao.insertRoutine(Routine(
                name = "Lower Body",
                description = "Complete lower body workout targeting quads, hamstrings, glutes, and calves",
                targetMuscleGroups = "LEGS,GLUTES,CALVES",
                difficulty = "intermediate"
            )).also { id ->
                dao.insertRoutineExercises(listOf(
                    RoutineExercise(routineId = id, exerciseId = exId(37), orderIndex = 0, suggestedSets = 4, suggestedReps = "5-8"),
                    RoutineExercise(routineId = id, exerciseId = exId(39), orderIndex = 1, suggestedSets = 3, suggestedReps = "8-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(38), orderIndex = 2, suggestedSets = 3, suggestedReps = "8-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(42), orderIndex = 3, suggestedSets = 3, suggestedReps = "8-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(46), orderIndex = 4, suggestedSets = 3, suggestedReps = "8-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(53), orderIndex = 5, suggestedSets = 4, suggestedReps = "12-15"),
                    RoutineExercise(routineId = id, exerciseId = exId(54), orderIndex = 6, suggestedSets = 4, suggestedReps = "15-20")
                ))
            }

            // Chest & Triceps
            dao.insertRoutine(Routine(
                name = "Chest & Triceps",
                description = "Focused chest and triceps session",
                targetMuscleGroups = "CHEST,TRICEPS",
                difficulty = "intermediate"
            )).also { id ->
                dao.insertRoutineExercises(listOf(
                    RoutineExercise(routineId = id, exerciseId = exId(1), orderIndex = 0, suggestedSets = 4, suggestedReps = "6-8"),
                    RoutineExercise(routineId = id, exerciseId = exId(2), orderIndex = 1, suggestedSets = 3, suggestedReps = "8-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(3), orderIndex = 2, suggestedSets = 3, suggestedReps = "8-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(4), orderIndex = 3, suggestedSets = 3, suggestedReps = "12-15"),
                    RoutineExercise(routineId = id, exerciseId = exId(32), orderIndex = 4, suggestedSets = 3, suggestedReps = "10-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(31), orderIndex = 5, suggestedSets = 3, suggestedReps = "10-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(33), orderIndex = 6, suggestedSets = 3, suggestedReps = "12-15"),
                    RoutineExercise(routineId = id, exerciseId = exId(52), orderIndex = 7, suggestedSets = 3, suggestedReps = "8-12")
                ))
            }

            // Back & Biceps
            dao.insertRoutine(Routine(
                name = "Back & Biceps",
                description = "Focused back and biceps session",
                targetMuscleGroups = "BACK,BICEPS",
                difficulty = "intermediate"
            )).also { id ->
                dao.insertRoutineExercises(listOf(
                    RoutineExercise(routineId = id, exerciseId = exId(16), orderIndex = 0, suggestedSets = 3, suggestedReps = "5-6"),
                    RoutineExercise(routineId = id, exerciseId = exId(10), orderIndex = 1, suggestedSets = 4, suggestedReps = "6-10"),
                    RoutineExercise(routineId = id, exerciseId = exId(9), orderIndex = 2, suggestedSets = 3, suggestedReps = "8-10"),
                    RoutineExercise(routineId = id, exerciseId = exId(12), orderIndex = 3, suggestedSets = 3, suggestedReps = "10-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(15), orderIndex = 4, suggestedSets = 3, suggestedReps = "15-20"),
                    RoutineExercise(routineId = id, exerciseId = exId(24), orderIndex = 5, suggestedSets = 3, suggestedReps = "8-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(26), orderIndex = 6, suggestedSets = 3, suggestedReps = "10-12"),
                    RoutineExercise(routineId = id, exerciseId = exId(51), orderIndex = 7, suggestedSets = 3, suggestedReps = "12-15")
                ))
            }
        }
    }
}
