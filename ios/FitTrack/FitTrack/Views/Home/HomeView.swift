import SwiftUI
import SwiftData
import Charts

struct HomeView: View {
    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject var proManager: ProManager
    @Query(sort: \Workout.startTime, order: .reverse) private var workouts: [Workout]
    @Query private var userPlans: [UserPlan]

    // Home Workout sheet state. `pendingHomeExercises` is set when the sheet
    // generates a workout; the navigationDestination flip then pushes the
    // active workout. We can't push directly from inside the sheet callback
    // because the sheet is still presented at that point.
    @State private var showHomeWorkoutSheet = false
    @State private var pendingHomeExercises: [Exercise] = []
    @State private var navigateToGeneratedWorkout = false

    // Improvement Tips state. Refreshed off-main via `.task`. The plan-swap
    // flow lives here: tapping an actionable suggestion populates `swapState`,
    // step 1 sets `chosenRoutineId`, step 2 commits the swap.
    @State private var suggestions: [Suggestion] = []
    @State private var swapState: PlanSwapState?
    @State private var swapToast: String?

    /// Suggested muscle group when opening the sheet — primary muscle of the
    /// first plan day's routine, or chest as the catch-all default.
    private var suggestedHomeMuscleGroup: MuscleGroup? {
        guard let routine = userPlans.sorted(by: { $0.dayOrder < $1.dayOrder }).first?.routine else {
            return nil
        }
        let groups = routine.exercises.compactMap { $0.exercise?.muscleGroup }
        guard !groups.isEmpty else { return nil }
        let counts = Dictionary(grouping: groups, by: { $0 }).mapValues(\.count)
        return counts.max(by: { $0.value < $1.value })?.key
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    // My Plan preview
                    if !userPlans.isEmpty {
                        myPlanSection
                    } else {
                        setupPlanCard
                    }

                    // Pro upgrade banner
                    if !proManager.isPro {
                        NavigationLink(destination: UpgradeView()) {
                            HStack {
                                Image(systemName: "star.fill")
                                    .foregroundColor(.yellow)
                                VStack(alignment: .leading) {
                                    Text("Upgrade to Pro").font(.subheadline.bold())
                                    Text("Unlock ML insights, all programs").font(.caption).opacity(0.7)
                                }
                                Spacer()
                                Image(systemName: "chevron.right").opacity(0.5)
                            }
                            .padding()
                            .background(.ultraThinMaterial)
                            .cornerRadius(12)
                        }
                        .buttonStyle(.plain)
                    }

                    // Stats
                    statsCard

                    // Quick actions
                    quickActions

                    // Train at home — secondary CTA, distinct from the Quick
                    // Workout entry. Uses a person/yoga icon to avoid the
                    // "house = go home" overload.
                    Button {
                        showHomeWorkoutSheet = true
                    } label: {
                        HStack {
                            Image(systemName: "figure.mind.and.body")
                            VStack(alignment: .leading) {
                                Text("Home Workout").font(.subheadline.bold())
                                Text("No-equipment session for today's muscle group")
                                    .font(.caption).opacity(0.7)
                            }
                            Spacer()
                            Image(systemName: "chevron.right").opacity(0.5)
                        }
                        .padding()
                        .background(Color(.secondarySystemBackground))
                        .cornerRadius(12)
                    }
                    .buttonStyle(.plain)

                    // Improvement Tips
                    improvementTipsSection

                    // Recent workouts
                    recentWorkoutsSection
                }
                .padding()
            }
            .navigationTitle("FitTrack")
            .sheet(isPresented: $showHomeWorkoutSheet) {
                HomeWorkoutSheet(defaultGroup: suggestedHomeMuscleGroup) { exercises in
                    pendingHomeExercises = exercises
                    navigateToGeneratedWorkout = true
                }
            }
            .navigationDestination(isPresented: $navigateToGeneratedWorkout) {
                ActiveWorkoutView(routine: nil, initialExercises: pendingHomeExercises)
            }
            // Plan-swap two-step. Drives off `swapState`:
            //   * present == nil          → no sheet
            //   * chosenRoutineId == nil  → step 1 (pick routine)
            //   * chosenRoutineId set     → step 2 (pick day)
            .sheet(isPresented: Binding(
                get: { swapState != nil && swapState?.chosenRoutineId == nil },
                set: { if !$0 { swapState = nil } }
            )) {
                if let state = swapState {
                    PickRoutineSheet(
                        state: state,
                        onPick: { id in pickRoutineForSwap(id) },
                        onDismiss: { swapState = nil }
                    )
                }
            }
            .sheet(isPresented: Binding(
                get: { swapState?.chosenRoutineId != nil },
                set: { if !$0 { swapState = nil } }
            )) {
                if let state = swapState {
                    PickDaySheet(
                        state: state,
                        onPick: { id in commitSwap(planId: id) },
                        onDismiss: { swapState = nil }
                    )
                }
            }
            .task(id: workouts.count) {
                await refreshSuggestions()
            }
            .alert(swapToast ?? "", isPresented: Binding(
                get: { swapToast != nil },
                set: { if !$0 { swapToast = nil } }
            )) {
                Button("OK", role: .cancel) {}
            }
        }
    }

    // MARK: - Improvement Tips

    private var improvementTipsSection: some View {
        Group {
            if !suggestions.isEmpty {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Improvement Tips")
                        .font(.headline)
                    ForEach(suggestions.prefix(3)) { s in
                        SuggestionCard(
                            suggestion: s,
                            onTap: s.isActionable ? { applySuggestion(s) } : nil
                        )
                    }
                }
            }
        }
    }

    @MainActor
    private func refreshSuggestions() async {
        suggestions = ProgressAnalyzer.generateSuggestions(context: modelContext)
    }

    private func applySuggestion(_ suggestion: Suggestion) {
        guard suggestion.kind == .increaseFrequency else { return }
        let (state, error) = PlanSwapPlanner.prepareSwap(
            suggestion: suggestion,
            context: modelContext
        )
        if let state {
            swapState = state
        } else {
            swapToast = error ?? "Couldn't prepare a swap."
        }
    }

    private func pickRoutineForSwap(_ routineId: PersistentIdentifier) {
        guard let current = swapState else { return }
        let (days, recommended) = PlanSwapPlanner.computeDayOptions(
            for: current.suggestion,
            context: modelContext
        )
        var updated = current
        updated.chosenRoutineId = routineId
        updated.dayOptions = days
        updated.recommendedDayPlanId = recommended
        swapState = updated
    }

    private func commitSwap(planId: PersistentIdentifier) {
        guard let state = swapState,
              let routineId = state.chosenRoutineId,
              let day = state.dayOptions.first(where: { $0.id == planId }),
              let chosen = state.routineOptions.first(where: { $0.id == routineId })
        else {
            swapToast = "That option is no longer available."
            swapState = nil
            return
        }
        let oldName = day.routineName
        day.plan.routine = chosen.routine
        try? modelContext.save()
        swapToast = "Day \(day.dayOrder): '\(oldName)' → '\(chosen.routine.name)'"
        swapState = nil
        Task { await refreshSuggestions() }
    }

    private var myPlanSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("My Plan").font(.headline)
                if let prog = userPlans.first?.programName {
                    Text(prog).font(.caption).foregroundColor(.accentColor)
                }
                Spacer()
                NavigationLink("View All") { MyPlanView() }
                    .font(.caption)
            }

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    ForEach(userPlans.sorted { $0.dayOrder < $1.dayOrder }, id: \.self) { plan in
                        NavigationLink(destination: ActiveWorkoutView(routine: plan.routine)) {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(plan.programName != nil ? "Day \(plan.dayOrder)" : "Workout")
                                    .font(.caption).foregroundColor(.accentColor)
                                Text(plan.routine?.name ?? "Workout")
                                    .font(.subheadline.bold())
                                    .lineLimit(1)
                                Text("\(plan.routine?.exercises.count ?? 0) exercises")
                                    .font(.caption2).opacity(0.6)
                                Label("Start", systemImage: "play.fill")
                                    .font(.caption.bold())
                                    .padding(.horizontal, 12).padding(.vertical, 6)
                                    .background(Color.accentColor)
                                    .foregroundColor(.white)
                                    .cornerRadius(8)
                            }
                            .padding()
                            .frame(width: 170)
                            .background(Color(.secondarySystemBackground))
                            .cornerRadius(12)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
    }

    private var setupPlanCard: some View {
        NavigationLink(destination: RoutineListView()) {
            HStack {
                Image(systemName: "calendar")
                VStack(alignment: .leading) {
                    Text("Set up your workout plan").font(.subheadline.bold())
                    Text("Choose a 3-day or 5-day program").font(.caption).opacity(0.7)
                }
                Spacer()
                Image(systemName: "chevron.right").opacity(0.5)
            }
            .padding()
            .background(Color(.secondarySystemBackground))
            .cornerRadius(12)
        }
        .buttonStyle(.plain)
    }

    private var statsCard: some View {
        let completed = workouts.filter(\.isCompleted)
        // Monday-anchored start of this week, matching the Android calc.
        let now = Date()
        let cal = Calendar.current
        let weekday = cal.component(.weekday, from: now)
        let daysSinceMonday = (weekday + 5) % 7
        let startOfWeek = cal.date(byAdding: .day, value: -daysSinceMonday,
                                   to: cal.startOfDay(for: now)) ?? now
        let thisWeek = completed.filter { $0.startTime >= startOfWeek }.count
        // Total volume: sum(weightKg * reps) across working (non-warmup) sets.
        let totalVolumeKg = completed.reduce(0.0) { acc, w in
            acc + w.exercises.reduce(0.0) { eAcc, e in
                eAcc + e.sets.filter { !$0.isWarmup }.reduce(0.0) { sAcc, s in
                    sAcc + s.weightKg * Double(s.reps)
                }
            }
        }
        let isLbs = UserDefaults.standard.string(forKey: "profile_weightUnit") == "lbs"
        let totalVolume = isLbs ? totalVolumeKg * 2.2046226218 : totalVolumeKg
        let unit = isLbs ? "lbs" : "kg"

        return VStack(alignment: .leading, spacing: 12) {
            Text("Your Progress").font(.subheadline.bold())
            HStack {
                HomeStat(value: "\(completed.count)", label: "Total")
                HomeStat(value: "\(thisWeek)", label: "This Week")
                HomeStat(value: formatVolume(totalVolume), label: "Volume (\(unit))")
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding()
        .background(Color.accentColor.opacity(0.1))
        .cornerRadius(12)
    }

    /// Compact volume formatting that matches the Android `formatVolume` helper.
    /// Numbers can hit the millions for power users; collapse to "12.3k" /
    /// "1.2M" so the row never wraps.
    private func formatVolume(_ value: Double) -> String {
        switch value {
        case 1_000_000...: return String(format: "%.1fM", value / 1_000_000)
        case 10_000...: return String(format: "%.0fk", value / 1000)
        case 1_000...: return String(format: "%.1fk", value / 1000)
        default: return String(format: "%.0f", value)
        }
    }

    private var quickActions: some View {
        HStack(spacing: 12) {
            NavigationLink(destination: ActiveWorkoutView(routine: nil)) {
                quickActionCard(icon: "dumbbell.fill", label: "Workout")
            }
            NavigationLink(destination: RoutineListView()) {
                quickActionCard(icon: "list.bullet", label: "Routines")
            }
            NavigationLink(destination: ExerciseProgressView()) {
                quickActionCard(icon: "chart.xyaxis.line", label: "Progress")
            }
            NavigationLink(destination: InsightsView()) {
                quickActionCard(icon: "brain.head.profile", label: "Insights")
            }
        }
        .buttonStyle(.plain)
    }

    private func quickActionCard(icon: String, label: String) -> some View {
        VStack(spacing: 4) {
            Image(systemName: icon)
            Text(label).font(.caption2)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
        .background(Color(.secondarySystemBackground))
        .cornerRadius(10)
    }

    private var recentWorkoutsSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("Recent Workouts").font(.headline)
                Spacer()
                NavigationLink("See all") { WorkoutHistoryView() }
                    .font(.caption)
            }
            if workouts.filter(\.isCompleted).isEmpty {
                Text("No workouts yet. Start your first one!")
                    .font(.caption).opacity(0.6)
                    .frame(maxWidth: .infinity).padding(.vertical, 32)
            } else {
                ForEach(workouts.filter(\.isCompleted).prefix(5), id: \.self) { workout in
                    HStack {
                        VStack(alignment: .leading) {
                            Text(workout.startTime, style: .date).font(.subheadline.bold())
                            Text("\(workout.exercises.count) exercises")
                                .font(.caption).opacity(0.6)
                        }
                        Spacer()
                        if let mins = workout.durationMinutes {
                            Text("\(mins) min").font(.caption).foregroundColor(.accentColor)
                        }
                    }
                    .padding()
                    .background(Color(.secondarySystemBackground))
                    .cornerRadius(10)
                }
            }
        }
    }
}

