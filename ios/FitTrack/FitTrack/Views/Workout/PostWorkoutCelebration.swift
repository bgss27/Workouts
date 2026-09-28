import SwiftUI
import UIKit

/// Full-screen celebration shown after a workout that produced one or more
/// 1RM personal records. Bouncy trophy + delta cards + auto-dismiss after
/// 6 seconds. Mirrors the Android `PostWorkoutCelebration` (without confetti
/// — iOS doesn't have a built-in particle library and pulling one in for a
/// 6-second flourish isn't worth the dependency).
struct PostWorkoutCelebration: View {
    let prs: [PRResult]
    let displayUnit: String  // "kg" or "lbs"
    let onDismiss: () -> Void

    @State private var trophyScale: CGFloat = 0.6
    @State private var cardScale: CGFloat = 0.85

    var body: some View {
        ZStack {
            Color.black.opacity(0.45).ignoresSafeArea()

            VStack(spacing: 18) {
                Image(systemName: "trophy.fill")
                    .font(.system(size: 64))
                    .foregroundStyle(.yellow)
                    .scaleEffect(trophyScale)

                Text(prs.count == 1 ? "New Personal Record!" : "\(prs.count) New PRs!")
                    .font(.title2.bold())
                    .multilineTextAlignment(.center)

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

                Button(action: onDismiss) {
                    Text("Continue")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .padding(.top, 4)
            }
            .padding(24)
            .frame(maxWidth: 360)
            .background(Color(.systemBackground))
            .cornerRadius(28)
            .shadow(radius: 14)
            .scaleEffect(cardScale)
            .padding()
        }
        .onAppear {
            // Heavy + heavy haptic combo to feel celebratory.
            let notif = UINotificationFeedbackGenerator()
            notif.notificationOccurred(.success)
            // Spring-bouncy entrance for the card; trophy bounces in slightly
            // after so the eye lands on the headline first.
            withAnimation(.spring(response: 0.55, dampingFraction: 0.55)) {
                cardScale = 1.0
            }
            withAnimation(.spring(response: 0.6, dampingFraction: 0.5).delay(0.1)) {
                trophyScale = 1.0
            }
            // Auto-dismiss after 6 seconds matches the Android behavior — if
            // the user puts the phone down we don't block the workout flow.
            DispatchQueue.main.asyncAfter(deadline: .now() + 6) {
                onDismiss()
            }
        }
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
