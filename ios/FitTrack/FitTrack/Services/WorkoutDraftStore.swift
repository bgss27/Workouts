import Foundation
import SwiftData

/// Crash / background-kill insurance for the in-progress workout.
///
/// Sets are only written to SwiftData when the user taps Finish, so if iOS
/// evicts the app while the phone is locked between sets, everything typed so
/// far would vanish. `ActiveWorkoutView` mirrors its entry state here after
/// every change and Home offers to resume it on next launch while the
/// matching `Workout` row is still open (`endTime == nil`).
/// Mirrors Android's `WorkoutDraftStore`.
struct WorkoutDraft: Codable {
    struct Set: Codable {
        var weight: String
        var reps: String
        var isWarmup: Bool
        var rpe: Int?
        var isCompleted: Bool
    }

    struct Exercise: Codable {
        var exerciseID: PersistentIdentifier
        /// Fallback lookup when the identifier can't be resolved (e.g. the
        /// catalog was re-seeded between launches).
        var exerciseName: String
        var supersetGroup: Int?
        var isFinished: Bool
        var sets: [Set]
    }

    var workoutID: PersistentIdentifier
    var startedAt: Date
    var weightUnit: String
    var exercises: [Exercise]
}

enum WorkoutDraftStore {
    private static let key = "fittrack_workout_draft"

    static func save(_ draft: WorkoutDraft) {
        if let data = try? JSONEncoder().encode(draft) {
            UserDefaults.standard.set(data, forKey: key)
        }
    }

    static func load() -> WorkoutDraft? {
        guard let data = UserDefaults.standard.data(forKey: key) else { return nil }
        return try? JSONDecoder().decode(WorkoutDraft.self, from: data)
    }

    static var hasDraft: Bool { UserDefaults.standard.data(forKey: key) != nil }

    static func clear() {
        UserDefaults.standard.removeObject(forKey: key)
    }

    /// The still-open workout this draft belongs to, or nil if it was finished
    /// or deleted since (in which case the draft is stale and gets dropped).
    static func resolveWorkout(_ draft: WorkoutDraft, in context: ModelContext) -> Workout? {
        guard let workout = context.model(for: draft.workoutID) as? Workout,
              workout.endTime == nil else {
            clear()
            return nil
        }
        return workout
    }
}