// MARK: - 3-stat helper

private struct HomeStat: View {
    let value: String
    let label: String

    var body: some View {
        VStack(spacing: 2) {
            Text(value)
                .font(.title2.bold())
                .lineLimit(1)
                .minimumScaleFactor(0.7)
            Text(label)
                .font(.caption2)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
    }
}

// MARK: - Workout History

struct WorkoutHistoryView: View {
    @EnvironmentObject private var proManager: ProManager
    @Query(filter: #Predicate<Workout> { $0.endTime != nil },
           sort: \Workout.startTime, order: .reverse)
    private var completedWorkouts: [Workout]

    private var historyCutoff: Date {
        Calendar.current.date(byAdding: .day, value: -ProManager.FreeTier.historyDays, to: Date()) ?? .distantPast
    }

    var body: some View {
        let visible = proManager.isPro
            ? completedWorkouts
            : completedWorkouts.filter { $0.startTime >= historyCutoff }
        let hidden = completedWorkouts.count - visible.count
        let isClamped = !proManager.isPro && hidden > 0

        Group {
            if completedWorkouts.isEmpty {
                emptyState
            } else {
                List {
                    ForEach(visible, id: \.persistentModelID) { workout in
                        WorkoutHistoryRow(workout: workout)
                    }
                    if isClamped {
                        FullHistoryUpgradeCta(
                            hiddenCount: hidden,
                            unit: hidden == 1 ? "older workout" : "older workouts"
                        )
                        .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 8, trailing: 16))
                    }
                }
                .listStyle(.plain)
            }
        }
        .navigationTitle("Workout History")
        .navigationBarTitleDisplayMode(.inline)
    }

    private var emptyState: some View {
        VStack(spacing: 12) {
            Image(systemName: "dumbbell.fill").font(.system(size: 48)).opacity(0.3)
            Text("No workout history").font(.headline)
            Text("Complete your first workout to see it here.")
                .font(.caption).opacity(0.6)
        }
        .padding(32)
    }
}

