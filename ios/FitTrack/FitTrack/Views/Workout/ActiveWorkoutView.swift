import SwiftUI
import SwiftData
import UIKit
import AudioToolbox
import AVFoundation

struct ActiveWorkoutView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var healthKit: HealthKitService
    @AppStorage("profile_weightUnit") private var storedWeightUnit = "kg"
    @Query(sort: \Exercise.name) private var allExercises: [Exercise]

    /// Snapshotted at workout start — if the user flips the Settings unit
    /// mid-session, the in-flight workout keeps using the original unit so
    /// already-entered numbers aren't reinterpreted.
    @State private var weightUnit: String = "kg"

    let routine: Routine?
    /// Hand-picked exercises to seed the workout with — used by the "Home
    /// Workout" generator, which builds an ad-hoc session without persisting
    /// a routine. Ignored when `routine` is set; otherwise loaded in order.
    let initialExercises: [Exercise]

    init(routine: Routine?, initialExercises: [Exercise] = []) {
        self.routine = routine
        self.initialExercises = initialExercises
    }

    @State private var workout: Workout?
    @State private var exerciseEntries: [ExerciseEntry] = []
    /// Non-empty when the finished workout produced PRs — drives the
    /// post-workout celebration overlay.
    @State private var celebrationPRs: [PRResult] = []
    @State private var showExercisePicker = false
    @State private var showFinishAlert = false
    @State private var showDiscardAlert = false
    @State private var searchText = ""
    @State private var selectedGroup: MuscleGroup?

    struct ExerciseEntry: Identifiable {
        let id = UUID()
        var exercise: Exercise
        var sets: [SetEntry]
        /// Non-nil when this exercise is part of a superset; shared with peers.
        var supersetGroup: Int? = nil
    }

    struct SetEntry: Identifiable {
        let id = UUID()
        var weight: String = ""
        var reps: String = ""
        var isWarmup: Bool = false
        /// RPE 6-10. nil = unrated. Warmup sets are conventionally not rated.
        var rpe: Int? = nil
        /// Whether the user has marked this set as done. Auto-toggles on when
        /// weight + reps are both valid for a non-warmup set, but the user can
        /// tap the leading button to manually toggle it back off and edit.
        /// Session-only — not persisted to SwiftData.
        var isCompleted: Bool = false
    }

    private let guidance = TimeOfDayAdvisor.getGuidance()
    @State private var showTips = false
    /// Per-workout dismissal of the warmup banner. Resets between workouts.
    @State private var warmupDismissed = false

    @State private var restTimerEndsAt: Date?
    @State private var showRestTimer = false
    @State private var showPlateCalc = false

    /// Default rest length auto-started when a working set is logged.
    private static let defaultRestSeconds: TimeInterval = 90

    /// Any completed working set restarts the rest timer to the full default.
    /// If the user is moving through a superset / new exercise mid-rest, that's
    /// the signal their previous rest is over (parity with Android).
    private func autoStartRestTimer() {
        restTimerEndsAt = Date().addingTimeInterval(Self.defaultRestSeconds)
    }
    @State private var formExpanded: Set<UUID> = []
    /// When non-nil, the swap sheet is open for this exercise entry.
    @State private var swapTarget: SwapTarget?

    /// Accordion: only one exercise's set inputs are visible at a time. Auto-
    /// advances when the user finishes the current exercise's working sets.
    /// A nil value means the user explicitly collapsed everything; we don't
    /// re-open without a tap. Parity with Android's `expandedIndex`.
    @State private var expandedExerciseID: UUID?

    private func isWorkingSetComplete(_ set: SetEntry) -> Bool {
        !set.isWarmup && set.isCompleted
    }

    /// "Has the user typed valid numbers?" — the auto-toggle trigger.
    /// Distinct from `isWorkingSetComplete` (which checks the user-visible
    /// completion flag) so the user can manually un-tick a set to edit it
    /// without auto-snapping back to complete.
    private func canBeAutoCompleted(_ set: SetEntry) -> Bool {
        !set.isWarmup && (Int(set.reps) ?? 0) > 0 && (Double(set.weight) ?? 0) > 0
    }

    /// Recompute the active exercise after a set change. If the currently-
    /// expanded entry has all of its working sets filled in, advance to the
    /// next entry that still has incomplete sets.
    private func autoAdvanceExpansion() {
        guard let current = expandedExerciseID,
              let idx = exerciseEntries.firstIndex(where: { $0.id == current }) else { return }
        let workingSets = exerciseEntries[idx].sets.filter { !$0.isWarmup }
        let allDone = !workingSets.isEmpty && workingSets.allSatisfy(isWorkingSetComplete)
        guard allDone else { return }
        if let next = exerciseEntries[(idx + 1)...].firstIndex(where: { entry in
            let ws = entry.sets.filter { !$0.isWarmup }
            return ws.isEmpty || !ws.allSatisfy(isWorkingSetComplete)
        }) {
            // firstIndex on a slice returns an index into the slice's start —
            // it's actually the absolute index into the array. Use it directly.
            expandedExerciseID = exerciseEntries[next].id
        }
    }

    private struct SwapTarget: Identifiable { let id: UUID }

    var body: some View {
        NavigationStack {
            List {
                // Time-of-day guidance banner (with warmup recommendations).
                // Hidden entirely once the user dismisses it for this workout.
                if !warmupDismissed {
                Section {
                    DisclosureGroup(isExpanded: $showTips) {
                        Text(guidance.warmupAdvice.description)
                            .font(.caption)
                            .foregroundColor(.secondary)
                            .padding(.bottom, 4)

                        let muscleGroups = exerciseEntries.map { $0.exercise.muscleGroup }
                        ForEach(WarmupCatalog.forMuscleGroups(muscleGroups)) { section in
                            WarmupSectionView(section: section)
                                .padding(.top, 4)
                        }

                        Text("Other tips")
                            .font(.caption.bold())
                            .foregroundColor(.secondary)
                            .padding(.top, 8)
                        ForEach(guidance.detailedTips, id: \.self) { tip in
                            HStack(alignment: .top, spacing: 6) {
                                Text("•").font(.caption)
                                Text(tip).font(.caption).foregroundColor(.secondary)
                            }
                        }

                        if guidance.intensityModifier < 1.0 {
                            Text("Suggested intensity: \(Int(guidance.intensityModifier * 100))% of usual weight")
                                .font(.caption.bold())
                                .foregroundColor(.accentColor)
                                .padding(.top, 4)
                        }

                        Button("Skip warmup") { warmupDismissed = true }
                            .font(.caption)
                            .padding(.top, 8)
                            .frame(maxWidth: .infinity, alignment: .trailing)
                    } label: {
                        HStack(spacing: 8) {
                            Image(systemName: guidance.timeOfDay.icon)
                                .foregroundColor(.accentColor)
                            Text(guidance.tip)
                                .font(.subheadline.bold())
                            Spacer()
                            Button {
                                warmupDismissed = true
                            } label: {
                                Image(systemName: "xmark.circle.fill")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
                }

                ForEach($exerciseEntries) { $entry in
                    Section {
                        // Accordion: only render the set inputs / form / add-set
                        // button when this exercise is the active one. The header
                        // stays visible so the user can tap to expand a collapsed
                        // exercise or use the action buttons.
                        if expandedExerciseID == entry.id {
                            // How-to text — pulled from the bundled
                            // free-exercise-db dataset first, then hand-written
                            // fallbacks for entries the open DB doesn't cover.
                            // Helps users figure out unfamiliar bodyweight moves
                            // (Wall Slides, Pike Push-Up, etc.) that ship with
                            // the at-home generator.
                            if let description = ExerciseDescriptions.text(for: entry.exercise.name) {
                                Text(description)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                    .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 8, trailing: 16))
                            }
                            let hasForm = !ExerciseImageService.getImageURLs(for: entry.exercise.name).isEmpty
                            if hasForm {
                                Button {
                                    if formExpanded.contains(entry.id) {
                                        formExpanded.remove(entry.id)
                                    } else {
                                        formExpanded.insert(entry.id)
                                    }
                                } label: {
                                    Label(
                                        formExpanded.contains(entry.id) ? "Hide form" : "View form",
                                        systemImage: "photo"
                                    )
                                    .font(.caption)
                                }
                                if formExpanded.contains(entry.id) {
                                    ExerciseImageGalleryView(exerciseName: entry.exercise.name)
                                        .listRowInsets(EdgeInsets(top: 4, leading: 8, bottom: 4, trailing: 8))
                                }
                            }

                            ForEach($entry.sets) { $set in
                                // Auto-toggle "completed" the first time the
                                // user fills in valid weight + reps for a
                                // non-warmup set. Doesn't auto-toggle BACK off
                                // when the user edits — they untick manually
                                // via the completion button.
                                let canAutoComplete = !set.isWarmup
                                    && (Int(set.reps) ?? 0) > 0
                                    && (Double(set.weight) ?? 0) > 0
                                VStack(alignment: .leading, spacing: 4) {
                                    HStack {
                                        SetCompleteButton(
                                            isCompleted: set.isCompleted,
                                            isWarmup: set.isWarmup,
                                            canBeComplete: canAutoComplete
                                        ) {
                                            set.isCompleted.toggle()
                                        }
                                        Text(set.isWarmup ? "W" : "\(entry.sets.firstIndex(where: { $0.id == set.id }).map { $0 + 1 } ?? 0)")
                                            .frame(width: 20)
                                            .foregroundColor(set.isWarmup ? .teal : .primary)
                                        TextField(weightUnit, text: $set.weight)
                                            .keyboardType(.decimalPad)
                                            .textFieldStyle(.roundedBorder)
                                        TextField("Reps", text: $set.reps)
                                            .keyboardType(.numberPad)
                                            .textFieldStyle(.roundedBorder)
                                        Button {
                                            let nowWarmup = !set.isWarmup
                                            set.isWarmup = nowWarmup
                                            // Switching to warmup clears any
                                            // RPE rating AND the completed flag
                                            // — warmups don't count.
                                            if nowWarmup {
                                                set.rpe = nil
                                                set.isCompleted = false
                                            }
                                        } label: {
                                            Text("W").foregroundColor(set.isWarmup ? .teal : .gray.opacity(0.3))
                                        }
                                    }
                                    if !set.isWarmup {
                                        RpeRow(rpe: $set.rpe)
                                    }
                                }
                                .padding(.vertical, 4)
                                .padding(.horizontal, 6)
                                .background(set.isCompleted ? Color.fitTrackSuccess.opacity(0.15) : Color.clear)
                                .cornerRadius(8)
                                .animation(.easeInOut(duration: 0.22), value: set.isCompleted)
                                .onChange(of: canAutoComplete) { _, can in
                                    // Auto-tick on, but never auto-tick off —
                                    // the user might be editing a completed set.
                                    if can && !set.isCompleted {
                                        set.isCompleted = true
                                    }
                                }
                                .onChange(of: set.isCompleted) { wasComplete, nowComplete in
                                    if nowComplete && !wasComplete {
                                        UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                                        autoStartRestTimer()
                                        autoAdvanceExpansion()
                                    }
                                }
                            }
                            .onDelete { indices in entry.sets.remove(atOffsets: indices) }

                            Button("Add Set") {
                                entry.sets.append(SetEntry())
                            }
                        }
                    } header: {
                        HStack {
                            // Tappable title area — toggles expansion. Wrapped
                            // in a plain Button so it doesn't visually conflict
                            // with the action buttons to the right.
                            Button {
                                if expandedExerciseID == entry.id {
                                    expandedExerciseID = nil
                                } else {
                                    expandedExerciseID = entry.id
                                }
                            } label: {
                                HStack {
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(entry.exercise.name).font(.subheadline.bold())
                                        HStack(spacing: 6) {
                                            Text(entry.exercise.muscleGroup.displayName)
                                                .font(.caption).opacity(0.6)
                                            if entry.supersetGroup != nil {
                                                SupersetBadge()
                                            }
                                        }
                                    }
                                    Image(systemName: expandedExerciseID == entry.id ? "chevron.down" : "chevron.right")
                                        .font(.caption2)
                                        .foregroundColor(.secondary)
                                }
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .contentShape(Rectangle())
                            }
                            .buttonStyle(.plain)
                            Button { swapTarget = SwapTarget(id: entry.id) } label: {
                                Image(systemName: "arrow.left.arrow.right")
                                    .font(.caption)
                                    .foregroundColor(.accentColor)
                            }
                            .accessibilityLabel("Swap exercise")
                            if shouldShowSupersetButton(for: entry) {
                                Button { toggleSuperset(for: entry.id) } label: {
                                    Image(systemName: entry.supersetGroup != nil ? "link.circle.fill" : "link")
                                        .font(.caption)
                                        .foregroundColor(.accentColor)
                                }
                                .accessibilityLabel(entry.supersetGroup != nil ? "Remove from superset" : "Group with previous as superset")
                            }
                            Button(role: .destructive) {
                                exerciseEntries.removeAll { $0.id == entry.id }
                            } label: {
                                Image(systemName: "trash").font(.caption)
                            }
                        }
                    }
                }

                Button { showExercisePicker = true } label: {
                    Label("Add Exercise", systemImage: "plus")
                }
            }
            .navigationTitle("Workout")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Discard") { showDiscardAlert = true }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button { showRestTimer = true } label: {
                        if let endsAt = restTimerEndsAt {
                            TimelineView(.periodic(from: .now, by: 0.5)) { ctx in
                                Text(formatRemaining(from: ctx.date, until: endsAt))
                                    .font(.caption.bold())
                                    .monospacedDigit()
                            }
                        } else {
                            Image(systemName: "timer")
                        }
                    }
                    .accessibilityLabel("Rest timer")
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button { showPlateCalc = true } label: {
                        Image(systemName: "function")
                    }
                    .accessibilityLabel("Plate calculator")
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Finish") { showFinishAlert = true }
                }
            }
            .onAppear { startWorkout() }
            // Fire a beep + heavy haptic the moment the rest timer hits 0,
            // independent of whether the sheet is open. `task(id:)` cancels
            // and restarts when the user picks a new preset or completes
            // another set (which restarts the timer).
            .task(id: restTimerEndsAt) {
                guard let endsAt = restTimerEndsAt else { return }
                let delay = endsAt.timeIntervalSinceNow
                if delay > 0 {
                    do { try await Task.sleep(for: .seconds(delay)) }
                    catch { return }
                }
                guard !Task.isCancelled else { return }
                playRestTimerEndAlert()
            }
            .sheet(isPresented: $showExercisePicker) { exercisePickerSheet }
            .sheet(isPresented: $showRestTimer) {
                RestTimerSheet(endsAt: $restTimerEndsAt)
                    .presentationDetents([.medium])
            }
            .sheet(isPresented: $showPlateCalc) {
                PlateCalculatorSheet(defaultUnit: weightUnit == "lbs" ? .lb : .kg)
                    .presentationDetents([.medium, .large])
            }
            .sheet(item: $swapTarget) { target in
                if let idx = exerciseEntries.firstIndex(where: { $0.id == target.id }) {
                    SwapExerciseSheet(
                        target: exerciseEntries[idx].exercise,
                        excludedIds: Set(exerciseEntries.map { $0.exercise.persistentModelID }),
                        allExercises: allExercises,
                        onPick: { picked in
                            swapExercise(at: idx, with: picked)
                            swapTarget = nil
                        }
                    )
                }
            }
            .alert("Finish Workout?", isPresented: $showFinishAlert) {
                Button("Save") { finishWorkout() }
                Button("Cancel", role: .cancel) {}
            } message: { Text("Save \(exerciseEntries.count) exercise(s)?") }
            .alert("Discard Workout?", isPresented: $showDiscardAlert) {
                Button("Discard", role: .destructive) { discardWorkout() }
                Button("Keep Going", role: .cancel) {}
            }
            .fullScreenCover(isPresented: Binding(
                get: { !celebrationPRs.isEmpty },
                set: { if !$0 { celebrationPRs = [] } }
            )) {
                PostWorkoutCelebration(
                    prs: celebrationPRs,
                    displayUnit: weightUnit,
                    onDismiss: {
                        celebrationPRs = []
                        dismiss()
                    }
                )
                .presentationBackground(.clear)
            }
        }
    }

    private var exercisePickerSheet: some View {
        NavigationStack {
            List {
                let filtered = allExercises.filter { ex in
                    let matchesGroup = selectedGroup == nil || ex.muscleGroup == selectedGroup
                    let matchesSearch = searchText.isEmpty || ex.name.localizedCaseInsensitiveContains(searchText)
                    return matchesGroup && matchesSearch
                }
                ForEach(filtered, id: \.self) { exercise in
                    Button {
                        let newEntry = ExerciseEntry(exercise: exercise, sets: [SetEntry()])
                        exerciseEntries.append(newEntry)
                        // Auto-open the just-added exercise so the user can
                        // start logging immediately.
                        expandedExerciseID = newEntry.id
                        showExercisePicker = false
                    } label: {
                        HStack(spacing: 12) {
                            ExercisePickerThumbnail(exerciseName: exercise.name)
                            VStack(alignment: .leading) {
                                Text(exercise.name)
                                Text(exercise.muscleGroup.displayName).font(.caption).opacity(0.6)
                            }
                            Spacer()
                            Image(systemName: "plus.circle")
                                .foregroundColor(.accentColor)
                        }
                    }
                }
            }
            .searchable(text: $searchText, prompt: "Search exercises")
            .navigationTitle("Add Exercise")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { showExercisePicker = false }
                }
            }
        }
    }

    private func startWorkout() {
        let w = Workout()
        modelContext.insert(w)
        workout = w
        weightUnit = storedWeightUnit

        if let routine {
            for re in routine.exercises.sorted(by: { $0.orderIndex < $1.orderIndex }) {
                guard let ex = re.exercise else { continue }
                exerciseEntries.append(ExerciseEntry(exercise: ex, sets: [SetEntry()]))
            }
        } else if !initialExercises.isEmpty {
            // Ad-hoc workout pre-loaded by the Home Workout generator.
            for ex in initialExercises {
                exerciseEntries.append(ExerciseEntry(exercise: ex, sets: [SetEntry()]))
            }
        }
        // Open the first exercise's accordion by default.
        expandedExerciseID = exerciseEntries.first?.id
    }

    private func finishWorkout() {
        guard let workout else { return }
        let startDate = workout.startTime
        let isLbs = weightUnit == "lbs"
        for (i, entry) in exerciseEntries.enumerated() {
            let we = WorkoutExercise(
                exercise: entry.exercise,
                orderIndex: i,
                supersetGroup: entry.supersetGroup
            )
            we.workout = workout
            workout.exercises.append(we)
            var setNum = 0
            for set in entry.sets {
                guard let reps = Int(set.reps), let typedWeight = Double(set.weight), reps > 0 else { continue }
                if !set.isWarmup { setNum += 1 }
                // Storage is always kg; convert if user typed in lbs.
                let weightKg = isLbs ? typedWeight * 0.45359237 : typedWeight
                let ws = WorkoutSet(
                    setNumber: set.isWarmup ? 0 : setNum,
                    reps: reps,
                    weightKg: weightKg,
                    isWarmup: set.isWarmup,
                    rpe: set.isWarmup ? nil : set.rpe
                )
                ws.workoutExercise = we
                we.sets.append(ws)
            }
        }
        let endDate = Date()
        workout.endTime = endDate
        try? modelContext.save()

        // Mirror to Apple Health. Service no-ops cleanly if disabled / not authorized.
        Task { await healthKit.writeWorkout(startDate: startDate, endDate: endDate) }

        // Check for 1RM PRs against prior workouts. If any, hold the screen
        // for the celebration overlay; otherwise dismiss immediately.
        let prs = PRDetector.detectPRs(workout: workout, context: modelContext)
        if prs.isEmpty {
            dismiss()
        } else {
            celebrationPRs = prs
        }
    }

    // MARK: - Superset helpers

    private func shouldShowSupersetButton(for entry: ExerciseEntry) -> Bool {
        if entry.supersetGroup != nil { return true }
        guard let index = exerciseEntries.firstIndex(where: { $0.id == entry.id }) else { return false }
        return index > 0
    }

    /// Replace the exercise at [index] with [newExercise]. Preserves superset
    /// membership; resets sets (different movement = different working weight).
    private func swapExercise(at index: Int, with newExercise: Exercise) {
        guard exerciseEntries.indices.contains(index) else { return }
        let supersetGroup = exerciseEntries[index].supersetGroup
        exerciseEntries[index] = ExerciseEntry(
            exercise: newExercise,
            sets: [SetEntry()],
            supersetGroup: supersetGroup
        )
        formExpanded.removeAll() // collapse any open form gallery for the old exercise
    }

    /// Toggle superset membership for the exercise with [entryId]:
    ///
    /// - Already in a group → ungroup. If exactly one peer remains, ungroup
    ///   the peer too (no orphan single-element groups).
    /// - Not grouped → join the previous exercise's group, or start a new
    ///   one with the previous exercise as the first member.
    private func toggleSuperset(for entryId: UUID) {
        guard let index = exerciseEntries.firstIndex(where: { $0.id == entryId }) else { return }
        let original = exerciseEntries[index].supersetGroup

        if let group = original {
            exerciseEntries[index].supersetGroup = nil
            let peerIndices = exerciseEntries.indices
                .filter { exerciseEntries[$0].supersetGroup == group }
            if peerIndices.count == 1 {
                exerciseEntries[peerIndices[0]].supersetGroup = nil
            }
        } else {
            guard index > 0 else { return }
            let groupId: Int
            if let prevGroup = exerciseEntries[index - 1].supersetGroup {
                groupId = prevGroup
            } else {
                let maxExisting = exerciseEntries.compactMap { $0.supersetGroup }.max() ?? 0
                groupId = maxExisting + 1
                exerciseEntries[index - 1].supersetGroup = groupId
            }
            exerciseEntries[index].supersetGroup = groupId
        }
    }

    private func discardWorkout() {
        if let workout { modelContext.delete(workout) }
        dismiss()
    }
}

