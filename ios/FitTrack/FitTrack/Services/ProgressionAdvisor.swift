import Foundation

/// The next target for an exercise, derived from the last few sessions.
/// Mirrors Android's `ProgressionSuggestion`.
struct ProgressionSuggestion: Equatable {
    enum Kind: String { case increaseWeight = "INCREASE_WEIGHT", addReps = "ADD_REPS", hold = "HOLD", reduceWeight = "REDUCE_WEIGHT" }

    let kind: Kind
    let weightKg: Double
    let reps: Int
    let previousWeightKg: Double
    let previousMinReps: Int
    let reason: String

    /// "Target 62.5 kg × 8"; weight omitted for bodyweight work.
    func label(unit: String) -> String {
        if weightKg <= 0 { return "Target \(reps) reps" }
        return "Target \(SetFormat.weightText(weightKg, unit: unit)) \(unit) × \(reps)"
    }

    var glyph: String {
        switch kind {
        case .increaseWeight: return "arrow.up"
        case .addReps: return "plus"
        case .hold: return "equal"
        case .reduceWeight: return "arrow.down"
        }
    }
}

/// One past session's working sets, in storage units.
struct SessionSets {
    struct Set { let weightKg: Double; let reps: Int; let rpe: Int?; let setType: SetType }
    let sets: [Set]
}

/// Linear / double progression with an RPE guard. Pure function so both the
/// active workout and tests can call it. Mirrors Android's `ProgressionAdvisor`.
enum ProgressionAdvisor {
    static let prefKey = "progression_targets_enabled"

    /// Parse "8-12" → (8, 12), "5" → (5, 5). Anything else → 8–12.
    static func repTarget(from text: String?) -> (lo: Int, hi: Int) {
        guard let text else { return (8, 12) }
        let cleaned = text.replacingOccurrences(of: "–", with: "-").trimmingCharacters(in: .whitespaces)
        let parts = cleaned.split(separator: "-").map { Int($0.trimmingCharacters(in: .whitespaces)) }
        if parts.count == 2, let lo = parts[0], let hi = parts[1], lo > 0, hi >= lo { return (lo, hi) }
        if parts.count == 1, let n = parts[0], n > 0 { return (n, n) }
        return (8, 12)
    }

    /// `history` is newest first. Returns nil when there is nothing to go on.
    static func suggest(
        history: [SessionSets],
        repTarget target: (lo: Int, hi: Int),
        muscleGroup: MuscleGroup,
        equipment: Equipment,
        weightUnit: String
    ) -> ProgressionSuggestion? {
        guard let newest = history.first else { return nil }
        guard let analysis = analyze(newest) else { return nil }
        let isLbs = weightUnit == "lbs"
        let small = isLbs ? 5.0 : 2.5
        let lowerBody: Set<MuscleGroup> = [.legs, .glutes, .back]
        let heavyEquipment = equipment == .barbell || equipment == .machine
        let increment = (lowerBody.contains(muscleGroup) && heavyEquipment) ? small * 2 : small
        let weight = analysis.weight
        let bodyweight = weight <= 0
        let rpe = analysis.avgRpe

        func display(_ kg: Double) -> Double { isLbs ? kg * 2.2046226218 : kg }
        func storage(_ display: Double) -> Double { isLbs ? display * 0.45359237 : display }
        func snap(_ displayValue: Double) -> Double { (displayValue / small).rounded() * small }

        let allAtTop = analysis.minReps >= target.hi
        let allAtBottom = analysis.minReps >= target.lo
        let veryHard = (rpe ?? 0) >= 9.5

        // Back off when the previous session at this weight also missed / was very hard.
        if (!allAtBottom || veryHard), history.count >= 2, let prior = analyze(history[1]),
           abs(prior.weight - weight) < 0.01,
           (prior.minReps < target.lo || (prior.avgRpe ?? 0) >= 9.5), !bodyweight {
            let reduced = storage(snap(display(weight) * 0.9))
            return ProgressionSuggestion(
                kind: .reduceWeight, weightKg: reduced, reps: target.lo,
                previousWeightKg: weight, previousMinReps: analysis.minReps,
                reason: "Missed the range twice; drop 10% and rebuild"
            )
        }

        if allAtTop && (rpe ?? 0) <= 8.5 {
            if bodyweight {
                return ProgressionSuggestion(
                    kind: .addReps, weightKg: 0, reps: target.hi + 1,
                    previousWeightKg: 0, previousMinReps: analysis.minReps,
                    reason: "Hit \(target.hi) reps on every set; add a rep"
                )
            }
            let next = storage(snap(display(weight) + increment))
            return ProgressionSuggestion(
                kind: .increaseWeight, weightKg: next, reps: target.lo,
                previousWeightKg: weight, previousMinReps: analysis.minReps,
                reason: "Hit \(target.hi) reps on every set last time"
            )
        }

        if allAtBottom && !veryHard {
            let reps = allAtTop ? target.hi : min(target.hi, analysis.minReps + 1)
            return ProgressionSuggestion(
                kind: .addReps, weightKg: weight, reps: reps,
                previousWeightKg: weight, previousMinReps: analysis.minReps,
                reason: allAtTop ? "Effort was high (RPE \(formatRpe(rpe))); repeat before adding weight"
                                 : "Same weight, one more rep"
            )
        }

        let reason = veryHard
            ? "Effort was very high (RPE \(formatRpe(rpe)))"
            : "Repeat last time's weight and build back to \(target.lo)+"
        return ProgressionSuggestion(
            kind: .hold, weightKg: weight, reps: target.lo,
            previousWeightKg: weight, previousMinReps: analysis.minReps,
            reason: reason
        )
    }

    private struct Analysis { let weight: Double; let minReps: Int; let avgRpe: Double? }

    /// The dominant weight of the session's PR-eligible sets and the stats at that weight.
    private static func analyze(_ session: SessionSets) -> Analysis? {
        let eligible = session.sets.filter { $0.setType.countsForPR && $0.reps > 0 }
        guard !eligible.isEmpty else { return nil }
        var counts: [Double: Int] = [:]
        for s in eligible { counts[(s.weightKg * 100).rounded() / 100, default: 0] += 1 }
        guard let weight = counts.max(by: { a, b in
            a.value != b.value ? a.value < b.value : a.key < b.key
        })?.key else { return nil }
        let top = eligible.filter { abs(($0.weightKg * 100).rounded() / 100 - weight) < 0.001 }
        let minReps = top.map(\.reps).min() ?? 0
        let rpes = top.compactMap(\.rpe)
        let avg = rpes.isEmpty ? nil : Double(rpes.reduce(0, +)) / Double(rpes.count)
        return Analysis(weight: weight, minReps: minReps, avgRpe: avg)
    }

    private static func formatRpe(_ rpe: Double?) -> String {
        guard let rpe else { return "–" }
        return rpe == rpe.rounded() ? String(Int(rpe)) : String(format: "%.1f", rpe)
    }
}