private struct WorkoutHistoryRow: View {
    let workout: Workout

    private var dateText: String {
        let f = DateFormatter()
        f.dateStyle = .full
        f.timeStyle = .none
        return f.string(from: workout.startTime)
    }

    private var timeText: String {
        let f = DateFormatter()
        f.dateStyle = .none
        f.timeStyle = .short
        return f.string(from: workout.startTime)
    }

    private var summary: String {
        let names = workout.exercises
            .sorted(by: { $0.orderIndex < $1.orderIndex })
            .compactMap { $0.exercise?.name }
        if names.isEmpty { return "\(workout.exercises.count) exercise(s)" }
        return names.joined(separator: ", ")
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(dateText).font(.subheadline.bold())
            Text(summary)
                .font(.caption).opacity(0.7)
                .lineLimit(2)
            HStack(spacing: 12) {
                Text(timeText).font(.caption2).opacity(0.5)
                if let mins = workout.durationMinutes {
                    Text("\(mins) min").font(.caption2).foregroundColor(.accentColor)
                }
            }
        }
        .padding(.vertical, 4)
    }
}

// MARK: - Per-exercise progress (1RM + volume charts)

struct ExerciseProgressView: View {
    @EnvironmentObject private var proManager: ProManager
    @AppStorage("profile_weightUnit") private var unit = "kg"
    @Query(sort: \Exercise.name) private var exercises: [Exercise]
    @Query(filter: #Predicate<Workout> { $0.endTime != nil })
    private var completedWorkouts: [Workout]

    @State private var selectedMuscleGroup: MuscleGroup?
    @State private var selectedExercise: Exercise?
    /// Per-exercise sparkline data, recomputed when the underlying workout
    /// set changes. Cached so the LazyVStack doesn't recompute on every
    /// SwiftUI render pass — for a large catalog this matters.
    @State private var sparklineData: [PersistentIdentifier: [ProgressDataPoint]] = [:]

    private var isLbs: Bool { unit == "lbs" }
    private func toDisplay(_ kg: Double) -> Double {
        isLbs ? kg * 2.2046226218 : kg
    }

    private var historyCutoff: Date {
        Calendar.current.date(byAdding: .day, value: -ProManager.FreeTier.historyDays, to: Date()) ?? .distantPast
    }

    private var filteredExercises: [Exercise] {
        guard let group = selectedMuscleGroup else { return exercises }
        return exercises.filter { $0.muscleGroup == group }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                muscleGroupChips

                if let exercise = selectedExercise {
                    chartCard(for: exercise)
                }

                Text("Select an exercise to view progress")
                    .font(.subheadline.bold())
                    .padding(.horizontal)

                LazyVStack(spacing: 4) {
                    ForEach(filteredExercises, id: \.persistentModelID) { exercise in
                        Button { selectedExercise = exercise } label: {
                            HStack {
                                VStack(alignment: .leading) {
                                    Text(exercise.name).font(.subheadline)
                                    Text(exercise.muscleGroup.displayName)
                                        .font(.caption).opacity(0.6)
                                }
                                Spacer()
                                if let points = sparklineData[exercise.persistentModelID] {
                                    ExerciseSparkline(points: points, convert: toDisplay)
                                }
                                if selectedExercise?.persistentModelID == exercise.persistentModelID {
                                    Image(systemName: "checkmark")
                                        .foregroundColor(.accentColor)
                                }
                            }
                            .padding()
                            .background(
                                selectedExercise?.persistentModelID == exercise.persistentModelID
                                    ? Color.accentColor.opacity(0.1)
                                    : Color(.secondarySystemBackground)
                            )
                            .cornerRadius(10)
                        }
                        .buttonStyle(.plain)
                        .padding(.horizontal)
                    }
                }
            }
            .padding(.vertical)
        }
        .navigationTitle("Progress")
        .navigationBarTitleDisplayMode(.inline)
        // Precompute sparkline data when the workout set changes. Runs off
        // the main render path so the LazyVStack doesn't recompute it per row
        // per re-render. Triggered by workout count change (covers new
        // workouts) and by entering the view fresh.
        .task(id: completedWorkouts.count) {
            await rebuildSparklineData()
        }
    }

    private func rebuildSparklineData() async {
        var map: [PersistentIdentifier: [ProgressDataPoint]] = [:]
        for exercise in exercises {
            let pts = ProgressMath.dataPoints(for: exercise, across: completedWorkouts)
            if pts.count >= 2 {
                map[exercise.persistentModelID] = Array(pts.suffix(8))
            }
        }
        sparklineData = map
    }

    private var muscleGroupChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                FilterChipView(label: "All", selected: selectedMuscleGroup == nil) {
                    selectedMuscleGroup = nil
                    selectedExercise = nil
                }
                ForEach(MuscleGroup.allCases) { group in
                    FilterChipView(label: group.displayName, selected: selectedMuscleGroup == group) {
                        selectedMuscleGroup = (selectedMuscleGroup == group) ? nil : group
                        selectedExercise = nil
                    }
                }
            }
            .padding(.horizontal)
        }
    }

    private func chartCard(for exercise: Exercise) -> some View {
        let allPoints = ProgressMath.dataPoints(for: exercise, across: completedWorkouts)
        let visible = proManager.isPro ? allPoints : allPoints.filter { $0.date >= historyCutoff }
        let hidden = allPoints.count - visible.count
        let isClamped = !proManager.isPro && hidden > 0

        return VStack(alignment: .leading, spacing: 12) {
            Text(exercise.name).font(.headline).padding(.horizontal)

            if visible.isEmpty {
                Text("No data yet — complete a workout with this exercise to see your progress.")
                    .font(.caption).opacity(0.7)
                    .padding(.horizontal)
            } else {
                let oneRmAxis = "Est. 1RM (\(unit))"
                let volumeAxis = "Volume (\(unit))"
                Chart(visible, id: \.date) { point in
                    LineMark(x: .value("Date", point.date), y: .value(oneRmAxis, toDisplay(point.estimated1RM)))
                        .foregroundStyle(Color.accentColor)
                        .interpolationMethod(.monotone)
                    PointMark(x: .value("Date", point.date), y: .value(oneRmAxis, toDisplay(point.estimated1RM)))
                        .foregroundStyle(Color.accentColor)
                }
                .frame(height: 180)
                .padding(.horizontal)
                // Re-key on selected exercise so swapping triggers SwiftUI
                // Charts' built-in transition instead of a hard cut.
                .animation(.easeInOut(duration: 0.6), value: exercise.persistentModelID)

                Text("Total Volume (\(unit))").font(.caption.bold()).padding(.horizontal)
                Chart(visible, id: \.date) { point in
                    BarMark(x: .value("Date", point.date), y: .value(volumeAxis, toDisplay(point.totalVolume)))
                        .foregroundStyle(Color.accentColor.opacity(0.6))
                }
                .frame(height: 140)
                .padding(.horizontal)
                .animation(.easeInOut(duration: 0.6), value: exercise.persistentModelID)
            }

            if isClamped {
                FullHistoryUpgradeCta(hiddenCount: hidden, unit: "data points")
                    .padding(.horizontal)
            }
        }
        .padding(.vertical, 12)
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
        .padding(.horizontal)
    }
}

