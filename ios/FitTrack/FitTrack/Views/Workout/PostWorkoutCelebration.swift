import SwiftUI
import UIKit

/// Full-screen summary shown after every finished workout: duration, sets,
/// volume, any 1RM personal records, and a Share button that renders the
/// workout card. Mirrors the Android `PostWorkoutCelebration`.
struct PostWorkoutCelebration: View {
    let summary: WorkoutShareSummary
    let prs: [PRResult]
    let displayUnit: String  // "kg" or "lbs"
    let onDismiss: () -> Void

    @State private var trophyScale: CGFloat = 0.6
    @State private var cardScale: CGFloat = 0.85
    @State private var shareImage: UIImage?

    private var hasPRs: Bool { !prs.isEmpty }

    var body: some View {
        ZStack {
            Color.black.opacity(0.55).ignoresSafeArea()

            VStack(spacing: 18) {
                Image(systemName: hasPRs ? "trophy.fill" : "checkmark.seal.fill")
                    .font(.system(size: 60))
                    .foregroundStyle(hasPRs ? Color.yellow : Color.fitPrimary)
                    .scaleEffect(trophyScale)

                Text(headline)
                    .font(.system(.title2, design: .rounded).bold())
                    .multilineTextAlignment(.center)

                HStack(spacing: 10) {
                    SummaryStat(value: "\(summary.durationMinutes)", label: "min")
                    SummaryStat(value: "\(summary.setCount)", label: "sets")
                    SummaryStat(value: summary.volumeText, label: summary.unit)
                }

                if summary.streakWeeks > 1 {
                    Label("\(summary.streakWeeks)-week streak", systemImage: "flame.fill")
                        .font(.caption.bold())
                        .foregroundStyle(Color.fitOnPrimary)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(Color.fitPrimary)
                        .clipShape(Capsule())
                }

                if hasPRs {
                    VStack(spacing: 8) {
                        ForEach(prs.prefix(3)) { pr in
                            PrRow(pr: pr, displayUnit: displayUnit)
                        }
                        if prs.count > 3 {
                            Text("+\(prs.count - 3) more")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                }

                HStack(spacing: 10) {
                    Button {
                        shareImage = WorkoutShareRenderer.render(summary)
                    } label: {
                        Label("Share", systemImage: "square.and.arrow.up")
                    }
                    .buttonStyle(TonalButtonStyle())

                    Button(action: onDismiss) {
                        Text("Continue")
                    }
                    .buttonStyle(PrimaryButtonStyle())
                }
                .padding(.top, 4)
            }
            .padding(24)
            .frame(maxWidth: 360)
            .background(Color.fitSurface)
            .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
            .shadow(radius: 14)
            .scaleEffect(cardScale)
            .padding()
        }
        .onAppear {
            UINotificationFeedbackGenerator().notificationOccurred(.success)
            withAnimation(.spring(response: 0.55, dampingFraction: 0.55)) {
                cardScale = 1.0
            }
            withAnimation(.spring(response: 0.6, dampingFraction: 0.5).delay(0.1)) {
                trophyScale = 1.0
            }
        }
        .sheet(isPresented: Binding(get: { shareImage != nil }, set: { if !$0 { shareImage = nil } })) {
            if let shareImage {
                ShareImageSheet(image: shareImage, text: shareText)
                    .presentationDetents([.medium, .large])
            }
        }
    }

    private var headline: String {
        if prs.count == 1 { return "New Personal Record!" }
        if prs.count > 1 { return "\(prs.count) New PRs!" }
        return "Workout complete"
    }

    private var shareText: String {
        "\(summary.durationMinutes) min · \(summary.setCount) sets · \(summary.volumeText) \(summary.unit) with FitTrack"
    }
}

private struct SummaryStat: View {
    let value: String
    let label: String

    var body: some View {
        VStack(spacing: 2) {
            Text(value)
                .font(.system(size: 24, weight: .heavy, design: .rounded))
                .monospacedDigit()
                .lineLimit(1)
                .minimumScaleFactor(0.6)
            Text(label.uppercased())
                .font(.caption2.weight(.bold))
                .tracking(1.2)
                .foregroundStyle(Color.fitOnSurfaceVariant)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
        .background(Color.fitSurfaceHigh)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}

private struct PrRow: View {
    let pr: PRResult
    let displayUnit: String

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: "arrow.up.right.circle.fill")
                .foregroundColor(.fitTrackSuccess)
            VStack(alignment: .leading, spacing: 2) {
                Text(pr.exerciseName)
                    .font(.subheadline.bold())
                Text("Previous: \(format(pr.previousBest1RM)) \(displayUnit)")
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 2) {
                Text("\(format(pr.newBest1RM)) \(displayUnit)")
                    .font(.subheadline.bold())
                    .foregroundColor(.fitTrackSuccess)
                Text("+\(format(pr.delta))")
                    .font(.caption2)
                    .foregroundColor(.fitTrackSuccess)
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(Color.fitTrackSuccess.opacity(0.12))
        .cornerRadius(12)
    }

    private func format(_ kg: Double) -> String {
        let isLbs = displayUnit == "lbs"
        let value = isLbs ? kg * 2.2046226218 : kg
        return String(format: "%.1f", value)
    }
}