// MARK: - Swap exercise sheet

private struct SwapExerciseSheet: View {
    let target: Exercise
    let excludedIds: Set<PersistentIdentifier>
    let allExercises: [Exercise]
    let onPick: (Exercise) -> Void

    @Environment(\.dismiss) private var dismiss

    private var candidates: [Exercise] {
        allExercises.filter {
            $0.muscleGroup == target.muscleGroup &&
                !excludedIds.contains($0.persistentModelID)
        }
    }

    var body: some View {
        NavigationStack {
            Group {
                if candidates.isEmpty {
                    VStack(spacing: 12) {
                        Image(systemName: "checkmark.circle")
                            .font(.system(size: 40))
                            .foregroundColor(.secondary)
                        Text("No alternatives available")
                            .font(.headline)
                        Text("This workout already includes every \(target.muscleGroup.displayName.lowercased()) exercise we have.")
                            .font(.caption)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal)
                    }
                    .padding(32)
                } else {
                    List {
                        Section {
                            ForEach(candidates, id: \.persistentModelID) { exercise in
                                Button { onPick(exercise) } label: {
                                    HStack(spacing: 12) {
                                        ExercisePickerThumbnail(exerciseName: exercise.name)
                                        VStack(alignment: .leading) {
                                            Text(exercise.name)
                                            Text(exercise.muscleGroup.displayName)
                                                .font(.caption).opacity(0.6)
                                        }
                                        Spacer()
                                        Image(systemName: "arrow.left.arrow.right")
                                            .foregroundColor(.accentColor)
                                    }
                                }
                                .buttonStyle(.plain)
                            }
                        } header: {
                            Text("Other \(target.muscleGroup.displayName.lowercased()) exercises")
                        } footer: {
                            Text("Sets you've logged for \(target.name) will reset — the new exercise needs fresh weight numbers.")
                                .font(.caption2)
                        }
                    }
                }
            }
            .navigationTitle("Swap \(target.name)")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
    }
}

