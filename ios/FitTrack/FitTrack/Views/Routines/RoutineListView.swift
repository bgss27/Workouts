import SwiftUI
import SwiftData

struct RoutineListView: View {
    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject var proManager: ProManager
    @Query(sort: \Routine.name) private var routines: [Routine]
    @State private var selectedDays: Int? = nil
    @State private var toastMessage: String?

    var filteredRoutines: [Routine] {
        routines.filter { r in
            guard let days = selectedDays else { return true }
            return r.daysPerWeek == days
        }
    }

    var programRoutines: [(String, [Routine])] {
        let grouped = Dictionary(grouping: filteredRoutines.filter { $0.programName != nil }) { $0.programName! }
        return grouped.sorted { $0.key < $1.key }
    }

    var standaloneRoutines: [Routine] {
        filteredRoutines.filter { $0.programName == nil }
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    // Days filter
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            FilterChipView(label: "All", selected: selectedDays == nil) { selectedDays = nil }
                            FilterChipView(label: "3-Day", selected: selectedDays == 3) { selectedDays = selectedDays == 3 ? nil : 3 }
                            FilterChipView(label: "5-Day", selected: selectedDays == 5) { selectedDays = selectedDays == 5 ? nil : 5 }
                            FilterChipView(label: "Single Day", selected: selectedDays == 0) { selectedDays = selectedDays == 0 ? nil : 0 }
                        }
                        .padding(.horizontal)
                    }

                    // Programs
                    ForEach(programRoutines, id: \.0) { programName, days in
                        let sorted = days.sorted { $0.dayOrder < $1.dayOrder }
                        let daysCount = sorted.first?.daysPerWeek ?? 0
                        let canAccess = proManager.canAccessProgram(daysPerWeek: daysCount)

                        VStack(spacing: 4) {
                            // Program header
                            HStack {
                                VStack(alignment: .leading) {
                                    Text(programName).font(.headline)
                                    Text("\(daysCount)x/week · \(sorted.first?.difficulty.capitalized ?? "")")
                                        .font(.caption).opacity(0.6)
                                }
                                Spacer()
                                if canAccess {
                                    Button("Add to Plan") { addProgramToPlan(programName, sorted) }
                                        .font(.caption.bold())
                                        .buttonStyle(.borderedProminent)
                                        .controlSize(.small)
                                } else {
                                    Button { } label: {
                                        Label("PRO", systemImage: "lock.fill")
                                    }
                                    .font(.caption.bold())
                                    .buttonStyle(.bordered)
                                    .controlSize(.small)
                                }
                            }
                            .padding()
                            .background(Color.accentColor.opacity(0.1))
                            .cornerRadius(12)

                            // Days
                            ForEach(sorted, id: \.self) { routine in
                                NavigationLink(destination: RoutineDetailView(routine: routine)) {
                                    HStack {
                                        VStack(alignment: .leading) {
                                            Text(routine.name).font(.subheadline.bold())
                                            Text(routine.routineDescription).font(.caption).opacity(0.6)
                                        }
                                        Spacer()
                                        Text("\(routine.exercises.count) ex").font(.caption).foregroundColor(.accentColor)
                                    }
                                    .padding(12)
                                    .background(Color(.secondarySystemBackground))
                                    .cornerRadius(8)
                                }
                                .buttonStyle(.plain)
                                .padding(.leading, 16)
                            }
                        }
                        .padding(.horizontal)
                    }

                    // Standalone
                    if !standaloneRoutines.isEmpty {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Single Day Routines").font(.headline).padding(.horizontal)

                            ForEach(standaloneRoutines, id: \.self) { routine in
                                NavigationLink(destination: RoutineDetailView(routine: routine)) {
                                    HStack {
                                        VStack(alignment: .leading) {
                                            Text(routine.name).font(.subheadline.bold())
                                            Text(routine.routineDescription).font(.caption).opacity(0.6)
                                            Text("\(routine.exercises.count) exercises").font(.caption2).foregroundColor(.accentColor)
                                        }
                                        Spacer()
                                        Button {
                                            addStandaloneRoutineToPlan(routine)
                                        } label: {
                                            Image(systemName: "plus").foregroundColor(.accentColor)
                                        }
                                    }
                                    .padding()
                                    .background(Color(.secondarySystemBackground))
                                    .cornerRadius(12)
                                }
                                .buttonStyle(.plain)
                                .padding(.horizontal)
                            }
                        }
                    }
                }
                .padding(.bottom, 16)
            }
            .navigationTitle("Routines")
            .overlay(alignment: .bottom) {
                if let msg = toastMessage {
                    Text(msg).font(.caption.bold()).padding(12)
                        .background(.ultraThinMaterial).cornerRadius(20)
                        .padding(.bottom, 20)
                        .onAppear {
                            DispatchQueue.main.asyncAfter(deadline: .now() + 2) { toastMessage = nil }
                        }
                }
            }
        }
    }

    private func addProgramToPlan(_ name: String, _ routines: [Routine]) {
        // Clear existing plan
        let descriptor = FetchDescriptor<UserPlan>()
        if let existing = try? modelContext.fetch(descriptor) {
            existing.forEach { modelContext.delete($0) }
        }
        for routine in routines {
            let plan = UserPlan(routine: routine, programName: name, dayOrder: routine.dayOrder)
            modelContext.insert(plan)
        }
        try? modelContext.save()
        toastMessage = "\(name) added to My Plan"
    }

    private func addStandaloneRoutineToPlan(_ routine: Routine) {
        let plan = UserPlan(routine: routine)
        modelContext.insert(plan)
        try? modelContext.save()
        toastMessage = "\(routine.name) added to My Plan"
    }
}

struct FilterChipView: View {
    let label: String
    let selected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(label).font(.caption.bold())
                .padding(.horizontal, 14).padding(.vertical, 8)
                .background(selected ? Color.accentColor : Color(.secondarySystemBackground))
                .foregroundColor(selected ? .white : .primary)
                .cornerRadius(20)
        }
    }
}