// MARK: - Shared upgrade CTA

private struct FullHistoryUpgradeCta: View {
    let hiddenCount: Int
    let unit: String

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("Showing the last \(ProManager.FreeTier.historyDays) days")
                .font(.subheadline.bold())
            Text("\(hiddenCount) \(unit) hidden. Unlock full history with Pro.")
                .font(.caption).opacity(0.7)
            NavigationLink {
                UpgradeView()
            } label: {
                Label("Unlock with Pro", systemImage: "star.fill")
                    .font(.caption.bold())
                    .padding(.horizontal, 12).padding(.vertical, 6)
                    .background(Color.accentColor)
                    .foregroundColor(.white)
                    .cornerRadius(8)
            }
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.accentColor.opacity(0.1))
        .cornerRadius(12)
    }
}

// MARK: - Per-exercise progression math

/// Pure value type representing a single workout's contribution to an
/// Tiny inline 1RM trend chart used in the exercise picker rows. No labels,
/// no axes — just a thin line so the user can pick the "one that's improving"
/// at a glance. Mirrors Android's `Sparkline` composable.
private struct ExerciseSparkline: View {
    let points: [ProgressDataPoint]
    let convert: (Double) -> Double

    var body: some View {
        Chart(points, id: \.date) { point in
            LineMark(x: .value("x", point.date), y: .value("y", convert(point.estimated1RM)))
                .foregroundStyle(Color.accentColor)
                .interpolationMethod(.monotone)
        }
        .chartXAxis(.hidden)
        .chartYAxis(.hidden)
        .chartPlotStyle { plot in
            plot.background(Color.clear)
        }
        .frame(width: 56, height: 24)
        .padding(.trailing, 6)
    }
}

