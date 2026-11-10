import SwiftUI
import SwiftData

/// Create or edit a user-defined exercise. Free tier can hold a handful;
/// beyond that the sheet points at Pro. Mirrors Android's
/// `CustomExerciseSheet`.
struct CustomExerciseSheet: View {
    /// nil creates a new exercise.
    let existing: Exercise?
    /// How many custom exercises the free tier may hold.
    let freeLimit: Int
    let isPro: Bool
    let currentCustomCount: Int
    let onSave: (Exercise) -> Void

    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var modelContext
    @State private var name = ""
    @State private var muscleGroup: MuscleGroup = .chest
    @State private var secondary: MuscleGroup?
    @State private var equipment: Equipment = .barbell
    @State private var trackingMode: TrackingMode = .weightReps
    @State private var duplicateName = false
    @State private var notes = ""

    private var atLimit: Bool { existing == nil && !isPro && currentCustomCount >= freeLimit }
    private var trimmedName: String { name.trimmingCharacters(in: .whitespacesAndNewlines) }
    private var trimmedNotes: String? {
        let t = notes.trimmingCharacters(in: .whitespacesAndNewlines)
        return t.isEmpty ? nil : t
    }

    var body: some View {
        NavigationStack {
            Form {
                if atLimit {
                    Section {
                        VStack(alignment: .leading, spacing: 8) {
                            Label("Free plan holds \(freeLimit) custom exercises", systemImage: "lock.fill")
                                .font(.subheadline.weight(.semibold))
                            Text("Upgrade to Pro for unlimited custom exercises.")
                                .font(.caption).foregroundStyle(.secondary)
                            NavigationLink { UpgradeView() } label: {
                                Text("See Pro")
                            }
                            .buttonStyle(PrimaryButtonStyle())
                        }
                        .padding(.vertical, 4)
                    }
                }
                Section("Exercise") {
                    TextField("Name", text: $name)
                        .textInputAutocapitalization(.words)
                        .onChange(of: name) { _, _ in duplicateName = false }
                    if duplicateName {
                        Text("An exercise with this name already exists.")
                            .font(.caption).foregroundStyle(Color.fitTertiary)
                    }
                    Picker("Primary muscle", selection: $muscleGroup) {
                        ForEach(MuscleGroup.allCases) { Text($0.displayName).tag($0) }
                    }
                    Picker("Secondary muscle", selection: $secondary) {
                        Text("None").tag(MuscleGroup?.none)
                        ForEach(MuscleGroup.allCases) { Text($0.displayName).tag(MuscleGroup?.some($0)) }
                    }
                    Picker("Equipment", selection: $equipment) {
                        ForEach(Equipment.allCases, id: \.self) { Text($0.displayName).tag($0) }
                    }
                }
                Section {
                    Picker("Tracks", selection: $trackingMode) {
                        ForEach(TrackingMode.allCases) { Text($0.displayName).tag($0) }
                    }
                } footer: {
                    Text(trackingMode.example)
                }
                Section {
                    TextField("e.g. seat 4, grip at the rings", text: $notes, axis: .vertical)
                        .lineLimit(2...6)
                } header: {
                    Text("Note")
                } footer: {
                    Text("Shown every time this exercise comes up in a workout.")
                }
            }
            .scrollContentBackground(.hidden)
            .background(Color.fitBackground)
            .navigationTitle(existing == nil ? "New exercise" : "Edit exercise")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") { save() }
                        .disabled(trimmedName.isEmpty || atLimit)
                }
            }
            .onAppear {
                guard let existing else { return }
                name = existing.name
                muscleGroup = existing.muscleGroup
                secondary = existing.secondaryMuscleGroup
                equipment = existing.equipment
                trackingMode = existing.trackingMode
                notes = existing.notes ?? ""
            }
        }
    }

    private func save() {
        let target = trimmedName
        // Refuse a name that collides with any existing exercise (other than
        // the one being edited) so history lookups by name stay unambiguous.
        let all = (try? modelContext.fetch(FetchDescriptor<Exercise>())) ?? []
        if all.contains(where: { $0.name.caseInsensitiveCompare(target) == .orderedSame && $0.persistentModelID != existing?.persistentModelID }) {
            duplicateName = true
            return
        }
        if let existing {
            existing.name = target
            existing.muscleGroup = muscleGroup
            existing.secondaryMuscleGroup = secondary
            existing.equipment = equipment
            existing.trackingMode = trackingMode
            existing.notes = trimmedNotes
            onSave(existing)
        } else {
            let exercise = Exercise(
                name: target,
                muscleGroup: muscleGroup,
                secondaryMuscleGroup: secondary,
                isCustom: true,
                equipment: equipment
            )
            exercise.notes = trimmedNotes
            exercise.trackingMode = trackingMode
            modelContext.insert(exercise)
            onSave(exercise)
        }
        try? modelContext.save()
        dismiss()
    }
}
