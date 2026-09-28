import SwiftUI
import SwiftData

/// Manually log a workout that already happened — for when the user trained
/// without opening the app and wants to record it after the fact. Mirrors
/// the data model of `ActiveWorkoutView` (a `Workout` with exercises and
/// sets), but with explicit date/time pickers instead of a running timer.
///
/// Entry points:
///   * Calendar tab → tap any past or today day with no workout → "Add workout"
///   * Home tab → "Log Past Workout" toolbar action
///
/// Saved workouts behave like normal completed workouts everywhere else
/// (history, calendar, PR detection, charts, HealthKit export).
struct LogPastWorkoutView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    @AppStorage("profile_weightUnit") private var weightUnit = "kg"
    @Query(sort: \Exercise.name) private var allExercises: [Exercise]

    let initialDate: Date?

    @State private var workoutDate: Date
    @State private var startTime: Date
    @State private var endTime: Date
    @State private var notes: String = ""
    @State private var entries: [ExerciseEntry] = []
    @State private var showExercisePicker = false
    @State private var pickerSearchText = ""
    @State private var pickerGroup: MuscleGroup?
    @State private var saveError: String?

    struct ExerciseEntry: Identifiable {
        let id = UUID()
        var exercise: Exercise
        var sets: [SetEntry]
    }

    struct SetEntry: Identifiable {
        let id = UUID()
        var weight: String = ""
        var reps: String = ""
        var isWarmup: Bool = false
    }

    init(initialDate: Date? = nil) {
        self.initialDate = initialDate
        let now = Date()
        let base = initialDate ?? now
        let cal = Calendar.current
        // Default start: if logging today, an hour ago. If logging a past
        // day, default to 6 PM (typical evening workout). User can adjust.
        let defaultStart: Date
        if cal.isDateInToday(base) {
            defaultStart = now.addingTimeInterval(-3600)
        } else {
            defaultStart = cal.date(bySettingHour: 18, minute: 0, second: 0, of: base) ?? base
        }
        let defaultEnd = defaultStart.addingTimeInterval(3600)
        self._workoutDate = State(initialValue: cal.startOfDay(for: base))
        self._startTime = State(initialValue: defaultStart)
        self._endTime = State(initialValue: defaultEnd)
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    DatePicker("Date", selection: $workoutDate, in: ...Date(), displayedComponents: .date)
                    DatePicker("Start", selection: $startTime, displayedComponents: .hourAndMinute)
                    DatePicker("End", selection: $endTime, displayedComponents: .hourAndMinute)
                    Text(durationLabel)
                        .font(.caption)
                        .foregroundColor(.secondary)
                } header: { Text("When") }

                Section {
                    if entries.isEmpty {
                        Text("Add the exercises you did. Tap + below.")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    } else {
                        ForEach($entries) { $entry in
                            ExerciseEntryView(
                                entry: $entry,
                                weightUnit: weightUnit,
                                onDelete: { delete(entry: entry) }
                            )
                        }
                    }
                    Button {
                        showExercisePicker = true
                    } label: {
                        Label("Add Exercise", systemImage: "plus.circle.fill")
                    }
                } header: { Text("Exercises") }

                Section {
                    TextField("How did it go?", text: $notes, axis: .vertical)
                        .lineLimit(2...5)
                } header: { Text("Notes (optional)") }
            }
            .navigationTitle("Log Past Workout")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") { save() }
                        .disabled(!canSave)
                }
            }
            .sheet(isPresented: $showExercisePicker) { exercisePickerSheet }
            .alert("Couldn't save", isPresented: Binding(
                get: { saveError != nil },
                set: { if !$0 { saveError = nil } }
            )) {
                Button("OK", role: .cancel) {}
            } message: {
                Text(saveError ?? "")
            }
        }
    }

    // MARK: - Validity

    private var combinedStart: Date {
        combine(date: workoutDate, withTimeOf: startTime)
    }
    private var combinedEnd: Date {
        var end = combine(date: workoutDate, withTimeOf: endTime)
        // If end picker is "earlier than" start (e.g. workout that spanned
        // midnight — user picks 23:00 → 00:30), bump end forward a day.
        if end < combinedStart { end = end.addingTimeInterval(24 * 60 * 60) }
        return end
    }
    private var durationLabel: String {
        let mins = max(0, Int(combinedEnd.timeIntervalSince(combinedStart) / 60))
        return "Duration: \(mins) min"
    }
    private var canSave: Bool {
        !entries.isEmpty
            && entries.allSatisfy { e in
                // At least one non-warmup set with valid weight + reps per
                // exercise, OR the exercise has at least one warmup set.
                e.sets.contains { s in
                    (Int(s.reps) ?? 0) > 0 && (Double(s.weight) ?? 0) > 0
                }
            }
            && combinedEnd > combinedStart
    }

    // MARK: - Actions

    private func delete(entry: ExerciseEntry) {
        entries.removeAll { $0.id == entry.id }
    }

    private func save() {
        let start = combinedStart
        let end = combinedEnd
        guard end > start else {
            saveError = "End time must be after start time."
            return
        }
        let workout = Workout(startTime: start, endTime: end, notes: notes.isEmpty ? nil : notes)
        modelContext.insert(workout)

        let isLbs = weightUnit == "lbs"
        for (i, entry) in entries.enumerated() {
            let we = WorkoutExercise(exercise: entry.exercise, orderIndex: i)
            we.workout = workout
            workout.exercises.append(we)
            var setNum = 0
            for set in entry.sets {
                guard let reps = Int(set.reps), let typedWeight = Double(set.weight), reps > 0 else { continue }
                if !set.isWarmup { setNum += 1 }
                let weightKg = isLbs ? typedWeight * 0.45359237 : typedWeight
                let ws = WorkoutSet(
                    setNumber: set.isWarmup ? 0 : setNum,
                    reps: reps,
                    weightKg: weightKg,
                    isWarmup: set.isWarmup
                )
                ws.workoutExercise = we
                we.sets.append(ws)
            }
        }
        do {
            try modelContext.save()
            dismiss()
        } catch {
            saveError = error.localizedDescription
        }
    }

    /// Build a Date with [date]'s year/month/day and [time]'s hour/minute.
    private func combine(date: Date, withTimeOf time: Date) -> Date {
        let cal = Calendar.current
        let day = cal.dateComponents([.year, .month, .day], from: date)
        let clock = cal.dateComponents([.hour, .minute], from: time)
        var merged = DateComponents()
        merged.year = day.year; merged.month = day.month; merged.day = day.day
        merged.hour = clock.hour; merged.minute = clock.minute
        return cal.date(from: merged) ?? date
    }

    // MARK: - Exercise picker

    private var exercisePickerSheet: some View {
        NavigationStack {
            List {
                Section {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            LogPastFilterChip(label: "All", selected: pickerGroup == nil) {
                                pickerGroup = nil
                            }
                            ForEach(MuscleGroup.allCases) { g in
                                LogPastFilterChip(label: g.displayName, selected: pickerGroup == g) {
                                    pickerGroup = (pickerGroup == g) ? nil : g
                                }
                            }
                        }
                    }
                }
                let filtered = allExercises.filter { ex in
                    let mg = pickerGroup == nil || ex.muscleGroup == pickerGroup
                    let s = pickerSearchText.isEmpty || ex.name.localizedCaseInsensitiveContains(pickerSearchText)
                    return mg && s
                }
                ForEach(filtered, id: \.self) { exercise in
                    Button {
                        entries.append(ExerciseEntry(exercise: exercise, sets: [SetEntry()]))
                        showExercisePicker = false
                    } label: {
                        VStack(alignment: .leading) {
                            Text(exercise.name).foregroundColor(.primary)
                            Text(exercise.muscleGroup.displayName)
                                .font(.caption).opacity(0.6)
                        }
                    }
                }
            }
            .searchable(text: $pickerSearchText)
            .navigationTitle("Add Exercise")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { showExercisePicker = false }
                }
            }
        }
    }
}