/// exercise's progress timeline. Mirrors Android's `ProgressDataPoint`.
private struct ProgressDataPoint {
    let date: Date
    let estimated1RM: Double
    let totalVolume: Double
}

private enum ProgressMath {
    /// Walk every completed workout, find sets matching this exercise, and
    /// emit one data point per workout (best-1RM + total volume of working
    /// sets). Sorted ascending by date.
    static func dataPoints(for exercise: Exercise, across workouts: [Workout]) -> [ProgressDataPoint] {
        let target = exercise.persistentModelID
        var points: [ProgressDataPoint] = []
        for workout in workouts {
            guard workout.endTime != nil else { continue }
            let matchingExercises = workout.exercises.filter {
                $0.exercise?.persistentModelID == target
            }
            let workingSets = matchingExercises.flatMap { $0.sets }.filter { !$0.isWarmup }
            if workingSets.isEmpty { continue }

            let best1RM = workingSets.map { estimateOneRepMax(weight: $0.weightKg, reps: $0.reps) }.max() ?? 0
            let volume = workingSets.reduce(0.0) { $0 + $1.weightKg * Double($1.reps) }
            points.append(ProgressDataPoint(
                date: workout.startTime,
                estimated1RM: best1RM,
                totalVolume: volume
            ))
        }
        return points.sorted { $0.date < $1.date }
    }

    /// Epley one-rep-max estimate. Matches Android's `estimateOneRepMax` so
    /// numbers line up across platforms.
    private static func estimateOneRepMax(weight: Double, reps: Int) -> Double {
        if reps <= 0 || weight <= 0 { return 0 }
        if reps == 1 { return weight }
        return weight * (1 + Double(reps) / 30.0)
    }
}
