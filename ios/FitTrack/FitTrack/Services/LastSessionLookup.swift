import Foundation
import SwiftData

/// One set from a previous session, in storage units (kg).
struct PreviousSet: Equatable, Codable {
    let weightKg: Double
    let reps: Int
    var durationSeconds: Int? = nil
    var distanceMeters: Double? = nil
}

/// "What did I do last time?" for the active workout's ghost values.
/// Mirrors Android's `LastSessionLookup`.
enum LastSessionLookup {
    /// Non-warm-up sets from the most recent completed workout that included
    /// `exercise`, in set order. Empty when the exercise has never been logged.
    static func previousSets(for exercise: Exercise, in context: ModelContext) -> [PreviousSet] {
        let id = exercise.persistentModelID
        var descriptor = FetchDescriptor<Workout>(
            predicate: #Predicate { $0.endTime != nil },
            sortBy: [SortDescriptor(\.startTime, order: .reverse)]
        )
        // Bound the scan; an exercise absent from the last 200 sessions has
        // no useful "last time" anyway.
        descriptor.fetchLimit = 200
        let workouts = (try? context.fetch(descriptor)) ?? []
        for workout in workouts {
            guard let we = workout.exercises.first(where: { $0.exercise?.persistentModelID == id }) else { continue }
            let sets = we.sets.filter { !$0.isWarmup }.sorted { $0.setNumber < $1.setNumber }
            if !sets.isEmpty {
                return sets.map {
                    PreviousSet(weightKg: $0.weightKg, reps: $0.reps,
                                durationSeconds: $0.durationSeconds, distanceMeters: $0.distanceMeters)
                }
            }
        }
        return []
    }

    /// Last up to `limit` completed sessions (newest first) for the progression
    /// advisor: each session's non-warm-up sets in set order.
    static func recentSessions(for exercise: Exercise, in context: ModelContext, limit: Int = 3) -> [[WorkoutSet]] {
        let id = exercise.persistentModelID
        var descriptor = FetchDescriptor<Workout>(
            predicate: #Predicate { $0.endTime != nil },
            sortBy: [SortDescriptor(\.startTime, order: .reverse)]
        )
        descriptor.fetchLimit = 200
        let workouts = (try? context.fetch(descriptor)) ?? []
        var out: [[WorkoutSet]] = []
        for workout in workouts {
            guard let we = workout.exercises.first(where: { $0.exercise?.persistentModelID == id }) else { continue }
            let sets = we.sets.filter { !$0.isWarmup }.sorted { $0.setNumber < $1.setNumber }
            if !sets.isEmpty { out.append(sets) }
            if out.count == limit { break }
        }
        return out
    }

    /// Weight in the display unit as the text the set field would hold.
    static func weightText(_ weightKg: Double, unit: String) -> String {
        SetFormat.weightText(weightKg, unit: unit)
    }

    /// "Prev 60 × 8", "Prev 1:30", "Prev 40 × 1:00", "Prev 5.2 km · 28:10", "Prev 40 × 30 m".
    static func hint(_ set: PreviousSet, mode: TrackingMode, unit: String) -> String {
        let weight = weightText(set.weightKg, unit: unit)
        let duration = DurationFormat.format(set.durationSeconds ?? 0)
        let distance = DistanceUnits.format(set.distanceMeters ?? 0, unit: DistanceUnits.unit(for: mode, weightUnit: unit))
        switch mode {
        case .weightReps: return "Prev \(weight) × \(set.reps)"
        case .duration: return "Prev \(duration)"
        case .weightDuration: return "Prev \(weight) × \(duration)"
        case .distanceDuration: return "Prev \(distance) · \(duration)"
        case .weightDistance: return "Prev \(weight) × \(distance)"
        }
    }
}
