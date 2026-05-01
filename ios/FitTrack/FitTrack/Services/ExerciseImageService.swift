import Foundation
import SwiftUI

@MainActor
class ExerciseImageService: ObservableObject {
    static let shared = ExerciseImageService()

    @Published var images: [String: [URL]] = [:]
    @Published var loadingStates: [String: Bool] = [:]

    private let baseURL = "https://wger.de/api/v2"

    private let knownExerciseIds: [String: Int] = [
        "Barbell Bench Press": 192,
        "Incline Dumbbell Press": 312,
        "Dumbbell Bench Press": 97,
        "Cable Fly": 122,
        "Chest Dip": 82,
        "Push-Up": 182,
        "Pec Deck Machine": 204,
        "Barbell Row": 340,
        "Pull-Up": 107,
        "Lat Pulldown": 212,
        "Seated Cable Row": 362,
        "Dumbbell Row": 81,
        "Deadlift": 105,
        "Face Pull": 309,
        "Overhead Press": 119,
        "Lateral Raise": 148,
        "Front Raise": 233,
        "Rear Delt Fly": 327,
        "Arnold Press": 228,
        "Barbell Curl": 74,
        "Hammer Curl": 301,
        "Preacher Curl": 100,
        "Concentration Curl": 288,
        "Tricep Pushdown": 93,
        "Skull Crushers": 344,
        "Close-Grip Bench Press": 217,
        "Tricep Dip": 82,
        "Barbell Squat": 111,
        "Leg Press": 310,
        "Romanian Deadlift": 116,
        "Leg Extension": 177,
        "Leg Curl": 155,
        "Bulgarian Split Squat": 278,
        "Front Squat": 191,
        "Hip Thrust": 413,
        "Plank": 238,
        "Cable Crunch": 91,
        "Hanging Leg Raise": 126,
        "Standing Calf Raise": 104,
        "Seated Calf Raise": 103,
    ]

    func loadImages(for exerciseName: String) async {
        guard images[exerciseName] == nil else { return }
        loadingStates[exerciseName] = true

        do {
            guard let exerciseId = knownExerciseIds[exerciseName] ?? (try await searchExerciseId(exerciseName)) else {
                loadingStates[exerciseName] = false
                images[exerciseName] = []
                return
            }

            let fetched = try await fetchImages(exerciseBaseId: exerciseId)
            images[exerciseName] = fetched
        } catch {
            images[exerciseName] = []
        }
        loadingStates[exerciseName] = false
    }

    private func searchExerciseId(_ name: String) async throws -> Int? {
        let encoded = name.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? name
        guard let url = URL(string: "\(baseURL)/exercise/search/?term=\(encoded)&language=english&format=json") else { return nil }

        let (data, _) = try await URLSession.shared.data(from: url)
        let json = try JSONSerialization.jsonObject(with: data) as? [String: Any]
        let suggestions = json?["suggestions"] as? [[String: Any]]
        let first = suggestions?.first
        let dataObj = first?["data"] as? [String: Any]
        return dataObj?["base_id"] as? Int
    }

    private func fetchImages(exerciseBaseId: Int) async throws -> [URL] {
        guard let url = URL(string: "\(baseURL)/exerciseimage/?exercise_base=\(exerciseBaseId)&format=json") else { return [] }

        let (data, _) = try await URLSession.shared.data(from: url)
        let json = try JSONSerialization.jsonObject(with: data) as? [String: Any]
        let results = json?["results"] as? [[String: Any]] ?? []

        return results.compactMap { item -> URL? in
            guard let imageStr = item["image"] as? String else { return nil }
            return URL(string: imageStr)
        }
    }
}
