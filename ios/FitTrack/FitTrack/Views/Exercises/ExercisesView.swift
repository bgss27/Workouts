import SwiftUI
import SwiftData

struct ExercisesView: View {
    @Query(sort: \Exercise.name) private var allExercises: [Exercise]
    @State private var searchText = ""
    @State private var selectedGroup: MuscleGroup?

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
                                ExerciseRow(exercise: exercise)
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
                        ExerciseRow(exercise: exercise)
                    }
                }
            }
            .searchable(text: $searchText, prompt: "Search exercises")
            .navigationTitle("Exercises")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
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
                        Text(exercise.muscleGroup.displayName)
                            .font(.caption)
                            .foregroundColor(.accentColor)
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
