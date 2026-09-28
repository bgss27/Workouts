import SwiftUI
import SwiftData

/// Two-step swap flow surfaced when the user taps a "Train X More Often"
/// suggestion. Step 1 picks a routine to add; step 2 picks an existing plan
/// day to overwrite (so the plan keeps the same number of days, just
/// rebalanced toward the under-trained muscle). Mirrors the Android pair of
/// `PickRoutineDialog` + `PickDayDialog`.
///
/// State is kept in the parent (`HomeView`) so the two sheets can hand off:
/// after PickRoutine sets `pickedRoutineId`, PickDay opens automatically.

struct PlanSwapState: Equatable {
    let suggestion: Suggestion
    let routineOptions: [RoutineOption]
    var chosenRoutineId: PersistentIdentifier?
    var dayOptions: [DayOption] = []
    var recommendedDayPlanId: PersistentIdentifier?

    struct RoutineOption: Identifiable, Equatable {
        let id: PersistentIdentifier
        let routine: Routine
        let alreadyInPlan: Bool
        let exercisePreview: String
    }

    struct DayOption: Identifiable, Equatable {
        let id: PersistentIdentifier   // plan.persistentModelID
        let plan: UserPlan
        let dayOrder: Int
        let routineName: String
        let muscleGroupLabel: String
        let isTargetGroup: Bool
    }

    static func == (lhs: PlanSwapState, rhs: PlanSwapState) -> Bool {
        lhs.suggestion.id == rhs.suggestion.id
            && lhs.chosenRoutineId == rhs.chosenRoutineId
    }
}

// MARK: - Step 1: Pick routine

struct PickRoutineSheet: View {
    let state: PlanSwapState
    let onPick: (PersistentIdentifier) -> Void
    let onDismiss: () -> Void

