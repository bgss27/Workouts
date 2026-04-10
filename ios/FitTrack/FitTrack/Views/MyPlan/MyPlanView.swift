import SwiftUI
import SwiftData

struct MyPlanView: View {
    @Environment(\.modelContext) private var modelContext
    @Query(sort: \UserPlan.dayOrder) private var plans: [UserPlan]
    @State private var showClearAlert = false

    var body: some View {
        NavigationStack {
            if plans.isEmpty {
                VStack(spacing: 16) {
                    Image(systemName: "calendar").font(.system(size: 64)).opacity(0.3)
                    Text("No plan selected").font(.title3.bold())
                    Text("Browse routines and add a program").font(.caption).opacity(0.6)
                    NavigationLink("Browse Routines") { RoutineListView() }
                        .buttonStyle(.borderedProminent)
                }
                .padding()
                .navigationTitle("My Plan")
            } else {
                List {
                    if let program = plans.first?.programName {
                        Section {
                            HStack {
                                Image(systemName: "calendar")
                                Text(program).font(.headline)
                            }
                            Text("\(plans.count) workouts in your plan")
                                .font(.caption).opacity(0.6)
                        }
                    }

                    ForEach(plans, id: \.self) { plan in
                        Section {
                            VStack(alignment: .leading, spacing: 8) {
                                HStack {
                                    VStack(alignment: .leading) {
                                        if plan.programName != nil {
                                            Text("Day \(plan.dayOrder)").font(.caption).foregroundColor(.accentColor)
                                        }
                                        Text(plan.routine?.name ?? "Workout").font(.headline)
                                        Text(plan.routine?.routineDescription ?? "").font(.caption).opacity(0.6)
                                    }
                                    Spacer()
                                    NavigationLink(destination: ActiveWorkoutView(routine: plan.routine)) {
                                        Label("Start", systemImage: "play.fill")
                                            .font(.caption.bold())
                                    }
                                    .buttonStyle(.borderedProminent)
                                    .controlSize(.small)
                                }

                                if let routine = plan.routine {
                                    ForEach(routine.exercises.sorted { $0.orderIndex < $1.orderIndex }, id: \.self) { re in
                                        HStack {
                                            Text("\(re.orderIndex + 1).").font(.caption).opacity(0.5)
                                            Text(re.exercise?.name ?? "").font(.caption)
                                            Spacer()
                                            Text("\(re.suggestedSets)x\(re.suggestedReps)")
                                                .font(.caption2).foregroundColor(.accentColor)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Section {
                        NavigationLink("Change Plan") { RoutineListView() }
                        Button("Clear Plan", role: .destructive) { showClearAlert = true }
                    }
                }
                .navigationTitle("My Plan")
                .alert("Clear Plan?", isPresented: $showClearAlert) {
                    Button("Clear", role: .destructive) { clearPlan() }
                    Button("Cancel", role: .cancel) {}
                }
            }
        }
    }

    private func clearPlan() {
        plans.forEach { modelContext.delete($0) }
        try? modelContext.save()
    }
}
