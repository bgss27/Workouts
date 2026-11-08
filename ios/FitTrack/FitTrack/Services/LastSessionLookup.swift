import Foundation
import SwiftData

/// One set from a previous session, in storage units (kg).
struct PreviousSet: Equatable, Codable {
    let weightKg: Double
    let reps: Int
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
                return sets.map { PreviousSet(weightKg: $0.weightKg, reps: $0.reps) }
            }
        }
        return []
    }

    /// Weight in the display unit as the text the set field would hold.
    static func weightText(_ weightKg: Double, unit: String) -> String {
        let value = unit == "lbs" ? weightKg * 2.2046226218 : weightKg
        // Snap to the nearest 0.5 so lbs conversions don't show 132.277.
        let snapped = (value * 2).rounded() / 2
        return snapped == snapped.rounded() ? String(Int(snapped)) : String(format: "%.1f", snapped)
    }

    static func hint(_ set: PreviousSet, unit: String) -> String {
        "Prev \(weightText(set.weightKg, unit: unit)) × \(set.reps)"
    }
}