    var body: some View {
        NavigationStack {
            List {
                Section {
                    Text("Step 1 of 2 — choose which routine you want to train. Next you'll pick which existing day to convert.")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                Section {
                    ForEach(state.routineOptions) { option in
                        Button {
                            onPick(option.id)
                        } label: {
                            HStack(alignment: .top) {
                                VStack(alignment: .leading, spacing: 4) {
                                    HStack {
                                        Text(option.routine.name)
                                            .font(.subheadline.bold())
                                            .foregroundColor(.primary)
                                        if option.alreadyInPlan {
                                            Text("In plan")
                                                .font(.caption2)
                                                .padding(.horizontal, 6)
                                                .padding(.vertical, 2)
                                                .background(Color.secondary.opacity(0.2))
                                                .cornerRadius(4)
                                        }
                                    }
                                    if !option.routine.routineDescription.isEmpty {
                                        Text(option.routine.routineDescription)
                                            .font(.caption)
                                            .foregroundColor(.secondary)
                                    }
                                    if !option.exercisePreview.isEmpty {
                                        Text(option.exercisePreview)
                                            .font(.caption2)
                                            .foregroundColor(.secondary.opacity(0.7))
                                    }
                                }
                                Spacer()
                                Image(systemName: "chevron.right").foregroundColor(.secondary)
                            }
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            .navigationTitle("Pick a \(state.suggestion.muscleGroup?.displayName ?? "") routine")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { onDismiss() }
                }
            }
        }
    }
}

// MARK: - Step 2: Pick day

struct PickDaySheet: View {
    let state: PlanSwapState
    let onPick: (PersistentIdentifier) -> Void
    let onDismiss: () -> Void

    private var chosenRoutineName: String {
        state.routineOptions
            .first(where: { $0.id == state.chosenRoutineId })?
            .routine.name ?? "the new routine"
    }

    var body: some View {
        let targetGroupLabel = state.suggestion.muscleGroup?.displayName.lowercased() ?? "target"
        NavigationStack {
            List {
                Section {
                    Text("Step 2 of 2 — pick the existing plan day to convert into '\(chosenRoutineName)'. The recommended day is highlighted; it's the one most spaced out from your existing \(targetGroupLabel) days.")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                Section {
                    ForEach(state.dayOptions) { day in
                        Button {
                            onPick(day.id)
                        } label: {
                            HStack(alignment: .center) {
                                VStack(alignment: .leading, spacing: 4) {
                                    HStack {
                                        Text("Day \(day.dayOrder): \(day.routineName)")
                                            .font(.subheadline.bold())
                                            .foregroundColor(.primary)
                                        if day.id == state.recommendedDayPlanId {
                                            Text("Recommended")
                                                .font(.caption2)
                                                .padding(.horizontal, 6)
                                                .padding(.vertical, 2)
                                                .background(Color.accentColor.opacity(0.2))
                                                .foregroundColor(.accentColor)
                                                .cornerRadius(4)
                                        }
                                    }
                                    Text(day.isTargetGroup
                                         ? "Already \(state.suggestion.muscleGroup?.displayName ?? "") — picking this would erase, not add"
                                         : day.muscleGroupLabel)
                                        .font(.caption)
                                        .foregroundColor(day.isTargetGroup ? .fitTrackDanger : .secondary)
                                }
                                Spacer()
                                Image(systemName: "chevron.right").foregroundColor(.secondary)
                            }
                            .padding(.vertical, 4)
                            .background(
                                day.id == state.recommendedDayPlanId
                                    ? Color.accentColor.opacity(0.06)
                                    : Color.clear
                            )
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            .navigationTitle("Replace which day?")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { onDismiss() }
                }
            }
        }
    }
}

// MARK: - Planner helpers used by HomeView

enum PlanSwapPlanner {
    /// Build the initial PlanSwapState (with routine options) from a frequency
    /// suggestion. Returns nil if there's no plan or no candidate routines.
    static func prepareSwap(
        suggestion: Suggestion,
        context: ModelContext
    ) -> (PlanSwapState?, String?) {
        guard let group = suggestion.muscleGroup else { return (nil, nil) }
        let allPlans = (try? context.fetch(FetchDescriptor<UserPlan>())) ?? []
        if allPlans.isEmpty {
            return (nil, "Set up a plan first, then I can rebalance it")
        }
        let routinesFetch = FetchDescriptor<Routine>()
        let allRoutines = (try? context.fetch(routinesFetch)) ?? []
        let groupRoutines = allRoutines.filter { r in
            r.exercises.contains { $0.exercise?.muscleGroup == group }
        }
        if groupRoutines.isEmpty {
            return (nil, "No \(group.displayName.lowercased()) routine available")
        }
        let inPlanIds = Set(allPlans.compactMap { $0.routine?.persistentModelID })
        let ordered = groupRoutines.sorted { a, b in
            let aIn = inPlanIds.contains(a.persistentModelID)
            let bIn = inPlanIds.contains(b.persistentModelID)
            if aIn == bIn { return a.name < b.name }
            return !aIn && bIn
        }
        let options: [PlanSwapState.RoutineOption] = ordered.map { r in
            let names = r.exercises.prefix(3).compactMap { $0.exercise?.name }
            let more = max(0, r.exercises.count - names.count)
            let preview = names.joined(separator: ", ")
                + (more > 0 ? ", +\(more) more" : "")
            return PlanSwapState.RoutineOption(
                id: r.persistentModelID,
                routine: r,
                alreadyInPlan: inPlanIds.contains(r.persistentModelID),
                exercisePreview: preview
            )
        }
        return (PlanSwapState(suggestion: suggestion, routineOptions: options), nil)
    }

    /// After step 1: compute the existing plan days the user can overwrite,
    /// plus the spacing-aware "recommended" pick.
    static func computeDayOptions(
        for suggestion: Suggestion,
        context: ModelContext
    ) -> (days: [PlanSwapState.DayOption], recommended: PersistentIdentifier?) {
        guard let target = suggestion.muscleGroup else { return ([], nil) }
        let allPlans = (try? context.fetch(FetchDescriptor<UserPlan>())) ?? []

        struct DayInfo {
            let plan: UserPlan
            let routineName: String
            let group: MuscleGroup?
        }
        let info: [DayInfo] = allPlans.map { plan in
            let routine = plan.routine
            // Primary muscle group = most common across the routine's exercises.
            var counts: [MuscleGroup: Int] = [:]
            for re in routine?.exercises ?? [] {
                if let g = re.exercise?.muscleGroup { counts[g, default: 0] += 1 }
            }
            let primary = counts.max(by: { $0.value < $1.value })?.key
            return DayInfo(plan: plan, routineName: routine?.name ?? "Workout", group: primary)
        }
        let targetDayOrders: [Int] = info.filter { $0.group == target }.map { $0.plan.dayOrder }
        let groupCounts: [MuscleGroup: Int] = {
            var c: [MuscleGroup: Int] = [:]
            for d in info { if let g = d.group { c[g, default: 0] += 1 } }
            return c
        }()

        // Score non-target days: spacing * 100 + over-representation count.
        // Higher score wins. Inline scorer because DayInfo is a local type.
        func scoreFor(_ day: DayInfo) -> Int {
            let spacing: Int = targetDayOrders.isEmpty
                ? 1_000
                : targetDayOrders.map { abs($0 - day.plan.dayOrder) }.min() ?? 1_000
            return spacing * 100 + (day.group.flatMap { groupCounts[$0] } ?? 0)
        }
        let nonTarget = info.filter { $0.group != target }
        let recommended = nonTarget.max { a, b in scoreFor(a) < scoreFor(b) } ?? info.last

        let dayOptions: [PlanSwapState.DayOption] = info.map { d in
            PlanSwapState.DayOption(
                id: d.plan.persistentModelID,
                plan: d.plan,
                dayOrder: d.plan.dayOrder,
                routineName: d.routineName,
                muscleGroupLabel: d.group?.displayName ?? "Mixed",
                isTargetGroup: d.group == target
            )
        }
        return (dayOptions, recommended?.plan.persistentModelID)
    }

}
