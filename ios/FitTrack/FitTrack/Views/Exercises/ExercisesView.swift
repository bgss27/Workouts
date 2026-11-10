import SwiftUI
import SwiftData

struct ExercisesView: View {
    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject private var proManager: ProManager
    @Query(sort: \Exercise.name) private var allExercises: [Exercise]
    @State private var searchText = ""
    @State private var selectedGroup: MuscleGroup?
    @State private var showCustomSheet = false
    @State private var editingCustom: Exercise?
    /// Exercise whose carry-over note is being edited.
    @State private var noteTarget: Exercise?
    @State private var deleteBlockedName: String?

    private var customCount: Int { allExercises.filter(\.isCustom).count }

    private var filteredExercises: [Exercise] {
        allExercises.filter { ex in
            let matchesGroup = selectedGroup == nil || ex.muscleGroup == selectedGroup
            let matchesSearch = searchText.isEmpty || ex.name.localizedCaseInsensitiveContains(searchText)
            return matchesGroup && matchesSearch
        }
    }

    private var groupedExercises: [(MuscleGroup, [Exercise])] {
        Dictionary(grouping: filteredExercises) { $0.muscleGroup }
            .sorted { $0.key.displayName < $1.key.displayName }
    }

    private var showGrouped: Bool {
        selectedGroup == nil && searchText.isEmpty
    }

    var body: some View {
        NavigationStack {
            List {
                if showGrouped {
                    ForEach(groupedExercises, id: \.0) { group, exercises in
                        Section {
                            ForEach(exercises.sorted { $0.name < $1.name }, id: \.self) { exercise in
                                customAwareRow(exercise)
                            }
                        } header: {
                            HStack {
                                Image(systemName: "dumbbell.fill")
                                    .foregroundColor(.accentColor)
                                Text(group.displayName)
                                    .font(.headline)
                                Spacer()
                                Text("\(exercises.count)")
                                    .font(.caption)
                                    .padding(.horizontal, 8)
                                    .padding(.vertical, 2)
                                    .background(Color.accentColor.opacity(0.15))
                                    .cornerRadius(10)
                            }
                        }
                    }
                } else {
                    Section {
                        Text("\(filteredExercises.count) exercises")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    ForEach(filteredExercises.sorted { $0.name < $1.name }, id: \.self) { exercise in
                        customAwareRow(exercise)
                    }
                }
            }
            .scrollContentBackground(.hidden)
            .background(Color.fitBackground)
            .searchable(text: $searchText, prompt: "Search exercises")
            .navigationTitle("Exercises")
            .sheet(isPresented: $showCustomSheet) {
                CustomExerciseSheet(
                    existing: nil,
                    freeLimit: ProManager.FreeTier.customExerciseLimit,
                    isPro: proManager.isPro,
                    currentCustomCount: customCount
                ) { _ in }
            }
            .sheet(item: $editingCustom) { exercise in
                CustomExerciseSheet(
                    existing: exercise,
                    freeLimit: ProManager.FreeTier.customExerciseLimit,
                    isPro: proManager.isPro,
                    currentCustomCount: customCount
                ) { _ in }
            }
            .sheet(item: $noteTarget) { exercise in
                ExerciseNoteSheet(exerciseName: exercise.name, text: exercise.notes ?? "") { note in
                    exercise.notes = note
                    try? modelContext.save()
                }
            }
            .alert("Can't delete \(deleteBlockedName ?? "")", isPresented: Binding(
                get: { deleteBlockedName != nil },
                set: { if !$0 { deleteBlockedName = nil } }
            )) {
                Button("OK", role: .cancel) {}
            } message: {
                Text("It's used in a logged workout or a routine. Remove it there first.")
            }
            .toolbar {
                ToolbarItemGroup(placement: .topBarTrailing) {
                    Button { showCustomSheet = true } label: {
                        Image(systemName: "plus")
                    }
                    .accessibilityLabel("New custom exercise")
                    Menu {
                        Button("All") { selectedGroup = nil }
                        Divider()
                        ForEach(MuscleGroup.allCases) { group in
                            Button {
                                selectedGroup = selectedGroup == group ? nil : group
                            } label: {
                                if selectedGroup == group {
                                    Label(group.displayName, systemImage: "checkmark")
                                } else {
                                    Text(group.displayName)
                                }
                            }
                        }
                    } label: {
                        Label(
                            selectedGroup?.displayName ?? "Filter",
                            systemImage: selectedGroup != nil ? "line.3.horizontal.decrease.circle.fill" : "line.3.horizontal.decrease.circle"
                        )
                    }
                }
            }
        }
    }
}

extension ExercisesView {
    /// Catalog rows are plain; custom rows get edit / delete in a context menu.
    @ViewBuilder
    fileprivate func customAwareRow(_ exercise: Exercise) -> some View {
        ExerciseRow(exercise: exercise)
            .contextMenu {
                Button { noteTarget = exercise } label: {
                    Label(exercise.notes == nil ? "Add note" : "Edit note", systemImage: "note.text")
                }
                Menu {
                    ForEach(TrackingMode.allCases) { mode in
                        Button {
                            exercise.trackingMode = mode
                            try? modelContext.save()
                        } label: {
                            if exercise.trackingMode == mode {
                                Label(mode.displayName, systemImage: "checkmark")
                            } else {
                                Text(mode.displayName)
                            }
                        }
                    }
                } label: {
                    Label("Tracking: \(exercise.trackingMode.displayName)", systemImage: exercise.trackingMode.symbol ?? "scalemass")
                }
                if exercise.isCustom {
                    Button { editingCustom = exercise } label: { Label("Edit", systemImage: "pencil") }
                    Button(role: .destructive) { deleteCustom(exercise) } label: { Label("Delete", systemImage: "trash") }
                }
            }
    }

