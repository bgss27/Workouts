package com.fittrack.app.data.repository

import android.content.Context
import org.json.JSONArray

/**
 * Loads the bundled free-exercise-db dataset (yuhonas/free-exercise-db, public
 * domain / Unlicense, ~870 entries) on first access and exposes a lookup of
 * step-by-step instructions per exercise.
 *
 * The dataset is the same one that powers [ExerciseImageRepository] — by
 * pulling instructions from it too, our descriptions are sourced from a real
 * open-source DB instead of hand-written guesses, and they ship under a
 * commercially-safe license.
 *
 * Lookup strategy when resolving an app exercise name:
 *   1. Curated app-name → dataset-id mapping (handles names that differ).
 *   2. Exact match against the dataset's `name` field.
 *   3. Fuzzy match: app name normalized to the dataset's id convention.
 *
 * Unmatched exercises fall through to [ExerciseInstructions] which contains
 * the hand-written entries for things the open dataset doesn't cover
 * (no-equipment biceps work, novel bodyweight variants, etc.).
 */
object OpenExerciseDb {

    /**
     * App name → dataset id (which matches the image folder name). Built by
     * augmenting [ExerciseImageRepository]'s map with bodyweight-era additions.
     * Entries here take precedence over name/fuzzy matching.
     */
    private val nameToDatasetId: Map<String, String> = mapOf(
        // Existing exercises (mirrors ExerciseImageRepository.imageMap)
        "Barbell Bench Press" to "Barbell_Bench_Press_-_Medium_Grip",
        "Incline Dumbbell Press" to "Incline_Dumbbell_Press",
        "Dumbbell Bench Press" to "Dumbbell_Bench_Press",
        "Cable Fly" to "Flat_Bench_Cable_Flyes",
        "Chest Dip" to "Dips_-_Chest_Version",
        "Push-Up" to "Pushups",
        "Incline Barbell Press" to "Barbell_Incline_Bench_Press_-_Medium_Grip",
        "Pec Deck Machine" to "Butterfly",
        "Barbell Row" to "Bent_Over_Barbell_Row",
        "Pull-Up" to "Pullups",
        "Lat Pulldown" to "Wide-Grip_Lat_Pulldown",
        "Seated Cable Row" to "Seated_Cable_Rows",
        "Dumbbell Row" to "One-Arm_Dumbbell_Row",
        "T-Bar Row" to "T-Bar_Row_with_Handle",
        "Face Pull" to "Face_Pull",
        "Deadlift" to "Barbell_Deadlift",
        "Overhead Press" to "Standing_Military_Press",
        "Lateral Raise" to "Side_Lateral_Raise",
        "Front Raise" to "Front_Dumbbell_Raise",
        "Rear Delt Fly" to "Seated_Bent-Over_Rear_Delt_Raise",
        "Arnold Press" to "Arnold_Dumbbell_Press",
        "Dumbbell Shoulder Press" to "Dumbbell_Shoulder_Press",
        "Upright Row" to "Upright_Barbell_Row",
        "Barbell Curl" to "Barbell_Curl",
        "Dumbbell Curl" to "Dumbbell_Bicep_Curl",
        "Hammer Curl" to "Alternate_Hammer_Curl",
        "Preacher Curl" to "Preacher_Curl",
        "Incline Dumbbell Curl" to "Incline_Dumbbell_Curl",
        "Cable Curl" to "Standing_Biceps_Cable_Curl",
        "Concentration Curl" to "Concentration_Curls",
        "Tricep Pushdown" to "Triceps_Pushdown",
        "Skull Crushers" to "Lying_Triceps_Press",
        "Overhead Tricep Extension" to "Dumbbell_One-Arm_Triceps_Extension",
        "Close-Grip Bench Press" to "Close-Grip_Barbell_Bench_Press",
        "Cable Overhead Extension" to "Lying_Cable_Curl",
        "Barbell Squat" to "Barbell_Squat",
        "Leg Press" to "Leg_Press",
        "Romanian Deadlift" to "Romanian_Deadlift",
        "Leg Extension" to "Leg_Extensions",
        "Leg Curl" to "Lying_Leg_Curls",
        "Bulgarian Split Squat" to "Single_Leg_Push-off",
        "Front Squat" to "Front_Barbell_Squat",
        "Hack Squat" to "Hack_Squat",
        "Walking Lunge" to "Dumbbell_Lunges",
        "Hip Thrust" to "Barbell_Hip_Thrust",
        "Glute Bridge" to "Barbell_Glute_Bridge",
        "Cable Kickback" to "Glute_Kickback",
        "Sumo Deadlift" to "Sumo_Deadlift",
        "Plank" to "Plank",
        "Cable Crunch" to "Cable_Crunch",
        "Hanging Leg Raise" to "Hanging_Leg_Raise",
        "Ab Wheel Rollout" to "Ab_Roller",
        "Russian Twist" to "Russian_Twist",
        "Standing Calf Raise" to "Standing_Calf_Raises",
        "Seated Calf Raise" to "Seated_Calf_Raise",
        "Wrist Curl" to "Palms-Down_Wrist_Curl_Over_A_Bench",
        "Reverse Wrist Curl" to "Palms-Up_Barbell_Wrist_Curl_Over_A_Bench",
        "Farmer's Walk" to "Farmer's_Walk",

        // v5+ bodyweight additions with dataset coverage
        "Wide Push-Up" to "Push-Up_Wide",
        "Decline Push-Up" to "Decline_Push-Up",
        "Incline Push-Up" to "Incline_Push-Up",
        "Inverted Row" to "Inverted_Row",
        "Superman Hold" to "Superman",
        "Chin-Up" to "Chin-Up",
        "Bodyweight Squat" to "Bodyweight_Squat",
        "Mountain Climbers" to "Mountain_Climbers",
        "Dead Bug" to "Dead_Bug",
        "Single-Leg Glute Bridge" to "Single_Leg_Glute_Bridge",
        "Lying Leg Raise" to "Flat_Bench_Lying_Leg_Raise",
        "Tricep Dip" to "Bench_Dips",
    )

    @Volatile
    private var cache: Map<String, String>? = null

    fun getInstructions(context: Context, exerciseName: String): String? {
        val map = ensureLoaded(context)
        // 1. Curated mapping
        nameToDatasetId[exerciseName]?.let { id -> map[id]?.let { return it } }
        // 2. Exact name match (entries indexed by both id and name)
        map[exerciseName]?.let { return it }
        // 3. Fuzzy id: app name → underscore-separated
        val fuzzyId = exerciseName.replace(" ", "_").replace("-", "-")
        map[fuzzyId]?.let { return it }
        return null
    }

    @Synchronized
    private fun ensureLoaded(context: Context): Map<String, String> {
        cache?.let { return it }
        val raw = context.assets.open("free_exercise_db.json")
            .bufferedReader()
            .use { it.readText() }
        val arr = JSONArray(raw)
        val result = HashMap<String, String>(arr.length() * 2)
        for (i in 0 until arr.length()) {
            val ex = arr.optJSONObject(i) ?: continue
            val id = ex.optString("id")
            val name = ex.optString("name")
            val instructionsArr = ex.optJSONArray("instructions") ?: continue
            val sb = StringBuilder()
            for (j in 0 until instructionsArr.length()) {
                if (j > 0) sb.append("\n\n")
                sb.append(instructionsArr.optString(j))
            }
            val text = sb.toString()
            if (text.isBlank()) continue
            if (id.isNotEmpty()) result[id] = text
            if (name.isNotEmpty()) result[name] = text
        }
        cache = result
        return result
    }
}
