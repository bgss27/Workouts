import SwiftUI

struct RoutineDetailView: View {
    let routine: Routine

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
            }

            Section("Exercises") {
                ForEach(routine.exercises.sorted { $0.orderIndex < $1.orderIndex }, id: \.self) { re in
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
                }
            }
        }
        .navigationTitle(routine.name)
        .toolbar {
            NavigationLink(destination: ActiveWorkoutView(routine: routine)) {
                Label("Start", systemImage: "play.fill")
            }
        }
    }
}
