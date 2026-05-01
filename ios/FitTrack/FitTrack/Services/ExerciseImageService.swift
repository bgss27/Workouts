import Foundation

/**
 * Provides exercise demonstration images from the free-exercise-db
 * (github.com/yuhonas/free-exercise-db) - open source, no API key needed.
 *
 * Each exercise has 2 images: starting position (0.jpg) and end position (1.jpg).
 */
struct ExerciseImageService {
    static let base = "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises"

    private static let imageMap: [String: String] = [
        // Chest
        "Barbell Bench Press": "Barbell_Bench_Press_-_Medium_Grip",
        "Incline Dumbbell Press": "Incline_Dumbbell_Press",
        "Dumbbell Bench Press": "Dumbbell_Bench_Press",
        "Cable Fly": "Flat_Bench_Cable_Flyes",
        "Chest Dip": "Dips_-_Chest_Version",
        "Push-Up": "Close-Grip_Push-Up_off_of_a_Dumbbell",
        "Incline Barbell Press": "Barbell_Incline_Bench_Press_-_Medium_Grip",
        "Pec Deck Machine": "Butterfly",
        // Back
        "Barbell Row": "Bent_Over_Barbell_Row",
        "Pull-Up": "Pullups",
        "Lat Pulldown": "Wide-Grip_Lat_Pulldown",
        "Seated Cable Row": "Seated_Cable_Rows",
        "Dumbbell Row": "One-Arm_Dumbbell_Row",
        "T-Bar Row": "T-Bar_Row_with_Handle",
        "Face Pull": "Face_Pull",
        "Deadlift": "Barbell_Deadlift",
        // Shoulders
        "Overhead Press": "Standing_Military_Press",
        "Lateral Raise": "Side_Lateral_Raise",
        "Front Raise": "Front_Dumbbell_Raise",
        "Rear Delt Fly": "Seated_Bent-Over_Rear_Delt_Raise",
        "Arnold Press": "Arnold_Dumbbell_Press",
        "Dumbbell Shoulder Press": "Dumbbell_Shoulder_Press",
        "Upright Row": "Upright_Barbell_Row",
        // Biceps
        "Barbell Curl": "Barbell_Curl",
        "Dumbbell Curl": "Dumbbell_Bicep_Curl",
        "Hammer Curl": "Alternate_Hammer_Curl",
        "Preacher Curl": "Preacher_Curl",
        "Incline Dumbbell Curl": "Incline_Dumbbell_Curl",
        "Cable Curl": "Standing_Biceps_Cable_Curl",
        "Concentration Curl": "Concentration_Curls",
        // Triceps
        "Tricep Pushdown": "Triceps_Pushdown",
        "Skull Crushers": "Lying_Triceps_Press",
        "Overhead Tricep Extension": "Dumbbell_One-Arm_Triceps_Extension",
        "Close-Grip Bench Press": "Close-Grip_Barbell_Bench_Press",
        "Tricep Dip": "Tricep_Dumbbell_Kickback",
        "Cable Overhead Extension": "Lying_Cable_Curl",
        // Legs
        "Barbell Squat": "Barbell_Squat",
        "Leg Press": "Leg_Press",
        "Romanian Deadlift": "Romanian_Deadlift",
        "Leg Extension": "Leg_Extensions",
        "Leg Curl": "Lying_Leg_Curls",
        "Bulgarian Split Squat": "Single_Leg_Push-off",
        "Front Squat": "Front_Barbell_Squat",
        "Hack Squat": "Hack_Squat",
        "Walking Lunge": "Dumbbell_Lunges",
        // Glutes
        "Hip Thrust": "Barbell_Hip_Thrust",
        "Glute Bridge": "Barbell_Glute_Bridge",
        "Cable Kickback": "Glute_Kickback",
        "Sumo Deadlift": "Sumo_Deadlift",
        // Abs
        "Plank": "Plank",
        "Cable Crunch": "Cable_Crunch",
        "Hanging Leg Raise": "Hanging_Leg_Raise",
        "Ab Wheel Rollout": "Ab_Roller",
        "Russian Twist": "Russian_Twist",
        // Calves
        "Standing Calf Raise": "Standing_Calf_Raises",
        "Seated Calf Raise": "Seated_Calf_Raise",
        // Forearms
        "Wrist Curl": "Palms-Down_Wrist_Curl_Over_A_Bench",
        "Reverse Wrist Curl": "Palms-Up_Barbell_Wrist_Curl_Over_A_Bench",
        "Farmer's Walk": "Farmer's_Walk",
    ]

    static func getImageURLs(for exerciseName: String) -> [URL] {
        guard let folder = imageMap[exerciseName] else { return [] }
        return [
            URL(string: "\(base)/\(folder)/0.jpg")!,
            URL(string: "\(base)/\(folder)/1.jpg")!
        ]
    }
}
