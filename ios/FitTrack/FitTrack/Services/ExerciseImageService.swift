import Foundation

struct ExerciseMediaData {
    let gifUrl: URL
    let name: String
    let target: String
    let secondaryMuscles: [String]
}

@MainActor
class ExerciseImageService: ObservableObject {
    static let shared = ExerciseImageService()

    @Published var media: [String: ExerciseMediaData?] = [:]
    @Published var loadingStates: [String: Bool] = [:]

    private let baseURL = "https://exercisedb-api.vercel.app/api/v1"

    private let searchTermOverrides: [String: String] = [
        "Barbell Bench Press": "barbell bench press",
        "Incline Dumbbell Press": "incline dumbbell press",
        "Dumbbell Bench Press": "dumbbell bench press",
        "Cable Fly": "cable fly",
        "Chest Dip": "chest dip",
        "Push-Up": "push up",
        "Incline Barbell Press": "incline barbell bench press",
        "Pec Deck Machine": "pec deck",
        "Barbell Row": "barbell bent over row",
        "Pull-Up": "pull up",
        "Lat Pulldown": "lat pulldown",
        "Seated Cable Row": "seated cable row",
        "Dumbbell Row": "dumbbell bent over row",
        "T-Bar Row": "t bar bent over row",
        "Face Pull": "cable face pull",
        "Deadlift": "barbell deadlift",
        "Overhead Press": "barbell overhead press",
        "Lateral Raise": "dumbbell lateral raise",
        "Front Raise": "dumbbell front raise",
        "Rear Delt Fly": "dumbbell rear delt fly",
        "Arnold Press": "dumbbell arnold press",
        "Dumbbell Shoulder Press": "dumbbell shoulder press",
        "Upright Row": "barbell upright row",
        "Barbell Curl": "barbell curl",
        "Dumbbell Curl": "dumbbell curl",
        "Hammer Curl": "dumbbell hammer curl",
        "Preacher Curl": "barbell preacher curl",
        "Incline Dumbbell Curl": "dumbbell incline curl",
        "Cable Curl": "cable curl",
        "Concentration Curl": "dumbbell concentration curl",
        "Tricep Pushdown": "cable pushdown",
        "Skull Crushers": "barbell lying triceps extension skull crusher",
        "Overhead Tricep Extension": "dumbbell overhead triceps extension",
        "Close-Grip Bench Press": "close grip barbell bench press",
        "Tricep Dip": "triceps dip",
        "Cable Overhead Extension": "cable overhead triceps extension",
        "Barbell Squat": "barbell full squat",
        "Leg Press": "leg press",
        "Romanian Deadlift": "barbell romanian deadlift",
        "Leg Extension": "leg extension",
        "Leg Curl": "leg curl",
        "Bulgarian Split Squat": "dumbbell single leg split squat",
        "Front Squat": "barbell front squat",
        "Hack Squat": "sled hack squat",
        "Walking Lunge": "dumbbell lunge",
        "Hip Thrust": "barbell hip thrust",
        "Glute Bridge": "barbell glute bridge",
        "Cable Kickback": "cable kickback",
        "Sumo Deadlift": "barbell sumo deadlift",
        "Plank": "plank",
        "Cable Crunch": "cable crunch",
        "Hanging Leg Raise": "hanging leg raise",
        "Ab Wheel Rollout": "wheel rollout",
        "Russian Twist": "russian twist",
        "Standing Calf Raise": "standing calf raise",
        "Seated Calf Raise": "seated calf raise",
        "Wrist Curl": "dumbbell wrist curl",
        "Farmer's Walk": "farmer walk",
    ]

    func loadMedia(for exerciseName: String) async {
        guard media[exerciseName] == nil else { return }
        loadingStates[exerciseName] = true

        let searchTerm = searchTermOverrides[exerciseName] ?? exerciseName.lowercased()
        let result = await searchExercise(term: searchTerm)
        media[exerciseName] = result
        loadingStates[exerciseName] = false
    }

    private func searchExercise(term: String) async -> ExerciseMediaData? {
        // Try ExerciseDB first
        if let result = await searchExerciseDB(term: term) {
            return result
        }
        // Fallback to wger
        return await searchWger(term: term)
    }

    private func searchExerciseDB(term: String) async -> ExerciseMediaData? {
        guard let encoded = term.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed),
              let url = URL(string: "\(baseURL)/exercises?search=\(encoded)&limit=1") else { return nil }

        do {
            let (data, _) = try await URLSession.shared.data(from: url)
            let json = try JSONSerialization.jsonObject(with: data) as? [String: Any]

            // Handle nested data format
            let dataObj = json?["data"] as? [String: Any]
            let exercises = dataObj?["exercises"] as? [[String: Any]]
                ?? dataObj?["results"] as? [[String: Any]]
                ?? (try? JSONSerialization.jsonObject(with: data) as? [[String: Any]])

            guard let first = exercises?.first,
                  let gifUrlStr = first["gifUrl"] as? String,
                  let gifUrl = URL(string: gifUrlStr) else { return nil }

            let secondaryMuscles = (first["secondaryMuscles"] as? [String]) ?? []

            return ExerciseMediaData(
                gifUrl: gifUrl,
                name: (first["name"] as? String) ?? term,
                target: (first["target"] as? String) ?? "",
                secondaryMuscles: secondaryMuscles
            )
        } catch {
            return nil
        }
    }

    private func searchWger(term: String) async -> ExerciseMediaData? {
        let encoded = term.replacingOccurrences(of: " ", with: "+")
        guard let searchUrl = URL(string: "https://wger.de/api/v2/exercise/search/?term=\(encoded)&language=english&format=json") else { return nil }

        do {
            let (data, _) = try await URLSession.shared.data(from: searchUrl)
            let json = try JSONSerialization.jsonObject(with: data) as? [String: Any]
            guard let suggestions = json?["suggestions"] as? [[String: Any]],
                  let first = suggestions.first,
                  let dataObj = first["data"] as? [String: Any],
                  let baseId = dataObj["base_id"] as? Int else { return nil }

            guard let imgUrl = URL(string: "https://wger.de/api/v2/exerciseimage/?exercise_base=\(baseId)&format=json&limit=1") else { return nil }
            let (imgData, _) = try await URLSession.shared.data(from: imgUrl)
            let imgJson = try JSONSerialization.jsonObject(with: imgData) as? [String: Any]
            guard let results = imgJson?["results"] as? [[String: Any]],
                  let firstImg = results.first,
                  let imageStr = firstImg["image"] as? String,
                  let imageUrl = URL(string: imageStr) else { return nil }

            return ExerciseMediaData(gifUrl: imageUrl, name: term, target: "", secondaryMuscles: [])
        } catch {
            return nil
        }
    }
}
