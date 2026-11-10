import Foundation
import SwiftData

/// Weekly hard-set counts per muscle group against a target band, plus the
/// deload advice derived from plateau + effort + volume. Mirrors Android's
/// `VolumeStats` and `DeloadAdvisor`.
struct VolumeBand: Equatable {
    let low: Int
    let high: Int

    static func forGroup(_ group: MuscleGroup) -> VolumeBand {
        switch group {
        case .abs, .forearms, .calves: return VolumeBand(low: 6, high: 12)
        default: return VolumeBand(low: 10, high: 20)
        }
    }
}

struct MuscleVolumeRow: Identifiable, Equatable {
    enum Status { case below, inRange, above }

    let muscleGroup: MuscleGroup
    let sets: Int
    let fourWeekAverage: Double
    let band: VolumeBand

    var id: String { muscleGroup.rawValue }
    var status: Status {
        if sets < band.low { return .below }
        if sets > band.high { return .above }
        return .inRange
    }
}

struct DeloadAdvice: Identifiable, Equatable {
    enum Scope: Equatable { case muscleGroup(MuscleGroup), global }

    let scope: Scope
    let title: String
    let reason: String
    let action: String

    var id: String {
        switch scope {
        case .muscleGroup(let g): return "group-\(g.rawValue)"
        case .global: return "global"
        }
    }

    static let defaultAction = "Next week: half the sets, keep ~90% of the weight."
}

enum VolumeGuidance {
    /// Groups the guidance covers (no cardio / full body buckets).
    static let groups: [MuscleGroup] = MuscleGroup.allCases.filter { $0 != .cardio && $0 != .fullBody }

    /// Start of the calendar week containing `date`, per the device locale.
    static func weekStart(containing date: Date, calendar: Calendar = .current) -> Date {
        let comps = calendar.dateComponents([.yearForWeekOfYear, .weekOfYear], from: date)
        return calendar.date(from: comps) ?? calendar.startOfDay(for: date)
    }

    /// Non-warm-up sets per primary muscle group for workouts starting in [start, end).
    static func setCounts(workouts: [Workout], from start: Date, to end: Date) -> [MuscleGroup: Int] {
        var counts: [MuscleGroup: Int] = [:]
        for w in workouts where w.endTime != nil && w.startTime >= start && w.startTime < end {
            for we in w.exercises {
                guard let group = we.exercise?.muscleGroup else { continue }
                let hard = we.sets.filter { !$0.isWarmup }.count
                if hard > 0 { counts[group, default: 0] += hard }
            }
        }
        return counts
    }

    /// Rows for the week starting at `weekStart`, sorted by sets desc then name.
    static func rows(workouts: [Workout], weekStart: Date, calendar: Calendar = .current) -> [MuscleVolumeRow] {
        let weekEnd = calendar.date(byAdding: .day, value: 7, to: weekStart) ?? weekStart
        let thisWeek = setCounts(workouts: workouts, from: weekStart, to: weekEnd)
        let fourWeeksAgo = calendar.date(byAdding: .day, value: -28, to: Date()) ?? Date()
        let recent = setCounts(workouts: workouts, from: fourWeeksAgo, to: Date())
        return groups.map { group in
            MuscleVolumeRow(
                muscleGroup: group,
                sets: thisWeek[group] ?? 0,
                fourWeekAverage: Double(recent[group] ?? 0) / 4.0,
                band: .forGroup(group)
            )
        }
        .sorted { a, b in a.sets != b.sets ? a.sets > b.sets : a.muscleGroup.displayName < b.muscleGroup.displayName }
    }

    static func completedWorkouts(in context: ModelContext) -> [Workout] {
        let descriptor = FetchDescriptor<Workout>(predicate: #Predicate { $0.endTime != nil })
        return (try? context.fetch(descriptor)) ?? []
    }
}

enum DeloadAdvisor {
    /// Deload advice per muscle group plus an optional global one.
    static func evaluate(workouts: [Workout], now: Date = Date(), calendar: Calendar = .current) -> [DeloadAdvice] {
        var out: [DeloadAdvice] = []
        let thisWeekStart = VolumeGuidance.weekStart(containing: now, calendar: calendar)

        for group in VolumeGuidance.groups {
            let band = VolumeBand.forGroup(group)
            // Over volume: above the band top in each of the last 3 full weeks.
            var weeksAbove = 0
            for i in 1...3 {
                let start = calendar.date(byAdding: .day, value: -7 * i, to: thisWeekStart)!
                let end = calendar.date(byAdding: .day, value: 7, to: start)!
                let sets = VolumeGuidance.setCounts(workouts: workouts, from: start, to: end)[group] ?? 0
                if sets > band.high { weeksAbove += 1 }
            }
            let overVolume = weeksAbove == 3

            let plateaued = isPlateaued(group: group, workouts: workouts)
            let rpe = rpeTrend(group: group, workouts: workouts, now: now, calendar: calendar)

            if (plateaued && rpe.rising) || overVolume {
                let reason: String
                if overVolume {
                    reason = "3 weeks above \(band.high) sets"
                } else {
                    reason = "Strength flat for 3 sessions and effort rising (RPE \(String(format: "%.1f", rpe.recent ?? 0)))"
                }
                out.append(DeloadAdvice(
                    scope: .muscleGroup(group),
                    title: "Deload \(group.displayName)",
                    reason: reason,
                    action: DeloadAdvice.defaultAction
                ))
            }
        }

        if let global = globalAdvice(workouts: workouts, thisWeekStart: thisWeekStart, calendar: calendar) {
            out.append(global)
        }
        return out
    }

