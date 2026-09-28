import SwiftUI

/// One Improvement Tip card on Home. For actionable suggestions (currently
/// only `.increaseFrequency`) the whole card is tappable and shows a trailing
/// arrow — mirrors the Android `SuggestionCard` UX.
struct SuggestionCard: View {
    let suggestion: Suggestion
    let onTap: (() -> Void)?

    var body: some View {
        let isActionable = onTap != nil

        Button {
            onTap?()
        } label: {
            HStack(alignment: .center, spacing: 12) {
                Image(systemName: iconName)
                    .font(.title3)
                    .foregroundColor(accentColor)
                    .frame(width: 28)
                VStack(alignment: .leading, spacing: 4) {
                    Text(suggestion.title)
                        .font(.subheadline.bold())
                        .foregroundColor(.primary)
                    Text(suggestion.description)
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.leading)
                        .fixedSize(horizontal: false, vertical: true)
                }
                Spacer(minLength: 6)
                if isActionable {
                    Image(systemName: "arrow.right")
                        .font(.caption.bold())
                        .foregroundColor(accentColor)
                }
            }
            .padding()
            .background(accentColor.opacity(0.1))
            .cornerRadius(12)
        }
        .buttonStyle(.plain)
        .disabled(!isActionable)
    }

    private var iconName: String {
        switch suggestion.kind {
        case .increaseWeight: return "arrow.up.right.circle"
        case .increaseVolume: return "plus.circle"
        case .increaseFrequency: return "repeat"
        case .deload: return "arrow.down.right.circle"
        case .tryExercise: return "dumbbell.fill"
        case .muscleGroupSummary: return "chart.bar.fill"
        }
    }

    private var accentColor: Color {
        switch suggestion.kind {
        case .increaseWeight: return .fitTrackSuccess
        case .increaseVolume, .increaseFrequency: return .blue
        case .deload: return .fitTrackDanger
        case .tryExercise: return .teal
        case .muscleGroupSummary: return .fitTrackWarning
        }
    }
}