// MARK: - Warmup section view

private struct WarmupSectionView: View {
    let section: WarmupSection

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(section.title)
                .font(.caption.bold())
                .foregroundColor(.accentColor)
                .padding(.bottom, 2)

            ForEach(section.exercises) { exercise in
                HStack(alignment: .top, spacing: 6) {
                    Text("•").font(.caption)
                    VStack(alignment: .leading, spacing: 0) {
                        Text(exercise.name)
                            .font(.caption)
                            .foregroundColor(.primary.opacity(0.85))
                        let detail = exercise.note.map { "\(exercise.prescription) · \($0)" }
                            ?? exercise.prescription
                        Text(detail)
                            .font(.caption2)
                            .foregroundColor(.secondary)
                    }
                }
            }
        }
    }
}

// MARK: - RPE row + superset badge

/// Leading completion indicator on each set row. Circle outline when not
/// done, filled checkmark when done. Disabled on warmup rows (warmups don't
/// have a "completed" state). Mirrors Android's `SetCompleteButton`.
private struct SetCompleteButton: View {
    let isCompleted: Bool
    let isWarmup: Bool
    let canBeComplete: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            ZStack {
                Circle()
                    .stroke(strokeColor, lineWidth: 1.5)
                    .frame(width: 22, height: 22)
                if isCompleted {
                    Circle().fill(Color.fitTrackSuccess).frame(width: 22, height: 22)
                    Image(systemName: "checkmark")
                        .font(.caption2.bold())
                        .foregroundColor(.white)
                }
            }
            .animation(.easeInOut(duration: 0.18), value: isCompleted)
        }
        .buttonStyle(.plain)
        .disabled(isWarmup)
        .accessibilityLabel(isCompleted ? "Mark set incomplete" : "Mark set complete")
    }

    private var strokeColor: Color {
        if isCompleted { return .fitTrackSuccess }
        if isWarmup { return .gray.opacity(0.25) }
        if canBeComplete { return .fitTrackSuccess.opacity(0.6) }
        return .gray.opacity(0.5)
    }
}

