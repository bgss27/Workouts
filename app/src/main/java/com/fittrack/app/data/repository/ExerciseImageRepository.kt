package com.fittrack.app.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class ExerciseMedia(
    val gifUrl: String,
    val name: String,
    val target: String,
    val secondaryMuscles: List<String>
)

object ExerciseImageRepository {
    // ExerciseDB open-source API
    private const val BASE_URL = "https://exercisedb-api.vercel.app/api/v1"

    private val cache = mutableMapOf<String, ExerciseMedia?>()

    // Mapping our exercise names to ExerciseDB search terms
    private val searchTermOverrides = mapOf(
        "Barbell Bench Press" to "barbell bench press",
        "Incline Dumbbell Press" to "incline dumbbell press",
        "Dumbbell Bench Press" to "dumbbell bench press",
        "Cable Fly" to "cable fly",
        "Chest Dip" to "chest dip",
        "Push-Up" to "push up",
        "Incline Barbell Press" to "incline barbell bench press",
        "Pec Deck Machine" to "pec deck",
        "Barbell Row" to "barbell bent over row",
        "Pull-Up" to "pull up",
        "Lat Pulldown" to "lat pulldown",
        "Seated Cable Row" to "seated cable row",
        "Dumbbell Row" to "dumbbell bent over row",
        "T-Bar Row" to "t bar bent over row",
        "Face Pull" to "cable face pull",
        "Deadlift" to "barbell deadlift",
        "Overhead Press" to "barbell overhead press",
        "Lateral Raise" to "dumbbell lateral raise",
        "Front Raise" to "dumbbell front raise",
        "Rear Delt Fly" to "dumbbell rear delt fly",
        "Arnold Press" to "dumbbell arnold press",
        "Dumbbell Shoulder Press" to "dumbbell shoulder press",
        "Upright Row" to "barbell upright row",
        "Barbell Curl" to "barbell curl",
        "Dumbbell Curl" to "dumbbell curl",
        "Hammer Curl" to "dumbbell hammer curl",
        "Preacher Curl" to "barbell preacher curl",
        "Incline Dumbbell Curl" to "dumbbell incline curl",
        "Cable Curl" to "cable curl",
        "Concentration Curl" to "dumbbell concentration curl",
        "Tricep Pushdown" to "cable pushdown",
        "Skull Crushers" to "barbell lying triceps extension skull crusher",
        "Overhead Tricep Extension" to "dumbbell overhead triceps extension",
        "Close-Grip Bench Press" to "close grip barbell bench press",
        "Tricep Dip" to "triceps dip",
        "Cable Overhead Extension" to "cable overhead triceps extension",
        "Barbell Squat" to "barbell full squat",
        "Leg Press" to "leg press",
        "Romanian Deadlift" to "barbell romanian deadlift",
        "Leg Extension" to "leg extension",
        "Leg Curl" to "leg curl",
        "Bulgarian Split Squat" to "dumbbell single leg split squat",
        "Front Squat" to "barbell front squat",
        "Hack Squat" to "sled hack squat",
        "Walking Lunge" to "dumbbell lunge",
        "Hip Thrust" to "barbell hip thrust",
        "Glute Bridge" to "barbell glute bridge",
        "Cable Kickback" to "cable kickback",
        "Sumo Deadlift" to "barbell sumo deadlift",
        "Plank" to "plank",
        "Cable Crunch" to "cable crunch",
        "Hanging Leg Raise" to "hanging leg raise",
        "Ab Wheel Rollout" to "wheel rollout",
        "Russian Twist" to "russian twist",
        "Standing Calf Raise" to "standing calf raise",
        "Seated Calf Raise" to "seated calf raise",
        "Wrist Curl" to "dumbbell wrist curl",
        "Farmer's Walk" to "farmer walk",
    )

    suspend fun getExerciseMedia(exerciseName: String): ExerciseMedia? {
        cache[exerciseName]?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val searchTerm = searchTermOverrides[exerciseName] ?: exerciseName.lowercase()
                val media = searchExercise(searchTerm)
                cache[exerciseName] = media
                media
            } catch (e: Exception) {
                null
            }
        }
    }

    // For backward compatibility
    suspend fun getExerciseImages(exerciseName: String): List<String> {
        val media = getExerciseMedia(exerciseName)
        return if (media != null) listOf(media.gifUrl) else emptyList()
    }

    private fun searchExercise(term: String): ExerciseMedia? {
        try {
            val encoded = URLEncoder.encode(term, "UTF-8")
            val url = URL("$BASE_URL/exercises?search=$encoded&limit=1")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val data = json.optJSONObject("data")
                val exercises = data?.optJSONArray("exercises") ?: data?.optJSONArray("results")

                // Handle different response formats
                val exerciseArray = exercises ?: run {
                    // Try parsing as direct array
                    try { JSONArray(response) } catch (_: Exception) { null }
                }

                if (exerciseArray != null && exerciseArray.length() > 0) {
                    val exercise = exerciseArray.getJSONObject(0)
                    return parseExercise(exercise)
                }

                // Try the direct object format
                if (data != null && data.has("gifUrl")) {
                    return parseExercise(data)
                }
            }
        } catch (_: Exception) {}

        // Fallback: try wger API
        return searchWger(term)
    }

    private fun parseExercise(json: JSONObject): ExerciseMedia? {
        val gifUrl = json.optString("gifUrl", "")
        if (gifUrl.isEmpty()) return null

        val secondaryMuscles = mutableListOf<String>()
        json.optJSONArray("secondaryMuscles")?.let { arr ->
            for (i in 0 until arr.length()) {
                secondaryMuscles.add(arr.getString(i))
            }
        }

        return ExerciseMedia(
            gifUrl = gifUrl,
            name = json.optString("name", ""),
            target = json.optString("target", ""),
            secondaryMuscles = secondaryMuscles
        )
    }

    private fun searchWger(term: String): ExerciseMedia? {
        try {
            val encoded = term.replace(" ", "+")
            val url = URL("https://wger.de/api/v2/exercise/search/?term=$encoded&language=english&format=json")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val suggestions = json.optJSONArray("suggestions")
                if (suggestions != null && suggestions.length() > 0) {
                    val data = suggestions.getJSONObject(0).optJSONObject("data")
                    val baseId = data?.optInt("base_id", -1) ?: return null
                    if (baseId > 0) {
                        val images = fetchWgerImages(baseId)
                        if (images.isNotEmpty()) {
                            return ExerciseMedia(
                                gifUrl = images.first(),
                                name = term,
                                target = "",
                                secondaryMuscles = emptyList()
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun fetchWgerImages(exerciseBaseId: Int): List<String> {
        try {
            val url = URL("https://wger.de/api/v2/exerciseimage/?exercise_base=$exerciseBaseId&format=json&limit=2")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val results = json.optJSONArray("results") ?: return emptyList()
                return (0 until minOf(results.length(), 2)).mapNotNull { i ->
                    val imageUrl = results.getJSONObject(i).optString("image", "")
                    imageUrl.ifEmpty { null }
                }
            }
        } catch (_: Exception) {}
        return emptyList()
    }
}
