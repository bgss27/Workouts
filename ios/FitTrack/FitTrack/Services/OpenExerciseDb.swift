import Foundation

/// Loads the bundled free-exercise-db dataset (yuhonas/free-exercise-db, public
/// domain / Unlicense, ~870 entries) on first access and exposes a lookup of
/// step-by-step instructions per exercise.
///
/// Mirrors the Android `OpenExerciseDb`. Falls back to `ExerciseInstructions`
/// for exercises the open dataset doesn't cover (the four no-equipment biceps
/// moves, the wger-sourced back work, novel bodyweight variants, etc.).
///
/// **One-time Xcode setup required:** drag `free_exercise_db.json` into Xcode
/// → target FitTrack → "Copy Bundle Resources" so it actually ships. Without
/// that step `Bundle.main.url(forResource:)` returns nil and we return only
/// the hand-written fallbacks.
enum OpenExerciseDb {

    /// App name → dataset id (mirrors the dataset's image folder name).
    /// Curated, takes precedence over name/fuzzy matching.
    private static let nameToDatasetId: [String: String] = [
        // Existing exercises (mirrors `ExerciseImageService` mapping)
        "Barbell Bench Press": "Barbell_Bench_Press_-_Medium_Grip",
        "Incline Dumbbell Press": "Incline_Dumbbell_Press",
        "Dumbbell Bench Press": "Dumbbell_Bench_Press",
        "Cable Fly": "Flat_Bench_Cable_Flyes",
        "Chest Dip": "Dips_-_Chest_Version",
        "Push-Up": "Pushups",
        "Incline Barbell Press": "Barbell_Incline_Bench_Press_-_Medium_Grip",
        "Pec Deck Machine": "Butterfly",
        "Barbell Row": "Bent_Over_Barbell_Row",
        "Pull-Up": "Pullups",
        "Lat Pulldown": "Wide-Grip_Lat_Pulldown",
        "Seated Cable Row": "Seated_Cable_Rows",
        "Dumbbell Row": "One-Arm_Dumbbell_Row",
        "T-Bar Row": "T-Bar_Row_with_Handle",
        "Face Pull": "Face_Pull",
        "Deadlift": "Barbell_Deadlift",
        "Overhead Press": "Standing_Military_Press",
        "Lateral Raise": "Side_Lateral_Raise",
        "Front Raise": "Front_Dumbbell_Raise",
        "Rear Delt Fly": "Seated_Bent-Over_Rear_Delt_Raise",
        "Arnold Press": "Arnold_Dumbbell_Press",
        "Dumbbell Shoulder Press": "Dumbbell_Shoulder_Press",
        "Upright Row": "Upright_Barbell_Row",
        "Barbell Curl": "Barbell_Curl",
        "Dumbbell Curl": "Dumbbell_Bicep_Curl",
        "Hammer Curl": "Alternate_Hammer_Curl",
        "Preacher Curl": "Preacher_Curl",
        "Incline Dumbbell Curl": "Incline_Dumbbell_Curl",
        "Cable Curl": "Standing_Biceps_Cable_Curl",
        "Concentration Curl": "Concentration_Curls",
        "Tricep Pushdown": "Triceps_Pushdown",
        "Skull Crushers": "Lying_Triceps_Press",
        "Overhead Tricep Extension": "Dumbbell_One-Arm_Triceps_Extension",
        "Close-Grip Bench Press": "Close-Grip_Barbell_Bench_Press",
        "Cable Overhead Extension": "Lying_Cable_Curl",
        "Barbell Squat": "Barbell_Squat",
        "Leg Press": "Leg_Press",
        "Romanian Deadlift": "Romanian_Deadlift",
        "Leg Extension": "Leg_Extensions",
        "Leg Curl": "Lying_Leg_Curls",
        "Bulgarian Split Squat": "Single_Leg_Push-off",
        "Front Squat": "Front_Barbell_Squat",
        "Hack Squat": "Hack_Squat",
        "Walking Lunge": "Dumbbell_Lunges",
        "Hip Thrust": "Barbell_Hip_Thrust",
        "Glute Bridge": "Barbell_Glute_Bridge",
        "Cable Kickback": "Glute_Kickback",
        "Sumo Deadlift": "Sumo_Deadlift",
        "Plank": "Plank",
        "Cable Crunch": "Cable_Crunch",
        "Hanging Leg Raise": "Hanging_Leg_Raise",
        "Ab Wheel Rollout": "Ab_Roller",
        "Russian Twist": "Russian_Twist",
        "Standing Calf Raise": "Standing_Calf_Raises",
        "Seated Calf Raise": "Seated_Calf_Raise",
        "Wrist Curl": "Palms-Down_Wrist_Curl_Over_A_Bench",
        "Reverse Wrist Curl": "Palms-Up_Barbell_Wrist_Curl_Over_A_Bench",
        "Farmer's Walk": "Farmer's_Walk",

        // Bodyweight additions
        "Wide Push-Up": "Push-Up_Wide",
        "Decline Push-Up": "Decline_Push-Up",
        "Incline Push-Up": "Incline_Push-Up",
        "Inverted Row": "Inverted_Row",
        "Superman Hold": "Superman",
        "Chin-Up": "Chin-Up",
        "Bodyweight Squat": "Bodyweight_Squat",
        "Mountain Climbers": "Mountain_Climbers",
        "Dead Bug": "Dead_Bug",
        "Single-Leg Glute Bridge": "Single_Leg_Glute_Bridge",
        "Lying Leg Raise": "Flat_Bench_Lying_Leg_Raise",
        "Tricep Dip": "Bench_Dips",
    ]

    /// Lazy cache built on first call. Keyed by both dataset `id` and `name`.
    private static let cache: [String: String] = {
        guard let url = Bundle.main.url(forResource: "free_exercise_db", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let arr = (try? JSONSerialization.jsonObject(with: data)) as? [[String: Any]]
        else {
            return [:]
        }
        var result: [String: String] = [:]
        result.reserveCapacity(arr.count * 2)
        for ex in arr {
            guard let instructions = ex["instructions"] as? [String], !instructions.isEmpty else { continue }
            let text = instructions.joined(separator: "\n\n")
            if let id = ex["id"] as? String, !id.isEmpty { result[id] = text }
            if let name = ex["name"] as? String, !name.isEmpty { result[name] = text }
        }
        return result
    }()

    static func instructions(for exerciseName: String) -> String? {
        // 1. Curated mapping
        if let id = nameToDatasetId[exerciseName], let text = cache[id] {
            return text
        }
        // 2. Exact name match (cache indexes both id and name)
        if let text = cache[exerciseName] { return text }
        // 3. Fuzzy id: spaces → underscores
        let fuzzy = exerciseName.replacingOccurrences(of: " ", with: "_")
        return cache[fuzzy]
    }
}