// MARK: - Per-exercise editor

private struct ExerciseEntryView: View {
    @Binding var entry: LogPastWorkoutView.ExerciseEntry
    let weightUnit: String
    let onDelete: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(entry.exercise.name).font(.subheadline.bold())
                    Text(entry.exercise.muscleGroup.displayName)
                        .font(.caption).opacity(0.6)
                }
                Spacer()
                Button(role: .destructive, action: onDelete) {
                    Image(systemName: "trash").font(.caption)
                }
            }
            ForEach($entry.sets) { $set in
                HStack(spacing: 8) {
                    Text(set.isWarmup ? "W" : "\(entry.sets.firstIndex(where: { $0.id == set.id }).map { $0 + 1 } ?? 0)")
                        .frame(width: 20)
                        .font(.caption)
                        .foregroundColor(set.isWarmup ? .teal : .primary)
                    TextField(weightUnit, text: $set.weight)
                        .keyboardType(.decimalPad)
                        .textFieldStyle(.roundedBorder)
                    TextField("Reps", text: $set.reps)
                        .keyboardType(.numberPad)
                        .textFieldStyle(.roundedBorder)
                    Button {
                        set.isWarmup.toggle()
                    } label: {
                        Text("W").foregroundColor(set.isWarmup ? .teal : .gray.opacity(0.3))
                    }
                    .buttonStyle(.plain)
                }
            }
            .onDelete { indices in entry.sets.remove(atOffsets: indices) }
            HStack {
                Button("+ Set") {
                    entry.sets.append(LogPastWorkoutView.SetEntry())
                }
                .font(.caption)
                Spacer()
            }
        }
        .padding(.vertical, 4)
    }
}

// MARK: - Helper chip

private struct LogPastFilterChip: View {
    let label: String
    let selected: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            Text(label)
                .font(.caption)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(selected ? Color.accentColor : Color(.secondarySystemBackground))
                .foregroundColor(selected ? .white : .primary)
                .cornerRadius(14)
        }
        .buttonStyle(.plain)
    }
}