private struct RpeRow: View {
    @Binding var rpe: Int?
    private static let values = [6, 7, 8, 9, 10]

    var body: some View {
        HStack(spacing: 4) {
            Text("RPE")
                .font(.caption2)
                .foregroundColor(.secondary)
                .padding(.trailing, 4)
            ForEach(Self.values, id: \.self) { value in
                Button { rpe = (rpe == value ? nil : value) } label: {
                    Text("\(value)")
                        .font(.caption.bold())
                        .frame(width: 32, height: 24)
                        .background(rpe == value ? Color.accentColor : Color(.tertiarySystemBackground))
                        .foregroundColor(rpe == value ? .white : .primary)
                        .clipShape(RoundedRectangle(cornerRadius: 6))
                }
                .buttonStyle(.plain)
            }
            Spacer()
        }
        .padding(.leading, 32)
    }
}

private struct SupersetBadge: View {
    var body: some View {
        HStack(spacing: 3) {
            Image(systemName: "link").font(.caption2)
            Text("Superset").font(.caption2.bold())
        }
        .padding(.horizontal, 6)
        .padding(.vertical, 2)
        .background(Color.accentColor)
        .foregroundColor(.white)
        .clipShape(RoundedRectangle(cornerRadius: 4))
    }
}

// MARK: - Exercise picker thumbnail

