import SwiftUI

// Small controls shared by the active workout, routine detail and Settings.

/// Chip + menu for picking a rest length. A `nil` selection means "use the
/// global default" (only offered when `allowsDefault`).
struct RestDurationPicker: View {
    @Binding var seconds: Int?
    var allowsDefault = true
    var defaultSeconds: Int = RestTimerNotifier.defaultRestSeconds

    var body: some View {
        Menu {
            if allowsDefault {
                Button { seconds = nil } label: {
                    row("Default (\(RestTimerNotifier.label(defaultSeconds)))", selected: seconds == nil)
                }
                Divider()
            }
            ForEach(RestTimerNotifier.restChoices, id: \.self) { choice in
                Button { seconds = choice } label: {
                    row(RestTimerNotifier.label(choice), selected: seconds == choice)
                }
            }
        } label: {
            HStack(spacing: 4) {
                Image(systemName: "timer").font(.caption2.weight(.semibold))
                Text("Rest \(RestTimerNotifier.label(seconds ?? defaultSeconds))")
                    .font(.caption.weight(.semibold))
                    .monospacedDigit()
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 5)
            .background(Color.fitSurfaceHigh)
            .foregroundStyle(Color.primary)
            .clipShape(Capsule())
        }
        .accessibilityLabel("Rest length")
    }

    @ViewBuilder
    private func row(_ text: String, selected: Bool) -> some View {
        if selected { Label(text, systemImage: "checkmark") } else { Text(text) }
    }
}

/// The set-number badge at the start of a set row. Tapping opens the
/// set-type menu; the glyph and tint reflect the current type.
struct SetTypeBadge: View {
    let number: Int
    @Binding var setType: SetType

    var body: some View {
        Menu {
            ForEach(SetType.allCases) { type in
                Button { setType = type } label: {
                    if type == setType {
                        Label(type.displayName, systemImage: "checkmark")
                    } else {
                        Text(type.displayName)
                    }
                }
            }
        } label: {
            Text(setType.badge ?? "\(number)")
                .font(.subheadline.weight(.bold))
                .monospacedDigit()
                .frame(width: 26, height: 26)
                .background(setType == .working ? Color.clear : tint.opacity(0.18))
                .foregroundStyle(tint)
                .clipShape(RoundedRectangle(cornerRadius: 7, style: .continuous))
        }
        .buttonStyle(.plain)
        .accessibilityLabel("Set type: \(setType.displayName)")
    }

    private var tint: Color {
        switch setType {
        case .working: return .primary
        case .warmup: return .fitOnSurfaceVariant
        case .dropset: return .fitTertiary
        case .failure: return .fitPrimary
        }
    }
}

/// "Target 62.5 kg × 8 ↑ · Hit 12 reps on every set" with an Apply button
/// that fills the blank sets. Shown under the rest/note chips.
struct ProgressionChip: View {
    let target: ProgressionSuggestion
    let weightUnit: String
    let onApply: () -> Void

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: target.glyph)
                .font(.caption.weight(.bold))
                .frame(width: 22, height: 22)
                .background(tint.opacity(0.18))
                .foregroundStyle(tint)
                .clipShape(Circle())
            VStack(alignment: .leading, spacing: 1) {
                Text(target.label(unit: weightUnit))
                    .font(.caption.weight(.semibold))
                    .monospacedDigit()
                Text(target.reason)
                    .font(.caption2)
                    .foregroundStyle(Color.fitOnSurfaceVariant)
                    .lineLimit(2)
            }
            Spacer(minLength: 4)
            Button("Apply", action: onApply)
                .font(.caption.weight(.semibold))
                .buttonStyle(.plain)
                .padding(.horizontal, 10)
                .padding(.vertical, 5)
                .background(tint.opacity(0.18))
                .foregroundStyle(tint)
                .clipShape(Capsule())
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 7)
        .background(Color.fitSurfaceHigh)
        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(target.label(unit: weightUnit)). \(target.reason)")
    }

    private var tint: Color {
        switch target.kind {
        case .increaseWeight: return .fitTrackSuccess
        case .addReps: return .fitPrimary
        case .hold: return .fitOnSurfaceVariant
        case .reduceWeight: return .fitTrackWarning
        }
    }
}

/// Edit the per-exercise note that carries over between sessions
/// ("seat 4, grip at rings").
struct ExerciseNoteSheet: View {
    let exerciseName: String
    @State var text: String
    let onSave: (String?) -> Void

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("e.g. seat 4, grip at the rings", text: $text, axis: .vertical)
                        .lineLimit(3...8)
                } footer: {
                    Text("Shown every time \(exerciseName) comes up in a workout.")
                }
            }
            .scrollContentBackground(.hidden)
            .background(Color.fitBackground)
            .navigationTitle("Exercise note")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
                        onSave(trimmed.isEmpty ? nil : trimmed)
                        dismiss()
                    }
                }
            }
        }
    }
}
