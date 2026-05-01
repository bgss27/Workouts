import SwiftUI
import SwiftData

struct ActiveWorkoutView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    @Query(sort: \Exercise.name) private var allExercises: [Exercise]

    let routine: Routine?
    @State private var workout: Workout?
    @State private var exerciseEntries: [ExerciseEntry] = []
    @State private var showExercisePicker = false
    @State private var showFinishAlert = false
    @State private var showDiscardAlert = false
    @State private var searchText = ""
    @State private var selectedGroup: MuscleGroup?

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

    private let guidance = TimeOfDayAdvisor.getGuidance()
    @State private var showTips = false

    var body: some View {
        NavigationStack {
            List {
                // Time-of-day guidance banner
                Section {
                    DisclosureGroup(isExpanded: $showTips) {
                        Text(guidance.warmupAdvice.description)
                            .font(.caption)
                            .foregroundColor(.secondary)
                            .padding(.bottom, 4)

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
                    } label: {
                        HStack(spacing: 8) {
                            Image(systemName: guidance.timeOfDay.icon)
                                .foregroundColor(.accentColor)
                            Text(guidance.tip)
                                .font(.subheadline.bold())
                        }
                    }
                }

                ForEach($exerciseEntries) { $entry in
                    Section {
                        ForEach($entry.sets) { $set in
                            HStack {
                                Text(set.isWarmup ? "W" : "\(entry.sets.firstIndex(where: { $0.id == set.id }).map { $0 + 1 } ?? 0)")
                                    .frame(width: 24)
                                    .foregroundColor(set.isWarmup ? .teal : .primary)
                                TextField("kg", text: $set.weight)
                                    .keyboardType(.decimalPad)
                                    .textFieldStyle(.roundedBorder)
                                TextField("Reps", text: $set.reps)
                                    .keyboardType(.numberPad)
                                    .textFieldStyle(.roundedBorder)
                                Button { set.isWarmup.toggle() } label: {
                                    Text("W").foregroundColor(set.isWarmup ? .teal : .gray.opacity(0.3))
                                }
                            }
                        }
                        .onDelete { indices in entry.sets.remove(atOffsets: indices) }

                        Button("Add Set") {
                            entry.sets.append(SetEntry())
                        }
                    } header: {
                        HStack {
                            VStack(alignment: .leading) {
                                Text(entry.exercise.name).font(.subheadline.bold())
                                Text(entry.exercise.muscleGroup.displayName).font(.caption).opacity(0.6)
                            }
                            Spacer()
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
                ToolbarItem(placement: .confirmationAction) {
                    Button("Finish") { showFinishAlert = true }
                }
            }
            .onAppear { startWorkout() }
            .sheet(isPresented: $showExercisePicker) { exercisePickerSheet }
            .alert("Finish Workout?", isPresented: $showFinishAlert) {
                Button("Save") { finishWorkout() }
                Button("Cancel", role: .cancel) {}
            } message: { Text("Save \(exerciseEntries.count) exercise(s)?") }
            .alert("Discard Workout?", isPresented: $showDiscardAlert) {
                Button("Discard", role: .destructive) { discardWorkout() }
                Button("Keep Going", role: .cancel) {}
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
                        exerciseEntries.append(ExerciseEntry(exercise: exercise, sets: [SetEntry()]))
                        showExercisePicker = false
                    } label: {
                        VStack(alignment: .leading) {
                            Text(exercise.name)
                            Text(exercise.muscleGroup.displayName).font(.caption).opacity(0.6)
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

        if let routine {
            for re in routine.exercises.sorted(by: { $0.orderIndex < $1.orderIndex }) {
                guard let ex = re.exercise else { continue }
                exerciseEntries.append(ExerciseEntry(exercise: ex, sets: [SetEntry()]))
            }
        }
    }

    private func finishWorkout() {
        guard let workout else { return }
        for (i, entry) in exerciseEntries.enumerated() {
            let we = WorkoutExercise(exercise: entry.exercise, orderIndex: i)
            we.workout = workout
            workout.exercises.append(we)
            var setNum = 0
            for set in entry.sets {
                guard let reps = Int(set.reps), let weight = Double(set.weight), reps > 0 else { continue }
                if !set.isWarmup { setNum += 1 }
                let ws = WorkoutSet(setNumber: set.isWarmup ? 0 : setNum, reps: reps, weightKg: weight, isWarmup: set.isWarmup)
                ws.workoutExercise = we
                we.sets.append(ws)
            }
        }
        workout.endTime = Date()
        try? modelContext.save()
        dismiss()
    }

    private func discardWorkout() {
        if let workout { modelContext.delete(workout) }
        dismiss()
    }
}
