import SwiftUI
import SwiftData

struct RoutineDetailView: View {
    let routine: Routine
    @Environment(\.modelContext) private var modelContext
    @AppStorage(RestTimerNotifier.defaultRestKey) private var defaultRestSeconds = 90

    var body: some View {
        List {
            Section {
                Text(routine.routineDescription)
                HStack {
                    Label(routine.difficulty.capitalized, systemImage: "speedometer")
                    Spacer()
                    Label("\(routine.exercises.count) exercises", systemImage: "dumbbell.fill")
                }
                .font(.caption)
                if routine.daysPerWeek > 0 {
                    Label("\(routine.daysPerWeek)x/week", systemImage: "calendar")
                        .font(.caption)
                }
                if let program = routine.programName {
                    Label("Part of: \(program)", systemImage: "folder")
                        .font(.caption).foregroundColor(.secondary)
                }
                if let folder = routine.folder {
                    Label("Folder: \(folder)", systemImage: "folder.fill")
                        .font(.caption).foregroundColor(.secondary)
                }
            }

            Section {
                ForEach(routine.exercises.sorted { $0.orderIndex < $1.orderIndex }, id: \.self) { re in
                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            VStack(alignment: .leading) {
                                Text(re.exercise?.name ?? "Unknown").font(.subheadline)
                                Text(re.exercise?.muscleGroup.displayName ?? "").font(.caption).opacity(0.6)
                            }
                            Spacer()
                            VStack(alignment: .trailing) {
                                Text("\(re.suggestedSets) sets").font(.subheadline).foregroundColor(.accentColor)
                                Text("\(re.suggestedReps) reps").font(.caption)
                            }
                        }
                        HStack(spacing: 8) {
                            RestDurationPicker(seconds: restBinding(re), defaultSeconds: defaultRestSeconds > 0 ? defaultRestSeconds : 90)
                            if let note = re.exercise?.notes {
                                Label(note, systemImage: "note.text")
                                    .font(.caption)
                                    .foregroundStyle(Color.fitOnSurfaceVariant)
                                    .lineLimit(1)
                            }
                        }
                    }
                    .padding(.vertical, 2)
                }
            } header: {
                Text("Exercises")
            } footer: {
                Text("Rest applies after each set of that exercise. \"Default\" follows the length in Settings.")
            }
        }
        .scrollContentBackground(.hidden)
        .background(Color.fitBackground)
        .navigationTitle(routine.name)
        .toolbar {
            NavigationLink(destination: ActiveWorkoutView(routine: routine)) {
                Label("Start", systemImage: "play.fill")
            }
        }
    }

    /// Writes straight through to the routine so the next workout picks it up.
    private func restBinding(_ re: RoutineExercise) -> Binding<Int?> {
        Binding(
            get: { re.restSeconds },
            set: { newValue in
                re.restSeconds = newValue
                try? modelContext.save()
            }
        )
    }
}