    /// Refuse to delete an exercise that history or a routine still points at,
    /// so logged sets never lose their name.
    fileprivate func deleteCustom(_ exercise: Exercise) {
        let id = exercise.persistentModelID
        let usedInWorkouts = ((try? modelContext.fetch(FetchDescriptor<WorkoutExercise>())) ?? [])
            .contains { $0.exercise?.persistentModelID == id }
        let usedInRoutines = ((try? modelContext.fetch(FetchDescriptor<RoutineExercise>())) ?? [])
            .contains { $0.exercise?.persistentModelID == id }
        if usedInWorkouts || usedInRoutines {
            deleteBlockedName = exercise.name
            return
        }
        modelContext.delete(exercise)
        try? modelContext.save()
    }
}

struct ExerciseRow: View {
    let exercise: Exercise
    @State private var expanded = false

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Button { withAnimation { expanded.toggle() } } label: {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(exercise.name)
                            .font(.subheadline.bold())
                            .foregroundColor(.primary)
                        HStack(spacing: 6) {
                            Text(exercise.muscleGroup.displayName)
                                .font(.caption)
                                .foregroundColor(.accentColor)
                            if let symbol = exercise.trackingMode.symbol {
                                Label(exercise.trackingMode.displayName, systemImage: symbol)
                                    .font(.caption2)
                                    .foregroundStyle(Color.fitOnSurfaceVariant)
                            }
                            if exercise.isCustom {
                                Text("CUSTOM")
                                    .font(.caption2.weight(.heavy))
                                    .padding(.horizontal, 5).padding(.vertical, 1)
                                    .background(Color.fitSecondary.opacity(0.2))
                                    .foregroundStyle(Color.fitSecondary)
                                    .clipShape(RoundedRectangle(cornerRadius: 4))
                            }
                        }
                    }
                    Spacer()
                    Image(systemName: expanded ? "chevron.up" : "chevron.down")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            .buttonStyle(.plain)

            if expanded {
                let guide = ExerciseGuideData.getGuide(exercise.name)

                VStack(alignment: .leading, spacing: 10) {
                    Divider()
                        .padding(.vertical, 6)

                    // Exercise images from wger API
                    ExerciseImageGalleryView(exerciseName: exercise.name)
                        .frame(height: 200)
                        .cornerRadius(10)

                    // Muscles
                    HStack(spacing: 8) {
                        Circle().fill(Color.accentColor).frame(width: 10, height: 10)
                        Text("Primary:").font(.caption.bold())
                        Text(exercise.muscleGroup.displayName).font(.caption)
                    }
                    HStack(spacing: 8) {
                        Circle().fill(Color.teal).frame(width: 10, height: 10)
                        Text("Secondary:").font(.caption.bold())
                        Text(exercise.secondaryMuscleGroup?.displayName ?? "None")
                            .font(.caption).foregroundColor(exercise.secondaryMuscleGroup != nil ? .primary : .secondary)
                    }
                    HStack(spacing: 8) {
                        Image(systemName: "info.circle.fill").font(.caption2).foregroundColor(.secondary)
                        Text("Type:").font(.caption.bold())
                        Text(exercise.secondaryMuscleGroup != nil ? "Compound" : "Isolation").font(.caption)
                    }

                    // How to perform
                    Text("How to Perform").font(.caption.bold())
                    ForEach(Array(guide.steps.enumerated()), id: \.0) { i, step in
                        HStack(alignment: .top, spacing: 4) {
                            Text("\(i + 1).").font(.caption.bold()).foregroundColor(.accentColor)
                            Text(step).font(.caption)
                        }
                    }

                    // Breathing
                    HStack(spacing: 6) {
                        Image(systemName: "wind").font(.caption2).foregroundColor(.teal)
                        Text("Breathing:").font(.caption.bold())
                    }
                    Text(guide.breathingCue).font(.caption).padding(.leading, 20)

                    // Tempo
                    HStack(spacing: 6) {
                        Image(systemName: "timer").font(.caption2).foregroundColor(.orange)
                        Text("Tempo:").font(.caption.bold())
                        Text(guide.tempo).font(.caption)
                        if guide.tempo.contains("-") {
                            Text("(ecc-pause-con)").font(.caption2).foregroundColor(.secondary)
                        }
                    }

                    // Common mistakes
                    Text("Common Mistakes").font(.caption.bold()).foregroundColor(.red)
                    ForEach(guide.commonMistakes, id: \.self) { mistake in
                        HStack(alignment: .top, spacing: 4) {
                            Text("✗").font(.caption).foregroundColor(.red)
                            Text(mistake).font(.caption)
                        }
                    }

                    // Muscles worked chips
                    HStack(spacing: 6) {
                        Text("Muscles:").font(.caption.bold())
                        Text(exercise.muscleGroup.displayName)
                            .font(.caption2.bold())
                            .padding(.horizontal, 8).padding(.vertical, 3)
                            .background(Color.accentColor.opacity(0.15)).cornerRadius(8)
                        if let secondary = exercise.secondaryMuscleGroup {
                            Text(secondary.displayName)
                                .font(.caption2.bold())
                                .padding(.horizontal, 8).padding(.vertical, 3)
                                .background(Color.teal.opacity(0.15)).cornerRadius(8)
                        }
                    }
                }
                .padding(.top, 4)
            }
        }
        .padding(.vertical, 4)
    }
}