    // MARK: - Plateau

    /// Best e1RM of the last 3 sessions vs the 3 before, per exercise; the group
    /// is plateaued when at least 2 exercises (or all, if fewer) show < 1% gain.
    static func isPlateaued(group: MuscleGroup, workouts: [Workout]) -> Bool {
        var sessionsByExercise: [PersistentIdentifier: [(Date, Double)]] = [:]
        for w in workouts.sorted(by: { $0.startTime < $1.startTime }) where w.endTime != nil {
            for we in w.exercises {
                guard let ex = we.exercise, ex.muscleGroup == group, ex.trackingMode == .weightReps else { continue }
                let best = we.sets.filter { $0.countsForPR && $0.reps > 0 && $0.weightKg > 0 }
                    .map { $0.weightKg * (1 + Double($0.reps) / 30) }.max() ?? 0
                if best > 0 { sessionsByExercise[ex.persistentModelID, default: []].append((w.startTime, best)) }
            }
        }
        let eligible = sessionsByExercise.values.filter { $0.count >= 3 }
        guard !eligible.isEmpty else { return false }
        var flat = 0
        for sessions in eligible {
            let values = sessions.map(\.1)
            let recent = values.suffix(3)
            let baseline: Double
            if values.count >= 6 {
                baseline = values.dropLast(3).suffix(3).max() ?? 0
            } else {
                baseline = recent.first ?? 0
            }
            guard baseline > 0 else { continue }
            if ((recent.max() ?? 0) - baseline) / baseline < 0.01 { flat += 1 }
        }
        return flat >= min(2, eligible.count)
    }

    // MARK: - RPE

    struct RpeTrend { let recent: Double?; let prior: Double?; let rising: Bool }

    static func rpeTrend(group: MuscleGroup, workouts: [Workout], now: Date, calendar: Calendar) -> RpeTrend {
        let twoWeeksAgo = calendar.date(byAdding: .day, value: -14, to: now)!
        let fourWeeksAgo = calendar.date(byAdding: .day, value: -28, to: now)!
        var recent: [Int] = [], prior: [Int] = []
        for w in workouts where w.endTime != nil && w.startTime >= fourWeeksAgo {
            for we in w.exercises where we.exercise?.muscleGroup == group {
                let rpes = we.sets.filter { !$0.isWarmup }.compactMap(\.rpe)
                if w.startTime >= twoWeeksAgo { recent += rpes } else { prior += rpes }
            }
        }
        func mean(_ xs: [Int]) -> Double? { xs.isEmpty ? nil : Double(xs.reduce(0, +)) / Double(xs.count) }
        let r = mean(recent), p = mean(prior)
        let rising: Bool
        if let r {
            if let p { rising = r >= 8.5 && r > p } else { rising = r >= 9.0 }
        } else {
            rising = false
        }
        return RpeTrend(recent: r, prior: p, rising: rising)
    }

    // MARK: - Global

    /// Six or more consecutive full weeks at ≥ 60% of the 8-week mean with no lighter week.
    static func globalAdvice(workouts: [Workout], thisWeekStart: Date, calendar: Calendar) -> DeloadAdvice? {
        var weekly: [Int] = []
        for i in 1...8 {
            let start = calendar.date(byAdding: .day, value: -7 * i, to: thisWeekStart)!
            let end = calendar.date(byAdding: .day, value: 7, to: start)!
            let total = VolumeGuidance.setCounts(workouts: workouts, from: start, to: end).values.reduce(0, +)
            weekly.append(total) // index 0 = last full week
        }
        let mean = Double(weekly.reduce(0, +)) / 8.0
        guard mean > 0 else { return nil }
        var loaded = 0
        for sets in weekly {
            if Double(sets) >= mean * 0.6 { loaded += 1 } else { break }
        }
        guard loaded >= 6 else { return nil }
        return DeloadAdvice(
            scope: .global,
            title: "Time for a deload week",
            reason: "\(loaded) weeks of full training without a lighter week",
            action: DeloadAdvice.defaultAction
        )
    }
}
