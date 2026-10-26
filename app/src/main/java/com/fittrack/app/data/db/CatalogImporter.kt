package com.fittrack.app.data.db

import android.content.Context
import com.fittrack.app.data.db.dao.ExerciseDao
import com.fittrack.app.data.entity.Equipment
import com.fittrack.app.data.entity.Exercise
import com.fittrack.app.data.entity.MuscleGroup
import com.fittrack.app.data.repository.ExerciseImageRepository
import com.fittrack.app.data.repository.OpenExerciseDb
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

/**
 * Grows the exercise catalog from the bundled free-exercise-db JSON
 * (public domain text; see OpenExerciseDb). The hand-written
 * [SEED_EXERCISES] stay authoritative: dataset entries that those already
 * cover (via the curated name maps) are skipped, as are stretches, which
 * aren't strength-training content.
 *
 * Idempotent and name-keyed, so it's safe to run on every upgrade. A prefs
 * version stamp keeps it from re-parsing the 1 MB asset on every launch.
 */
object CatalogImporter {
    /** Bump when the mapping below changes in a way that should re-run. */
    const val CATALOG_VERSION = 1

    private const val PREFS = "fittrack_catalog"
    private const val KEY_VERSION = "imported_version"
    private const val ASSET = "free_exercise_db.json"

    /** Run once per [CATALOG_VERSION] on existing installs. */
    suspend fun importIfNeeded(context: Context, dao: ExerciseDao) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getInt(KEY_VERSION, 0) >= CATALOG_VERSION) return
        importMissing(context, dao)
        prefs.edit().putInt(KEY_VERSION, CATALOG_VERSION).apply()
    }

    /** Insert every dataset exercise whose name isn't already in the catalog. */
    suspend fun importMissing(context: Context, dao: ExerciseDao) {
        val existing = dao.getAllExercises().first().map { normalize(it.name) }.toHashSet()
        val fresh = load(context).filter { normalize(it.name) !in existing }
        if (fresh.isNotEmpty()) dao.insertAll(fresh)
    }

    /** Parse and map the bundled dataset into catalog rows (not yet inserted). */
    fun load(context: Context): List<Exercise> {
        val raw = context.assets.open(ASSET).bufferedReader().use { it.readText() }
        val arr = JSONArray(raw)
        val covered = OpenExerciseDb.mappedDatasetIds() + ExerciseImageRepository.mappedDatasetIds()
        val seenNames = HashSet<String>()
        val out = ArrayList<Exercise>(arr.length())
        for (i in 0 until arr.length()) {
            val ex = arr.optJSONObject(i) ?: continue
            val id = ex.optString("id")
            val name = ex.optString("name").trim()
            if (name.isEmpty() || id in covered) continue
            if (ex.optString("category") == "stretching") continue
            if (!seenNames.add(normalize(name))) continue

            val primaries = ex.optJSONArray("primaryMuscles").toStringList()
            val secondaries = ex.optJSONArray("secondaryMuscles").toStringList()
            val primary = primaries.firstNotNullOfOrNull { muscle(it) }
                ?: if (ex.optString("category") == "cardio") MuscleGroup.CARDIO else MuscleGroup.FULL_BODY
            val secondary = secondaries.firstNotNullOfOrNull { muscle(it) }?.takeIf { it != primary }

            out += Exercise(
                name = name,
                muscleGroup = primary,
                secondaryMuscleGroup = secondary,
                isCustom = false,
                equipment = equipment(ex.optString("equipment", "")),
            )
        }
        return out
    }

    private fun muscle(raw: String): MuscleGroup? = when (raw.lowercase()) {
        "abdominals" -> MuscleGroup.ABS
        "quadriceps", "hamstrings", "adductors", "abductors" -> MuscleGroup.LEGS
        "glutes" -> MuscleGroup.GLUTES
        "calves" -> MuscleGroup.CALVES
        "chest" -> MuscleGroup.CHEST
        "lats", "middle back", "lower back", "traps" -> MuscleGroup.BACK
        "shoulders", "neck" -> MuscleGroup.SHOULDERS
        "biceps" -> MuscleGroup.BICEPS
        "triceps" -> MuscleGroup.TRICEPS
        "forearms" -> MuscleGroup.FOREARMS
        else -> null
    }

    private fun equipment(raw: String): Equipment = when (raw.lowercase()) {
        "barbell", "e-z curl bar" -> Equipment.BARBELL
        "dumbbell" -> Equipment.DUMBBELL
        "machine", "cable" -> Equipment.MACHINE
        "bands" -> Equipment.BAND
        "kettlebells" -> Equipment.KETTLEBELL
        "body only", "", "null" -> Equipment.BODYWEIGHT
        else -> Equipment.OTHER // other, medicine ball, exercise ball, foam roll
    }

    private fun normalize(name: String): String =
        name.lowercase().filter { it.isLetterOrDigit() }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).map { optString(it) }
    }

    @Suppress("unused")
    private fun JSONObject.str(key: String): String = optString(key, "")
}