private struct ExercisePickerThumbnail: View {
    let exerciseName: String

    var body: some View {
        let url = ExerciseImageService.getImageURLs(for: exerciseName).first
        Group {
            if let url {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .empty:
                        Color(.tertiarySystemBackground)
                    case .success(let image):
                        image.resizable().aspectRatio(contentMode: .fill)
                    case .failure:
                        placeholder
                    @unknown default:
                        placeholder
                    }
                }
            } else {
                placeholder
            }
        }
        .frame(width: 40, height: 40)
        .clipShape(RoundedRectangle(cornerRadius: 6))
    }

    private var placeholder: some View {
        Image(systemName: "dumbbell.fill")
            .foregroundColor(.secondary.opacity(0.5))
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color(.tertiarySystemBackground))
    }
}

// MARK: - Rest Timer

/// Format the time remaining until [endsAt] as "m:ss". Returns "0:00" once the
/// timer has elapsed.
private func formatRemaining(from now: Date, until endsAt: Date) -> String {
    let remaining = max(0, Int(endsAt.timeIntervalSince(now)))
    return "\(remaining / 60):\(String(format: "%02d", remaining % 60))"
}

/// Tracks whether we've configured the audio session for playback routing.
/// Set once per process; further calls are cheap no-ops.
private var restTimerAudioSessionConfigured = false

