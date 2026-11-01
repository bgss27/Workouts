import SwiftUI
import SwiftData
import Charts

/// Dated body-metric history with a trend chart per measurement, plus an
/// add/edit sheet. Pro feature; the free tier sees the paywall tile.
/// Mirrors Android's `BodyMetricsScreen`.
struct BodyMetricsView: View {
    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject private var proManager: ProManager
    @AppStorage("profile_weightUnit") private var weightUnit = "kg"
    @Query(sort: \BodyMetric.recordedAt, order: .reverse) private var metrics: [BodyMetric]

    @State private var selectedType: BodyMetricType = .weight
    @State private var editing: BodyMetric?
    @State private var showAdd = false

    private var isLbs: Bool { weightUnit == "lbs" }

    var body: some View {
        Group {
            if proManager.isPro {
                content
            } else {
                paywall
            }
        }
        .fitScreenBackground()
        .navigationTitle("Body Metrics")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if proManager.isPro {
                ToolbarItem(placement: .primaryAction) {
                    Button { showAdd = true } label: { Image(systemName: "plus") }
                        .accessibilityLabel("Add check-in")
                }
            }
        }
        .sheet(isPresented: $showAdd) {
            BodyMetricEditSheet(metric: nil, isLbs: isLbs) { saveNew($0) }
        }
        .sheet(item: $editing) { metric in
            BodyMetricEditSheet(metric: metric, isLbs: isLbs) { _ in try? modelContext.save() }
        }
    }

    private var content: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(BodyMetricType.allCases) { type in
                            PillChip(label: type.displayName, selected: selectedType == type) {
                                selectedType = type
                            }
                        }
                    }
                    .padding(.horizontal, Fit.gutter)
                }

                chartTile
                    .padding(.horizontal, Fit.gutter)

                Overline(text: "Check-ins")
                    .padding(.horizontal, Fit.gutter)

                if metrics.isEmpty {
                    Tile {
                        VStack(alignment: .leading, spacing: 6) {
                            Text("No check-ins yet").font(.subheadline.weight(.semibold))
                            Text("Tap + to log your weight or measurements. Even a weekly weigh-in makes the trend line useful.")
                                .font(.caption).foregroundStyle(Color.fitOnSurfaceVariant)
                        }
                    }
                    .padding(.horizontal, Fit.gutter)
                } else {
                    Tile(padding: 0) {
                        VStack(spacing: 0) {
                            ForEach(Array(metrics.enumerated()), id: \.element.persistentModelID) { index, metric in
                                Button { editing = metric } label: {
                                    ListRow(
                                        title: metric.recordedAt.formatted(.dateTime.weekday(.abbreviated).month(.abbreviated).day()),
                                        subtitle: summary(metric),
                                        systemImage: "scalemass",
                                        tint: .fitSecondary
                                    ) { SmallChevron() }
                                }
                                .buttonStyle(.plain)
                                .contextMenu {
                                    Button(role: .destructive) {
                                        modelContext.delete(metric)
                                        try? modelContext.save()
                                    } label: { Label("Delete", systemImage: "trash") }
                                }
                                if index < metrics.count - 1 { RowDivider() }
                            }
                        }
                    }
                    .padding(.horizontal, Fit.gutter)
                }
            }
            .padding(.vertical, 12)
        }
    }

    private var chartTile: some View {
        let points = metrics
            .compactMap { m in selectedType.value(of: m).map { (m.recordedAt, display($0, selectedType)) } }
            .sorted { $0.0 < $1.0 }
        let latest = points.last?.1
        let first = points.first?.1
        return Tile {
            VStack(alignment: .leading, spacing: 12) {
                HStack(alignment: .firstTextBaseline) {
                    StatValue(
                        value: latest.map { format($0, selectedType) } ?? "—",
                        label: "\(selectedType.displayName) · \(unitLabel(selectedType))",
                        tint: .fitPrimary
                    )
                    Spacer()
                    if let latest, let first, points.count > 1 {
                        let delta = latest - first
                        Text((delta >= 0 ? "+" : "") + format(delta, selectedType))
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(delta == 0 ? Color.fitOnSurfaceVariant : (delta < 0 ? Color.fitTrackSuccess : Color.fitTertiary))
                    }
                }
                if points.count >= 2 {
                    Chart(points, id: \.0) { date, value in
                        LineMark(x: .value("Date", date), y: .value(selectedType.displayName, value))
                            .foregroundStyle(Color.fitPrimary)
                            .interpolationMethod(.monotone)
                        PointMark(x: .value("Date", date), y: .value(selectedType.displayName, value))
                            .foregroundStyle(Color.fitPrimary)
                    }
                    .chartYScale(domain: .automatic(includesZero: false))
                    .frame(height: 160)
                } else {
                    Text("Log two or more check-ins to see a trend.")
                        .font(.caption)
                        .foregroundStyle(Color.fitOnSurfaceVariant)
                }
            }
        }
    }

    private var paywall: some View {
        VStack(spacing: 20) {
            Spacer().frame(height: 24)
            IconBadge(systemName: "scalemass.fill", size: 72)
            Text("Track your body, not just your lifts")
                .font(.title3.bold())
                .multilineTextAlignment(.center)
            Text("Log weight, body fat and tape measurements over time, with a trend chart for each. Part of FitTrack Pro.")
                .font(.subheadline)
                .foregroundStyle(Color.fitOnSurfaceVariant)
                .multilineTextAlignment(.center)
            NavigationLink { UpgradeView() } label: {
                Label("Unlock with Pro", systemImage: "star.fill")
            }
            .buttonStyle(PrimaryButtonStyle())
            Spacer()
        }
        .padding(24)
    }

    private func saveNew(_ metric: BodyMetric) {
        modelContext.insert(metric)
        try? modelContext.save()
    }

    private func summary(_ m: BodyMetric) -> String {
        var parts: [String] = []
        for type in BodyMetricType.allCases {
            if let v = type.value(of: m) {
                parts.append("\(type.displayName) \(format(display(v, type), type)) \(unitLabel(type))")
            }
        }
        return parts.isEmpty ? "No values" : parts.joined(separator: " · ")
    }

    private func display(_ canonical: Double, _ type: BodyMetricType) -> Double {
        type.kind == .mass && isLbs ? canonical * 2.2046226218 : canonical
    }

    private func unitLabel(_ type: BodyMetricType) -> String {
        switch type.kind {
        case .mass: return isLbs ? "lbs" : "kg"
        case .length: return "cm"
        case .percent: return "%"
        }
    }

    private func format(_ value: Double, _ type: BodyMetricType) -> String {
        value == value.rounded() ? String(Int(value)) : String(format: "%.1f", value)
    }
}

