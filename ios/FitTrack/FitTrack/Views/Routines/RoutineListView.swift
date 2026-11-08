import SwiftUI
import SwiftData

struct RoutineListView: View {
    @Environment(\.modelContext) private var modelContext
    @Query(sort: \Routine.name) private var routines: [Routine]
    @State private var selectedDays: Int? = nil
    @State private var toastMessage: String?
    /// Routine being moved via the folder sheet.
    @State private var folderTarget: Routine?
    @State private var renamingFolder: String?
    @State private var renameText = ""
    @State private var collapsedFolders: Set<String> = []

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

    /// Standalone routines with no folder, listed first.
    var unfiledRoutines: [Routine] { standaloneRoutines.filter { $0.folder == nil } }

    /// Standalone routines grouped by folder, alphabetical.
    var folders: [(String, [Routine])] {
        Dictionary(grouping: standaloneRoutines.filter { $0.folder != nil }) { $0.folder! }
            .sorted { $0.key < $1.key }
    }

    /// Every folder name in use, for the move sheet.
    var allFolderNames: [String] { Array(Set(routines.compactMap(\.folder))).sorted() }

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

                        VStack(spacing: 4) {
                            // Program header
                            HStack {
                                VStack(alignment: .leading) {
                                    Text(programName).font(.headline)
                                    Text("\(daysCount)x/week · \(sorted.first?.difficulty.capitalized ?? "")")
                                        .font(.caption).opacity(0.6)
                                }
                                Spacer()
                                Button("Add to Plan") { addProgramToPlan(programName, sorted) }
                                    .font(.caption.bold())
                                    .buttonStyle(.borderedProminent)
                                    .controlSize(.small)
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
                                    .background(Color.fitSurfaceLow)
                                    .cornerRadius(8)
                                }
                                .buttonStyle(.plain)
                                .padding(.leading, 16)
                            }
                        }
                        .padding(.horizontal)
                    }

                    // Standalone: unfiled first, then one collapsible group per folder.
                    if !standaloneRoutines.isEmpty {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Single Day Routines").font(.headline).padding(.horizontal)

                            ForEach(unfiledRoutines, id: \.self) { routine in
                                standaloneRow(routine)
                                    .padding(.horizontal)
                            }

                            ForEach(folders, id: \.0) { name, items in
                                folderHeader(name, count: items.count)
                                    .padding(.horizontal)
                                if !collapsedFolders.contains(name) {
                                    ForEach(items, id: \.self) { routine in
                                        standaloneRow(routine)
                                            .padding(.leading, 32)
                                            .padding(.trailing)
                                    }
                                }
                            }
                        }
                    }
                }
                .padding(.bottom, 16)
            }
            .scrollContentBackground(.hidden)
            .background(Color.fitBackground)
            .navigationTitle("Routines")
            .sheet(item: $folderTarget) { routine in
                RoutineFolderSheet(routine: routine, existingFolders: allFolderNames)
                    .presentationDetents([.medium, .large])
            }
            .alert("Rename folder", isPresented: Binding(
                get: { renamingFolder != nil },
                set: { if !$0 { renamingFolder = nil } }
            )) {
                TextField("Folder name", text: $renameText)
                Button("Rename") { renameFolder() }
                Button("Cancel", role: .cancel) { renamingFolder = nil }
            }
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

    // MARK: - Rows

    private func standaloneRow(_ routine: Routine) -> some View {
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
            .background(Color.fitSurfaceLow)
            .cornerRadius(12)
        }
        .buttonStyle(.plain)
        .contextMenu {
            Button { folderTarget = routine } label: {
                Label("Move to folder…", systemImage: "folder")
            }
            if routine.folder != nil {
                Button {
                    routine.folder = nil
                    try? modelContext.save()
                } label: {
                    Label("Remove from folder", systemImage: "folder.badge.minus")
                }
            }
        }
    }

    private func folderHeader(_ name: String, count: Int) -> some View {
        HStack(spacing: 8) {
            Button {
                withAnimation(.easeInOut(duration: 0.2)) {
                    if collapsedFolders.contains(name) { collapsedFolders.remove(name) } else { collapsedFolders.insert(name) }
                }
            } label: {
                HStack(spacing: 8) {
                    Image(systemName: collapsedFolders.contains(name) ? "chevron.right" : "chevron.down")
                        .font(.caption2.weight(.bold))
                        .foregroundStyle(Color.fitOnSurfaceVariant)
                    Image(systemName: "folder.fill").foregroundStyle(Color.fitPrimary)
                    Text(name).font(.subheadline.weight(.semibold))
                    Text("\(count)").font(.caption).foregroundStyle(Color.fitOnSurfaceVariant)
                    Spacer()
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            Menu {
                Button {
                    renameText = name
                    renamingFolder = name
                } label: { Label("Rename folder", systemImage: "pencil") }
            } label: {
                Image(systemName: "ellipsis.circle").foregroundStyle(Color.fitOnSurfaceVariant)
            }
        }
        .padding(.vertical, 6)
    }

    // MARK: - Actions

    private func renameFolder() {
        guard let old = renamingFolder else { return }
        let new = renameText.trimmingCharacters(in: .whitespacesAndNewlines)
        renamingFolder = nil
        guard !new.isEmpty, new != old else { return }
        for routine in routines where routine.folder == old {
            routine.folder = new
        }
        try? modelContext.save()
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
                .background(selected ? Color.accentColor : Color.fitSurfaceLow)
                .foregroundColor(selected ? .white : .primary)
                .cornerRadius(20)
        }
    }
}
