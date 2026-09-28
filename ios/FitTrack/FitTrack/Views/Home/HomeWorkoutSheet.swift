import SwiftUI
import SwiftData

/// "Train at home" picker. The user chooses a muscle group + the equipment
/// they actually have on hand; we resolve a 4-5 exercise session from the
/// catalog filtered by both criteria.
///
/// Mirrors the Android `HomeWorkoutSheet`. Equipment preference is persisted
/// to `@AppStorage` so the user doesn't re-tick boxes every time. Defaults
/// to bodyweight-only on first run.
struct HomeWorkoutSheet: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss

    /// Hand-off: parent navigates to ActiveWorkoutView with these.
    let onGenerate: ([Exercise]) -> Void

    @State private var selectedGroup: MuscleGroup
    @AppStorage("homeEquipment") private var storedEquipmentRaw: String = Equipment.bodyweight.rawValue
    @State private var selectedEquipment: Set<Equipment> = [.bodyweight]
    @State private var warningMessage: String?

    init(defaultGroup: MuscleGroup?, onGenerate: @escaping ([Exercise]) -> Void) {
        self._selectedGroup = State(initialValue: defaultGroup ?? .chest)
        self.onGenerate = onGenerate
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Text("Pick the muscle group you want to train and what equipment you've got. We'll build a quick session from the catalog.")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }

                Section("Muscle group") {
                    Picker("Muscle group", selection: $selectedGroup) {
                        ForEach(MuscleGroup.allCases) { g in
                            Text(g.displayName).tag(g)
                        }
                    }
                    .pickerStyle(.menu)
                }

                Section("Equipment available") {
                    ForEach(Equipment.allCases) { eq in
                        Button {
                            if selectedEquipment.contains(eq) {
                                selectedEquipment.remove(eq)
                            } else {
                                selectedEquipment.insert(eq)
                            }
                        } label: {
                            HStack {
                                Image(systemName: selectedEquipment.contains(eq) ? "checkmark.circle.fill" : "circle")
                                    .foregroundColor(selectedEquipment.contains(eq) ? .accentColor : .secondary)
                                Text(eq.displayName)
                                    .foregroundColor(.primary)
                                Spacer()
                            }
                        }
                        .buttonStyle(.plain)
                    }
                    Text("Tip: leaving everything off won't generate anything. Bodyweight covers most exercises at home.")
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }

                if let warning = warningMessage {
                    Section {
                        Text(warning)
                            .font(.caption)
                            .foregroundColor(.orange)
                    }
                }

                Section {
                    Button("Generate workout") {
                        runGenerator()
                    }
                    .disabled(selectedEquipment.isEmpty)
                }
            }
            .navigationTitle("Home Workout")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
            }
            .onAppear { loadEquipmentPref() }
        }
    }

    private func loadEquipmentPref() {
        let parsed = storedEquipmentRaw
            .split(separator: ",")
            .compactMap { Equipment(rawValue: String($0).trimmingCharacters(in: .whitespaces)) }
        if !parsed.isEmpty {
            selectedEquipment = Set(parsed)
        }
    }

    private func saveEquipmentPref() {
        storedEquipmentRaw = selectedEquipment.map(\.rawValue).joined(separator: ",")
    }

    private func runGenerator() {
        saveEquipmentPref()
        let group = selectedGroup
        let equipment = selectedEquipment
        let result = HomeWorkoutGenerator.generate(
            group: group,
            equipment: equipment,
            context: modelContext
        )
        if result.exercises.isEmpty {
            warningMessage = "No \(group.displayName.lowercased()) exercises match that equipment combo."
            return
        }
        if let secondaryOnlyHint = result.secondaryOnlyHint {
            warningMessage = secondaryOnlyHint
            // Still hand off — user can see the limited list and proceed,
            // matching Android's snackbar-then-navigate behavior.
        }
        onGenerate(result.exercises)
        dismiss()
    }
}

/// Pure function that picks up to 5 exercises for the muscle group + equipment
/// combo. Primary-muscle matches first, then secondary. Surfaces a warning hint
/// when only secondary-muscle matches are available (e.g. Biceps + Bodyweight
/// before the dedicated biceps moves were added).
enum HomeWorkoutGenerator {
    struct Result {
        let exercises: [Exercise]
        let secondaryOnlyHint: String?
    }

    static func generate(group: MuscleGroup, equipment: Set<Equipment>, context: ModelContext) -> Result {
        guard !equipment.isEmpty else { return Result(exercises: [], secondaryOnlyHint: nil) }
        let descriptor = FetchDescriptor<Exercise>()
        let all = (try? context.fetch(descriptor)) ?? []
        let eqRaws = Set(equipment.map(\.rawValue))
        let candidates = all.filter { ex in
            // Use the `equipment` accessor — it normalises nil (legacy rows
            // from before the column existed) to .barbell.
            eqRaws.contains(ex.equipment.rawValue)
                && (ex.muscleGroup == group || ex.secondaryMuscleGroup == group)
        }
        let primary = candidates.filter { $0.muscleGroup == group }.sorted { $0.name < $1.name }
        let secondary = candidates.filter { $0.muscleGroup != group }.sorted { $0.name < $1.name }
        let ordered = (primary + secondary).prefix(5).map { $0 }

        var hint: String? = nil
        if !ordered.isEmpty && primary.isEmpty {
            let lowered = group.displayName.lowercased()
            let suffix: String
            switch group {
            case .biceps: suffix = "Add a Pull-Up Bar (chin-ups) or Resistance Band (band curls) for direct biceps work."
            case .back: suffix = "Add a Pull-Up Bar for direct back work."
            case .forearms: suffix = "Add a Pull-Up Bar (dead hangs) or dumbbells for direct forearm work."
            default: suffix = "Try adding more equipment for direct \(lowered) work."
            }
            hint = "Limited \(lowered) options — these hit \(lowered) as a secondary muscle. \(suffix)"
        }

        return Result(exercises: Array(ordered), secondaryOnlyHint: hint)
    }
}
