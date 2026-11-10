import Foundation
import SwiftData

/// How a logged set counts. Mirrors Android's `SetType`.
///
/// * `working`  – a normal set; counts for volume and PRs.
/// * `warmup`   – excluded from volume, PRs and "working set" counts.
/// * `dropset`  – counts for volume but not PRs (the weight was dropped
///                mid-set, so the 1RM estimate would be misleading).
/// * `failure`  – a working set taken to failure; counts for everything.
enum SetType: String, Codable, CaseIterable, Identifiable {
    case working, warmup, dropset, failure

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .working: return "Working"
        case .warmup: return "Warm-up"
        case .dropset: return "Drop set"
        case .failure: return "Failure"
        }
    }

    /// Short glyph for the set badge; working sets show their number instead.
    var badge: String? {
        switch self {
        case .working: return nil
        case .warmup: return "W"
        case .dropset: return "D"
        case .failure: return "F"
        }
    }

    var countsForPR: Bool { self == .working || self == .failure }
    var countsAsWorking: Bool { self != .warmup }
}

@Model
final class WorkoutSet {
    var setNumber: Int
    var reps: Int
    var weightKg: Double
    /// Legacy flag. Kept as a stored attribute so existing stores migrate
    /// additively; `setType` is the source of truth and keeps it in sync.
    var isWarmup: Bool
    /// Raw `SetType`. Rows written before set types existed hold the default
    /// "working" even when `isWarmup` is true, so the accessor below honours
    /// the legacy flag in that case.
    var setTypeRaw: String = SetType.working.rawValue
    var rpe: Int?
    /// Timed sets (plank, carries by time, cardio). nil when the exercise's
    /// tracking mode has no duration.
    var durationSeconds: Int?
    /// Distance covered, in metres. nil when the mode has no distance.
    var distanceMeters: Double?
    var workoutExercise: WorkoutExercise?

    /// Volume and 1RM maths only make sense for weight × reps.
    var isWeightReps: Bool { reps > 0 && durationSeconds == nil && distanceMeters == nil }

    var setType: SetType {
        get {
            let stored = SetType(rawValue: setTypeRaw) ?? .working
            if stored == .working && isWarmup { return .warmup }
            return stored
        }
        set {
            setTypeRaw = newValue.rawValue
            isWarmup = newValue == .warmup
        }
    }

    var countsForPR: Bool { setType.countsForPR }

    init(setNumber: Int, reps: Int, weightKg: Double, isWarmup: Bool = false, rpe: Int? = nil) {
        self.setNumber = setNumber
        self.reps = reps
        self.weightKg = weightKg
        self.isWarmup = isWarmup
        self.setTypeRaw = (isWarmup ? SetType.warmup : SetType.working).rawValue
        self.rpe = rpe
    }

    convenience init(
        setNumber: Int,
        reps: Int,
        weightKg: Double,
        setType: SetType,
        rpe: Int? = nil,
        durationSeconds: Int? = nil,
        distanceMeters: Double? = nil
    ) {
        self.init(setNumber: setNumber, reps: reps, weightKg: weightKg, isWarmup: setType == .warmup, rpe: rpe)
        self.setTypeRaw = setType.rawValue
        self.durationSeconds = durationSeconds
        self.distanceMeters = distanceMeters
    }
}
