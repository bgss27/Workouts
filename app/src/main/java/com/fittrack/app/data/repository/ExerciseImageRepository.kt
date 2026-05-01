package com.fittrack.app.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ExerciseImageRepository {
    private const val BASE_URL = "https://wger.de/api/v2"
    private val imageCache = mutableMapOf<String, List<String>>()
    private val exerciseIdCache = mutableMapOf<String, Int>()

    // Map our exercise names to wger exercise base IDs
    // These are the wger database IDs for common exercises
    private val knownExerciseIds = mapOf(
        "Barbell Bench Press" to 192,
        "Incline Dumbbell Press" to 312,
        "Dumbbell Bench Press" to 97,
        "Cable Fly" to 122,
        "Chest Dip" to 82,
        "Push-Up" to 182,
        "Pec Deck Machine" to 204,
        "Barbell Row" to 340,
        "Pull-Up" to 107,
        "Lat Pulldown" to 212,
        "Seated Cable Row" to 362,
        "Dumbbell Row" to 81,
        "Deadlift" to 105,
        "Face Pull" to 309,
        "Overhead Press" to 119,
        "Lateral Raise" to 148,
        "Front Raise" to 233,
        "Rear Delt Fly" to 327,
        "Arnold Press" to 228,
        "Barbell Curl" to 74,
        "Dumbbell Curl" to 81,
        "Hammer Curl" to 301,
        "Preacher Curl" to 100,
        "Concentration Curl" to 288,
        "Tricep Pushdown" to 93,
        "Skull Crushers" to 344,
        "Close-Grip Bench Press" to 217,
        "Tricep Dip" to 82,
        "Barbell Squat" to 111,
        "Leg Press" to 310,
        "Romanian Deadlift" to 116,
        "Leg Extension" to 177,
        "Leg Curl" to 155,
        "Bulgarian Split Squat" to 278,
        "Front Squat" to 191,
        "Hip Thrust" to 413,
        "Plank" to 238,
        "Cable Crunch" to 91,
        "Hanging Leg Raise" to 126,
        "Standing Calf Raise" to 104,
        "Seated Calf Raise" to 103,
    )

    suspend fun getExerciseImages(exerciseName: String): List<String> {
        // Check memory cache first
        imageCache[exerciseName]?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val exerciseId = knownExerciseIds[exerciseName]
                    ?: searchExerciseId(exerciseName)
                    ?: return@withContext emptyList()

                val images = fetchImages(exerciseId)
                if (images.isNotEmpty()) {
                    imageCache[exerciseName] = images
                }
                images
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    private fun searchExerciseId(name: String): Int? {
        exerciseIdCache[name]?.let { return it }

        try {
            val searchName = name.replace(" ", "+")
            val url = URL("$BASE_URL/exercise/search/?term=$searchName&language=english&format=json")
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
                    val first = suggestions.getJSONObject(0)
                    val data = first.optJSONObject("data")
                    val id = data?.optInt("base_id", -1) ?: -1
                    if (id > 0) {
                        exerciseIdCache[name] = id
                        return id
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun fetchImages(exerciseBaseId: Int): List<String> {
        try {
            val url = URL("$BASE_URL/exerciseimage/?exercise_base=$exerciseBaseId&format=json")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val results = json.optJSONArray("results") ?: return emptyList()

                val images = mutableListOf<String>()
                for (i in 0 until results.length()) {
                    val item = results.getJSONObject(i)
                    val imageUrl = item.optString("image", "")
                    if (imageUrl.isNotEmpty()) {
                        images.add(imageUrl)
                    }
                }
                return images
            }
        } catch (_: Exception) {}
        return emptyList()
    }
}