/// Plays a short alert when the rest timer hits 0. Heavy haptic + system
/// "Tink" sound (ID 1057). Audio session is configured to `.playback` with
/// `.mixWithOthers` so the beep plays alongside music and routes through
/// whichever output is active — Bluetooth earbuds, AirPods, wired headphones.
/// This mirrors Android's `ToneGenerator(STREAM_MUSIC, …)` behavior.
private func playRestTimerEndAlert() {
    UIImpactFeedbackGenerator(style: .heavy).impactOccurred()
    if !restTimerAudioSessionConfigured {
        do {
            try AVAudioSession.sharedInstance().setCategory(
                .playback,
                mode: .default,
                options: [.mixWithOthers]
            )
            try AVAudioSession.sharedInstance().setActive(true, options: [])
            restTimerAudioSessionConfigured = true
        } catch {
            // If the session can't be configured we still fall through to
            // play the sound — it'll just route through the default session.
        }
    }
    AudioServicesPlaySystemSound(1057)
}

private struct RestTimerSheet: View {
    @Binding var endsAt: Date?
    @Environment(\.dismiss) private var dismiss

    private let presets: [(label: String, seconds: Int)] = [
        ("60s", 60), ("90s", 90), ("2m", 120), ("3m", 180), ("5m", 300),
    ]

