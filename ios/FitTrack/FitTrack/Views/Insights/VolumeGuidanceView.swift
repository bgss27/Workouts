import SwiftUI
import SwiftData

/// Hard sets per muscle group this week against a target band, plus deload
/// advice. Mirrors Android's `VolumeGuidanceScreen`.
struct VolumeGuidanceView: View {
    @Environment(\.modelContext) private var modelContext

    private enum Week: String, CaseIterable, Identifiable {
        case this = "This week", last = "Last week"
        var id: String { rawValue }
    }

    @State private var week: Week = .this
    @State private var rows: [MuscleVolumeRow] = []
    @State private var advice: [DeloadAdvice] = []
    @State private var loaded = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Picker("Week", selection: $week) {
                    ForEach(Week.allCases) { Text($0.rawValue).tag($0) }
                }
                .pickerStyle(.segmented)
                .padding(.horizontal, Fit.gutter)

                summaryTile
                    .padding(.horizontal, Fit.gutter)

                Overline(text: "Sets per muscle group")
                    .padding(.horizontal, Fit.gutter)

                Tile(padding: 0) {
                    VStack(spacing: 0) {
                        ForEach(Array(rows.enumerated()), id: \.element.id) { index, row in
                            VolumeRowView(row: row)
                            if index < rows.count - 1 { RowDivider() }
                        }
                    }
                }
                .padding(.horizontal, Fit.gutter)

                if !advice.isEmpty {
                    Overline(text: "Deload advice")
                        .padding(.horizontal, Fit.gutter)
                    ForEach(advice) { item in
                        DeloadAdviceCard(advice: item)
                            .padding(.horizontal, Fit.gutter)
                    }
                }

                Text("Most research puts productive hypertrophy volume around 10–20 hard sets per muscle per week (6–12 for small groups). Warm-ups don't count. A deload is a planned lighter week so fatigue drops while strength holds.")
                    .font(.caption)
                    .foregroundStyle(Color.fitOnSurfaceVariant)
                    .padding(.horizontal, Fit.gutter)
            }
            .padding(.vertical, 12)
        }
        .fitScreenBackground()
        .navigationTitle("Weekly Volume")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { if !loaded { reload() } }
        .onChange(of: week) { _, _ in reload() }
    }

    private var summaryTile: some View {
        let inRange = rows.filter { $0.status == .inRange }.count
        let trained = rows.filter { $0.sets > 0 }.count
        let total = rows.reduce(0) { $0 + $1.sets }
        return Tile {
            HStack(spacing: 0) {
                StatValue(value: "\(total)", label: "Hard sets", tint: .fitPrimary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                StatValue(value: "\(trained)", label: "Groups trained")
                    .frame(maxWidth: .infinity, alignment: .leading)
                StatValue(value: "\(inRange)/\(rows.count)", label: "In range")
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
    }

    private func reload() {
        let workouts = VolumeGuidance.completedWorkouts(in: modelContext)
        var start = VolumeGuidance.weekStart(containing: Date())
        if week == .last { start = Calendar.current.date(byAdding: .day, value: -7, to: start) ?? start }
        rows = VolumeGuidance.rows(workouts: workouts, weekStart: start)
        advice = DeloadAdvisor.evaluate(workouts: workouts)
        loaded = true
    }
}

/// Summary line for the Insights tile: "6 of 9 groups in range".
enum VolumeGuidanceSummary {
    static func text(in context: ModelContext) -> String {
        let rows = VolumeGuidance.rows(
            workouts: VolumeGuidance.completedWorkouts(in: context),
            weekStart: VolumeGuidance.weekStart(containing: Date())
        )
        let inRange = rows.filter { $0.status == .inRange }.count
        let total = rows.reduce(0) { $0 + $1.sets }
        return total == 0 ? "No hard sets yet this week" : "\(total) hard sets · \(inRange) of \(rows.count) groups in range"
    }
}

private struct VolumeRowView: View {
    let row: MuscleVolumeRow

    private var statusText: String {
        switch row.status {
        case .below: return "Below"
        case .inRange: return "In range"
        case .above: return "Above"
        }
    }

    private var tint: Color {
        switch row.status {
        case .below: return .fitTrackWarning
        case .inRange: return .fitTrackSuccess
        case .above: return .fitTertiary
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(row.muscleGroup.displayName).font(.subheadline.weight(.semibold))
                Spacer()
                Text("\(row.sets) sets").font(.subheadline.weight(.semibold)).monospacedDigit()
                Text(statusText)
                    .font(.caption2.weight(.bold))
                    .padding(.horizontal, 7).padding(.vertical, 2)
                    .background(tint.opacity(0.18))
                    .foregroundStyle(tint)
                    .clipShape(Capsule())
            }
            VolumeBar(sets: row.sets, band: row.band, tint: tint)
            Text("Target \(row.band.low)–\(row.band.high) · 4-week avg \(String(format: "%.1f", row.fourWeekAverage))")
                .font(.caption2)
                .foregroundStyle(Color.fitOnSurfaceVariant)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
    }
}

/// A horizontal bar with the target band shaded behind the filled portion.
private struct VolumeBar: View {
    let sets: Int
    let band: VolumeBand
    let tint: Color

    private var scaleMax: Double { Double(max(band.high + 6, sets + 2)) }

    var body: some View {
        GeometryReader { geo in
            let w = geo.size.width
            ZStack(alignment: .leading) {
                Capsule().fill(Color.fitSurfaceHigh)
                Rectangle()
                    .fill(Color.fitTrackSuccess.opacity(0.18))
                    .frame(width: w * (Double(band.high - band.low) / scaleMax))
                    .offset(x: w * (Double(band.low) / scaleMax))
                Capsule()
                    .fill(tint)
                    .frame(width: max(sets > 0 ? 6 : 0, w * (Double(sets) / scaleMax)))
            }
        }
        .frame(height: 8)
        .clipShape(Capsule())
    }
}

struct DeloadAdviceCard: View {
    let advice: DeloadAdvice

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: "arrow.down.right.circle.fill")
                .font(.title3)
                .foregroundStyle(Color.fitTrackWarning)
            VStack(alignment: .leading, spacing: 3) {
                Text(advice.title).font(.subheadline.weight(.semibold))
                Text(advice.reason).font(.caption).foregroundStyle(Color.fitOnSurfaceVariant)
                Text(advice.action).font(.caption.weight(.medium))
            }
            Spacer(minLength: 0)
        }
        .padding()
        .background(Color.fitTrackWarning.opacity(0.1))
        .clipShape(RoundedRectangle(cornerRadius: Fit.smallRadius, style: .continuous))
    }
}