// MARK: - Add / edit sheet

private struct BodyMetricEditSheet: View {
    let metric: BodyMetric?
    let isLbs: Bool
    let onSave: (BodyMetric) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var date: Date = Date()
    @State private var values: [BodyMetricType: String] = [:]
    @State private var notes: String = ""

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    DatePicker("Date", selection: $date, in: ...Date(), displayedComponents: .date)
                }
                Section("Measurements") {
                    ForEach(BodyMetricType.allCases) { type in
                        HStack {
                            Text(type.displayName)
                            Spacer()
                            TextField("—", text: binding(for: type))
                                .keyboardType(.decimalPad)
                                .multilineTextAlignment(.trailing)
                                .frame(maxWidth: 90)
                            Text(unit(type)).foregroundStyle(.secondary).frame(width: 34, alignment: .leading)
                        }
                    }
                }
                Section("Notes") {
                    TextField("Optional", text: $notes, axis: .vertical)
                }
            }
            .scrollContentBackground(.hidden)
            .background(Color.fitBackground)
            .navigationTitle(metric == nil ? "New check-in" : "Edit check-in")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") { save() }.disabled(!hasAnyValue)
                }
            }
            .onAppear(perform: load)
        }
    }

    private var hasAnyValue: Bool {
        values.values.contains { Double($0) != nil }
    }

    private func binding(for type: BodyMetricType) -> Binding<String> {
        Binding(
            get: { values[type] ?? "" },
            set: { values[type] = $0.filter { $0.isNumber || $0 == "." } }
        )
    }

    private func unit(_ type: BodyMetricType) -> String {
        switch type.kind {
        case .mass: return isLbs ? "lbs" : "kg"
        case .length: return "cm"
        case .percent: return "%"
        }
    }

    private func load() {
        guard let metric else { return }
        date = metric.recordedAt
        notes = metric.notes ?? ""
        for type in BodyMetricType.allCases {
            if let v = type.value(of: metric) {
                let shown = type.kind == .mass && isLbs ? v * 2.2046226218 : v
                values[type] = shown == shown.rounded() ? String(Int(shown)) : String(format: "%.1f", shown)
            }
        }
    }

    private func save() {
        let target = metric ?? BodyMetric()
        target.recordedAt = date
        target.notes = notes.isEmpty ? nil : notes
        for type in BodyMetricType.allCases {
            let typed = Double(values[type] ?? "")
            let canonical = typed.map { type.kind == .mass && isLbs ? $0 * 0.45359237 : $0 }
            type.set(canonical, on: target)
        }
        onSave(target)
        dismiss()
    }
}
