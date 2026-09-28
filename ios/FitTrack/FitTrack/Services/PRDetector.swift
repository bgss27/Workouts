import Foundation
import SwiftData

/// One PR hit during a workout — used to populate the celebration screen.
/// Mirrors the Android `PRResult`. 1RM values are kg internally; the UI
/// converts to the user's display unit at render time.
struct PRResult: Identifiable {
    let id = UUID()
    let exerciseName: String
    let newBest1RM: Double
    let previousBest1RM: Double
    var delta: Double { newBest1RM - previousBest1RM }
}

/// Find all 1RM personal records hit during [workout]. Uses Epley
/// (`weight * (1 + reps/30)`) — same formula as the Android side.
///
/// Filters:
///   * Skips first-time exercises (no prior best to compare against).
///   * Requires delta > 0.5 kg so float-precision noise doesn't trigger
///     bogus celebrations.
enum PRDetector {
    static let minDelta: Double = 0.5

    static func detectPRs(workout: Workout, context: ModelContext) -> [PRResult] {
        // Best 1RM per exercise in the finished workout.
        var bestThisWorkout: [PersistentIdentifier: (name: String, best: Double)] = [:]
        for we in workout.exercises {
            guard let exercise = we.exercise else { continue }
            for set in we.sets where !set.isWarmup && set.reps > 0 && set.weightKg > 0 {
                let oneRm = set.weightKg * (1.0 + Double(set.reps) / 30.0)
                let existing = bestThisWorkout[exercise.persistentModelID]?.best ?? 0
                if oneRm > existing {
                    bestThisWorkout[exercise.persistentModelID] = (exercise.name, oneRm)
                }
            }
        }
        guard !bestThisWorkout.isEmpty else { return [] }

        // Fetch all completed workouts (we'll exclude the current one from
        // the "previous best" calculation per-exercise).
        let priorDescriptor = FetchDescriptor<Workout>(
            predicate: #Predicate { $0.endTime != nil }
        )
        let allCompleted = (try? context.fetch(priorDescriptor)) ?? []
        let workoutId = workout.persistentModelID

        var results: [PRResult] = []
        for (exId, (name, newBest)) in bestThisWorkout {
            var previousBest = 0.0
            for w in allCompleted where w.persistentModelID != workoutId {
                for we in w.exercises where we.exercise?.persistentModelID == exId {
                    for set in we.sets where !set.isWarmup && set.reps > 0 && set.weightKg > 0 {
                        let oneRm = set.weightKg * (1.0 + Double(set.reps) / 30.0)
                        if oneRm > previousBest { previousBest = oneRm }
                    }
                }
            }
            if previousBest > 0 && newBest - previousBest > minDelta {
                results.append(PRResult(
                    exerciseName: name,
                    newBest1RM: newBest,
                    previousBest1RM: previousBest
                ))
            }
        }
        return results.sorted { $0.delta > $1.delta }
    }
}