    var body: some View {
        VStack(spacing: 16) {
            Text("Rest Timer")
                .font(.headline)
                .padding(.top, 8)

            TimelineView(.periodic(from: .now, by: 0.2)) { ctx in
                let remaining = endsAt.map { max(0, Int($0.timeIntervalSince(ctx.date))) } ?? 0
                let finished = endsAt != nil && remaining == 0
                Text("\(remaining / 60):\(String(format: "%02d", remaining % 60))")
                    .font(.system(size: 56, weight: .bold, design: .rounded))
                    .monospacedDigit()
                    .foregroundColor(finished ? .accentColor : .primary)
            }
            // Haptic + beep on completion fires from the parent .task(id:)
            // so it works even when this sheet is closed.

            Text("Quick start").font(.caption).foregroundColor(.secondary)

            HStack(spacing: 8) {
                ForEach(presets, id: \.seconds) { preset in
                    Button(preset.label) {
                        endsAt = Date().addingTimeInterval(TimeInterval(preset.seconds))
                    }
                    .buttonStyle(.bordered)
                    .frame(maxWidth: .infinity)
                }
            }
            .padding(.horizontal)

            if endsAt != nil {
                Button {
                    endsAt = nil
                    dismiss()
                } label: {
                    Label("Cancel timer", systemImage: "stop.fill")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .tint(.secondary)
                .padding(.horizontal)
            }

            Spacer()
        }
        .padding()
    }
}

// MARK: - Plate Calculator

private enum PlateUnit: String, CaseIterable, Identifiable {
    case kg, lb
    var id: String { rawValue }
    var label: String { rawValue.uppercased() }

