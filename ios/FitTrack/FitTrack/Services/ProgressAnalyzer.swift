import Foundation
import SwiftData

/// Mirror of the Kotlin `ProgressAnalyzer.analyzeMuscleGroupFrequency`.
///
/// Generates Improvement-Tip suggestions. Today only the frequency-based
/// rule is ported (the user-visible "Train X More Often" line on Home);
/// weight-increase / deload / try-new-exercise can be ported as needed.
///
/// Plan-aware: if the user has a plan that schedules X 1x/week, hitting X
/// 1x/week is ON TRACK — no suggestion. Fall back to a generic 2x/week
/// threshold only when no plan is set.
enum ProgressAnalyzer {
    /// Public entry point. Pure read against SwiftData; safe to call on
    /// any thread (we use it from the main thread in HomeView via .task).
    static func generateSuggestions(context: ModelContext) -> [Suggestion] {
        var out: [Suggestion] = []
        analyzeMuscleGroupFrequency(context: context, into: &out)
        return out.sorted { $0.priority < $1.priority }
    }

    private static let fourWeeks: TimeInterval = 28 * 24 * 60 * 60

    private static func analyzeMuscleGroupFrequency(
        context: ModelContext,
        into out: inout [Suggestion]
    ) {
        // Fetch completed workouts in the last 4 weeks.
        let cutoff = Date().addingTimeInterval(-fourWeeks)
        let descriptor = FetchDescriptor<Workout>(
            predicate: #Predicate { $0.endTime != nil && $0.startTime >= cutoff }
        )
        guard let workouts = try? context.fetch(descriptor), !workouts.isEmpty else { return }

        // Skip if user has < 14 days of history — too early to flag missed days.
        let firstDescriptor = FetchDescriptor<Workout>(
            predicate: #Predicate { $0.endTime != nil },
            sortBy: [SortDescriptor(\Workout.startTime)]
        )
        if let firstEver = try? context.fetch(firstDescriptor).first {
            let daysSpan = Int(Date().timeIntervalSince(firstEver.startTime) / (24 * 60 * 60))
            if daysSpan < 14 { return }
        }

        // Distinct workout days per muscle group, NOT exercises. A back day
        // with 5 back exercises = 1, not 5. Matches Android.
        var muscleGroupDays: [MuscleGroup: Int] = [:]
        for workout in workouts {
            var hit = Set<MuscleGroup>()
            for we in workout.exercises {
                if let group = we.exercise?.muscleGroup { hit.insert(group) }
            }
            for g in hit {
                muscleGroupDays[g, default: 0] += 1
            }
        }

        let planned = computePlannedFrequencyPerWeek(context: context)

        let weeksTracked = 4
        // Iterate over every muscle group the plan covers, not just trained
        // ones — a 0-times-in-4-weeks shoulder should still fire when shoulders
        // are scheduled.
        let groupsToCheck = Set(muscleGroupDays.keys).union(planned.keys)
        for group in groupsToCheck {
            if group == .cardio { continue }
            let totalDays = muscleGroupDays[group] ?? 0
            let target = planned[group] ?? 2
            let expectedTotal = target * weeksTracked
            if totalDays < expectedTotal {
                out.append(.increaseFrequency(
                    muscleGroup: group,
                    sessionsLast4Weeks: totalDays,
                    suggestedFreqPerWeek: target
                ))
            }
        }
    }

    /// Days-per-week each muscle group is scheduled by the user's current
    /// plan. Each plan day's routine contributes 1 for every muscle group
    /// that has at least one exercise in it (primary muscle only — counting
    /// secondaries is too noisy).
    private static func computePlannedFrequencyPerWeek(context: ModelContext) -> [MuscleGroup: Int] {
        let descriptor = FetchDescriptor<UserPlan>()
        guard let plans = try? context.fetch(descriptor), !plans.isEmpty else { return [:] }
        var planned: [MuscleGroup: Int] = [:]
        for plan in plans {
            guard let routine = plan.routine else { continue }
            var groups = Set<MuscleGroup>()
            for re in routine.exercises {
                if let g = re.exercise?.muscleGroup { groups.insert(g) }
            }
            for g in groups {
                planned[g, default: 0] += 1
            }
        }
        return planned
    }
}
