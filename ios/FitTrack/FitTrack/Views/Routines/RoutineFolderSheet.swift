import SwiftUI
import SwiftData

/// "Move to folder…" for a routine: pick an existing folder, create a new
/// one, or unfile it. Mirrors Android's `RoutineFolderSheet`.
struct RoutineFolderSheet: View {
    let routine: Routine
    let existingFolders: [String]

    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    @State private var newFolder = ""

    private var trimmedNew: String { newFolder.trimmingCharacters(in: .whitespacesAndNewlines) }

    var body: some View {
        NavigationStack {
            List {
                Section("New folder") {
                    HStack {
                        TextField("Folder name", text: $newFolder)
                            .textInputAutocapitalization(.words)
                            .onSubmit { if !trimmedNew.isEmpty { move(to: trimmedNew) } }
                        Button("Create") { move(to: trimmedNew) }
                            .disabled(trimmedNew.isEmpty)
                    }
                }
                if !existingFolders.isEmpty {
                    Section("Folders") {
                        ForEach(existingFolders, id: \.self) { folder in
                            Button { move(to: folder) } label: {
                                HStack {
                                    Label(folder, systemImage: "folder")
                                    Spacer()
                                    if routine.folder == folder {
                                        Image(systemName: "checkmark").foregroundStyle(Color.fitPrimary)
                                    }
                                }
                            }
                            .tint(.primary)
                        }
                    }
                }
                if routine.folder != nil {
                    Section {
                        Button("Remove from folder", role: .destructive) { move(to: nil) }
                    }
                }
            }
            .scrollContentBackground(.hidden)
            .background(Color.fitBackground)
            .navigationTitle("Move \(routine.name)")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
            }
        }
    }

    private func move(to folder: String?) {
        let cleaned = folder?.trimmingCharacters(in: .whitespacesAndNewlines)
        routine.folder = (cleaned?.isEmpty ?? true) ? nil : cleaned
        try? modelContext.save()
        dismiss()
    }
}
