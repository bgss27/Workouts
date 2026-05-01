package com.fittrack.app.data.repository

/**
 * Provides exercise demonstration images from the free-exercise-db
 * (github.com/yuhonas/free-exercise-db) - open source, no API key needed.
 *
 * Each exercise has 2 images: starting position (0.jpg) and end position (1.jpg).
 * Images are served directly from GitHub's CDN.
 */
object ExerciseImageRepository {
    private const val BASE = "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises"

    // Our exercise name -> free-exercise-db folder name
    private val imageMap = mapOf(
        // Chest
        "Barbell Bench Press" to "Barbell_Bench_Press_-_Medium_Grip",
        "Incline Dumbbell Press" to "Incline_Dumbbell_Press",
        "Dumbbell Bench Press" to "Dumbbell_Bench_Press",
        "Cable Fly" to "Flat_Bench_Cable_Flyes",
        "Chest Dip" to "Dips_-_Chest_Version",
        "Push-Up" to "Close-Grip_Push-Up_off_of_a_Dumbbell",
        "Incline Barbell Press" to "Barbell_Incline_Bench_Press_-_Medium_Grip",
        "Pec Deck Machine" to "Butterfly",
        // Back
        "Barbell Row" to "Bent_Over_Barbell_Row",
        "Pull-Up" to "Pullups",
        "Lat Pulldown" to "Wide-Grip_Lat_Pulldown",
        "Seated Cable Row" to "Seated_Cable_Rows",
        "Dumbbell Row" to "One-Arm_Dumbbell_Row",
        "T-Bar Row" to "T-Bar_Row_with_Handle",
        "Face Pull" to "Face_Pull",
        "Deadlift" to "Barbell_Deadlift",
        // Shoulders
        "Overhead Press" to "Standing_Military_Press",
        "Lateral Raise" to "Side_Lateral_Raise",
        "Front Raise" to "Front_Dumbbell_Raise",
        "Rear Delt Fly" to "Seated_Bent-Over_Rear_Delt_Raise",
        "Arnold Press" to "Arnold_Dumbbell_Press",
        "Dumbbell Shoulder Press" to "Dumbbell_Shoulder_Press",
        "Upright Row" to "Upright_Barbell_Row",
        // Biceps
        "Barbell Curl" to "Barbell_Curl",
        "Dumbbell Curl" to "Dumbbell_Bicep_Curl",
        "Hammer Curl" to "Alternate_Hammer_Curl",
        "Preacher Curl" to "Preacher_Curl",
        "Incline Dumbbell Curl" to "Incline_Dumbbell_Curl",
        "Cable Curl" to "Standing_Biceps_Cable_Curl",
        "Concentration Curl" to "Concentration_Curls",
        // Triceps
        "Tricep Pushdown" to "Triceps_Pushdown",
        "Skull Crushers" to "Lying_Triceps_Press",
        "Overhead Tricep Extension" to "Dumbbell_One-Arm_Triceps_Extension",
        "Close-Grip Bench Press" to "Close-Grip_Barbell_Bench_Press",
        "Tricep Dip" to "Tricep_Dumbbell_Kickback",
        "Cable Overhead Extension" to "Lying_Cable_Curl",
        // Legs
        "Barbell Squat" to "Barbell_Squat",
        "Leg Press" to "Leg_Press",
        "Romanian Deadlift" to "Romanian_Deadlift",
        "Leg Extension" to "Leg_Extensions",
        "Leg Curl" to "Lying_Leg_Curls",
        "Bulgarian Split Squat" to "Single_Leg_Push-off",
        "Front Squat" to "Front_Barbell_Squat",
        "Hack Squat" to "Hack_Squat",
        "Walking Lunge" to "Dumbbell_Lunges",
        // Glutes
        "Hip Thrust" to "Barbell_Hip_Thrust",
        "Glute Bridge" to "Barbell_Glute_Bridge",
        "Cable Kickback" to "Glute_Kickback",
        "Sumo Deadlift" to "Sumo_Deadlift",
        // Abs
        "Plank" to "Plank",
        "Cable Crunch" to "Cable_Crunch",
        "Hanging Leg Raise" to "Hanging_Leg_Raise",
        "Ab Wheel Rollout" to "Ab_Roller",
        "Russian Twist" to "Russian_Twist",
        // Calves
        "Standing Calf Raise" to "Standing_Calf_Raises",
        "Seated Calf Raise" to "Seated_Calf_Raise",
        // Forearms
        "Wrist Curl" to "Palms-Down_Wrist_Curl_Over_A_Bench",
        "Reverse Wrist Curl" to "Palms-Up_Barbell_Wrist_Curl_Over_A_Bench",
        "Farmer's Walk" to "Farmer's_Walk",
    )

    /**
     * Returns image URLs for an exercise (start position + end position).
     * These are static GitHub CDN URLs - no API calls needed.
     */
    fun getExerciseImages(exerciseName: String): List<String> {
        val folder = imageMap[exerciseName] ?: return emptyList()
        return listOf(
            "$BASE/$folder/0.jpg",
            "$BASE/$folder/1.jpg"
        )
    }
}