    var defaultBar: Double {
        switch self {
        case .kg: return 20
        case .lb: return 45
        }
    }

    var inventory: [Double] {
        switch self {
        case .kg: return [25, 20, 15, 10, 5, 2.5, 1.25]
        case .lb: return [45, 35, 25, 10, 5, 2.5]
        }
    }
}

private struct PlateBreakdown {
    let platesPerSide: [Double]
    let achievedTotal: Double
    let isExact: Bool
}

private func calculatePlates(targetTotal: Double, bar: Double, inventory: [Double]) -> PlateBreakdown {
    let perSide = (targetTotal - bar) / 2
    if perSide <= 0 {
        return PlateBreakdown(platesPerSide: [], achievedTotal: bar, isExact: abs(targetTotal - bar) < 1e-6)
    }
    var plates: [Double] = []
    var remaining = perSide
    for plate in inventory.sorted(by: >) {
        while remaining + 1e-9 >= plate {
            plates.append(plate)
            remaining -= plate
        }
    }
    let achievedTotal = bar + 2 * plates.reduce(0, +)
    return PlateBreakdown(
        platesPerSide: plates,
        achievedTotal: achievedTotal,
        isExact: abs(targetTotal - achievedTotal) < 1e-6
    )
}

private func formatPlate(_ value: Double) -> String {
    if abs(value - value.rounded()) < 1e-6 { return "\(Int(value))" }
    return String(format: "%.2f", value).replacingOccurrences(of: "0+$", with: "", options: .regularExpression)
        .replacingOccurrences(of: "\\.$", with: "", options: .regularExpression)
}

private struct PlateCalculatorSheet: View {
    init(defaultUnit: PlateUnit = .kg) {
        _unit = State(initialValue: defaultUnit)
    }

    @State private var unit: PlateUnit
    @State private var targetText: String = ""
    @State private var barText: String = ""
    @Environment(\.dismiss) private var dismiss

    private var bar: Double { Double(barText) ?? unit.defaultBar }
    private var target: Double? { Double(targetText) }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Picker("Unit", selection: $unit) {
                        ForEach(PlateUnit.allCases) { Text($0.label).tag($0) }
                    }
                    .pickerStyle(.segmented)
                    .onChange(of: unit) { _, _ in barText = "" }
                }
                Section {
                    HStack {
                        Text("Target")
                        Spacer()
                        TextField("0", text: $targetText)
                            .keyboardType(.decimalPad)
                            .multilineTextAlignment(.trailing)
                            .frame(maxWidth: 100)
                        Text(unit.label).foregroundColor(.secondary)
                    }
                    HStack {
                        Text("Bar")
                        Spacer()
                        TextField(formatPlate(unit.defaultBar), text: $barText)
                            .keyboardType(.decimalPad)
                            .multilineTextAlignment(.trailing)
                            .frame(maxWidth: 100)
                        Text(unit.label).foregroundColor(.secondary)
                    }
                }
                Section("Plates per side") {
                    if let target = target {
                        if target < bar {
                            Label("Target is less than the bar weight (\(formatPlate(bar)) \(unit.label))",
                                  systemImage: "exclamationmark.circle")
                                .foregroundColor(.red)
                                .font(.caption)
                        } else {
                            let breakdown = calculatePlates(targetTotal: target, bar: bar, inventory: unit.inventory)
                            if breakdown.platesPerSide.isEmpty {
                                Text("Just the bar — no plates needed.")
                                    .font(.subheadline)
                            } else {
                                let grouped = Dictionary(grouping: breakdown.platesPerSide, by: { $0 })
                                    .sorted { $0.key > $1.key }
                                ForEach(grouped, id: \.key) { plate, copies in
                                    HStack {
                                        Text("\(copies.count) ×")
                                            .foregroundColor(.secondary)
                                        Text("\(formatPlate(plate)) \(unit.label)")
                                            .font(.body.bold())
                                    }
                                }
                            }

                            HStack(spacing: 6) {
                                Image(systemName: breakdown.isExact ? "checkmark.circle.fill" : "info.circle.fill")
                                    .foregroundColor(breakdown.isExact ? .accentColor : .orange)
                                Text(breakdown.isExact
                                    ? "Total \(formatPlate(breakdown.achievedTotal)) \(unit.label)"
                                    : "Closest available: \(formatPlate(breakdown.achievedTotal)) \(unit.label)")
                                    .font(.caption)
                                    .foregroundColor(breakdown.isExact ? .accentColor : .orange)
                            }
                        }
                    } else {
                        Text("Enter a target weight to see the plate breakdown.")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
            }
            .navigationTitle("Plate Calculator")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
}
