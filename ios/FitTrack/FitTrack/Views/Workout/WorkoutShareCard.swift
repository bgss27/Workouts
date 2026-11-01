import SwiftUI
import UIKit

/// Everything the post-workout summary and the shareable card need. Built
/// once when a workout is finished; values are already in the display unit.
struct WorkoutShareSummary: Identifiable, Equatable {
    let id = UUID()
    let date: Date
    let durationMinutes: Int
    let exerciseCount: Int
    let setCount: Int
    /// Working-set volume in the display unit, e.g. 12_340.
    let volume: Double
    let unit: String
    let prCount: Int
    let streakWeeks: Int
    /// Exercise names in workout order; the card shows the first few.
    let exerciseNames: [String]

    var volumeText: String {
        switch volume {
        case 1_000_000...: return String(format: "%.1fM", volume / 1_000_000)
        case 10_000...: return String(format: "%.0fk", volume / 1000)
        case 1_000...: return String(format: "%.1fk", volume / 1000)
        default: return String(format: "%.0f", volume)
        }
    }
}

/// 4:5 portrait card (1080x1350 when rendered at 3x) in the dark athletic
/// look, independent of the device's color scheme so it always shares the
/// same way. Mirrors Android's `WorkoutShareCard`.
struct WorkoutShareCardView: View {
    let summary: WorkoutShareSummary

    private let volt = Color(red: 0xC6 / 255, green: 0xFF / 255, blue: 0x4A / 255)
    private let ink = Color(red: 0x0B / 255, green: 0x0C / 255, blue: 0x10 / 255)
    private let tile = Color(red: 0x15 / 255, green: 0x16 / 255, blue: 0x1C / 255)
    private let muted = Color(red: 0xA2 / 255, green: 0xA6 / 255, blue: 0xB5 / 255)

    var body: some View {
        ZStack(alignment: .topLeading) {
            ink
            Circle()
                .fill(volt.opacity(0.18))
                .frame(width: 320, height: 320)
                .offset(x: 200, y: -120)
                .blur(radius: 40)

            VStack(alignment: .leading, spacing: 0) {
                HStack {
                    Text("FITTRACK")
                        .font(.system(size: 13, weight: .heavy))
                        .tracking(3)
                        .foregroundStyle(volt)
                    Spacer()
                    Text(summary.date.formatted(.dateTime.weekday(.wide).month(.abbreviated).day()))
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(muted)
                }

                Spacer().frame(height: 36)

                Text("Workout\ncomplete")
                    .font(.system(size: 44, weight: .heavy, design: .rounded))
                    .foregroundStyle(.white)
                    .lineSpacing(-4)

                Spacer().frame(height: 28)

                HStack(spacing: 10) {
                    stat(value: "\(summary.durationMinutes)", label: "MIN")
                    stat(value: "\(summary.setCount)", label: "SETS")
                    stat(value: summary.volumeText, label: summary.unit.uppercased())
                }

                Spacer().frame(height: 18)

                VStack(alignment: .leading, spacing: 8) {
                    Text("\(summary.exerciseCount) EXERCISES")
                        .font(.system(size: 11, weight: .bold))
                        .tracking(1.5)
                        .foregroundStyle(muted)
                    ForEach(Array(summary.exerciseNames.prefix(5).enumerated()), id: \.offset) { _, name in
                        HStack(spacing: 8) {
                            Circle().fill(volt).frame(width: 6, height: 6)
                            Text(name)
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundStyle(.white)
                                .lineLimit(1)
                        }
                    }
                    if summary.exerciseNames.count > 5 {
                        Text("+\(summary.exerciseNames.count - 5) more")
                            .font(.system(size: 13, weight: .medium))
                            .foregroundStyle(muted)
                    }
                }

                Spacer()

                HStack(spacing: 8) {
                    if summary.prCount > 0 {
                        badge(systemName: "trophy.fill", text: summary.prCount == 1 ? "1 new PR" : "\(summary.prCount) new PRs")
                    }
                    if summary.streakWeeks > 1 {
                        badge(systemName: "flame.fill", text: "\(summary.streakWeeks)-week streak")
                    }
                }
            }
            .padding(28)
        }
        .frame(width: 360, height: 450)
        .clipShape(RoundedRectangle(cornerRadius: 0))
    }

    private func stat(value: String, label: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(value)
                .font(.system(size: 30, weight: .heavy, design: .rounded))
                .foregroundStyle(.white)
                .lineLimit(1)
                .minimumScaleFactor(0.6)
            Text(label)
                .font(.system(size: 10, weight: .bold))
                .tracking(1.5)
                .foregroundStyle(muted)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(tile)
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    private func badge(systemName: String, text: String) -> some View {
        HStack(spacing: 6) {
            Image(systemName: systemName).font(.system(size: 12, weight: .bold))
            Text(text).font(.system(size: 13, weight: .bold))
        }
        .foregroundStyle(ink)
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(volt)
        .clipShape(Capsule())
    }
}

enum WorkoutShareRenderer {
    /// Render the card to a 1080x1350 PNG-backed image.
    @MainActor
    static func render(_ summary: WorkoutShareSummary) -> UIImage? {
        let renderer = ImageRenderer(content: WorkoutShareCardView(summary: summary))
        renderer.scale = 3
        renderer.isOpaque = true
        return renderer.uiImage
    }
}

/// System share sheet for an image. Only the image is offered so the sheet
/// previews the card itself; a text item alongside makes iOS preview the
/// text instead.
struct ShareImageSheet: UIViewControllerRepresentable {
    let image: UIImage
    let text: String

    func makeUIViewController(context: Context) -> UIActivityViewController {
        let controller = UIActivityViewController(activityItems: [image], applicationActivities: nil)
        controller.setValue(text, forKey: "subject")
        return controller
    }

    func updateUIViewController(_ controller: UIActivityViewController, context: Context) {}
}
