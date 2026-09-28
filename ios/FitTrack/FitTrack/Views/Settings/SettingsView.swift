import SwiftUI
import SwiftData
import UIKit

struct SettingsView: View {
    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject var proManager: ProManager
    @EnvironmentObject var healthKit: HealthKitService
    @State private var shareItem: ShareItem?
    @AppStorage("profile_name") private var name = ""
    @AppStorage("profile_age") private var age = ""
    @AppStorage("profile_weight") private var weight = ""
    @AppStorage("profile_height") private var height = ""
    @AppStorage("profile_gender") private var gender = "Not specified"
    @AppStorage("profile_goal") private var fitnessGoal = "Build Muscle"
    @AppStorage("profile_experience") private var experienceLevel = "Intermediate"
    @AppStorage("profile_weightUnit") private var weightUnit = "kg"

    let genders = ["Male", "Female", "Not specified"]
    let goals = ["Build Muscle", "Lose Weight", "Gain Strength", "Improve Endurance", "General Fitness"]
    let levels = ["Beginner", "Intermediate", "Advanced"]

    var body: some View {
        NavigationStack {
            Form {
                // Profile header
                Section {
                    HStack(spacing: 16) {
                        Image(systemName: "person.circle.fill")
                            .font(.system(size: 48))
                            .foregroundColor(.accentColor)
                        VStack(alignment: .leading) {
                            Text(name.isEmpty ? "Set up your profile" : name)
                                .font(.headline)
                            Text(proManager.isPro ? "Pro Member" : "Free Plan")
                                .font(.caption)
                                .foregroundColor(proManager.isPro ? .accentColor : .secondary)
                        }
                    }
                    .padding(.vertical, 4)
                }

                // Profile info
                Section("Profile") {
                    TextField("Name", text: $name)
                    TextField("Age", text: $age)
                        .keyboardType(.numberPad)

                    HStack {
                        TextField("Weight", text: $weight)
                            .keyboardType(.decimalPad)
                        Text(weightUnit)
                            .foregroundColor(.secondary)
                    }

                    HStack {
                        TextField("Height", text: $height)
                            .keyboardType(.numberPad)
                        Text("cm")
                            .foregroundColor(.secondary)
                    }

                    Picker("Gender", selection: $gender) {
                        ForEach(genders, id: \.self) { Text($0) }
                    }
                }

                // Fitness preferences
                Section("Fitness Preferences") {
                    Picker("Fitness Goal", selection: $fitnessGoal) {
                        ForEach(goals, id: \.self) { Text($0) }
                    }

                    Picker("Experience Level", selection: $experienceLevel) {
                        ForEach(levels, id: \.self) { Text($0) }
                    }

                    Picker("Weight Unit", selection: $weightUnit) {
                        Text("kg").tag("kg")
                        Text("lbs").tag("lbs")
                    }
                    .pickerStyle(.segmented)
                }

                // Subscription
                Section("Subscription") {
                    NavigationLink(destination: UpgradeView()) {
                        HStack {
                            Image(systemName: "star.fill")
                                .foregroundColor(proManager.isPro ? .accentColor : .yellow)
                            VStack(alignment: .leading) {
                                Text(proManager.isPro ? "Pro Member" : "Upgrade to Pro")
                                    .font(.subheadline.bold())
                                Text(proManager.isPro ? "All features unlocked" : "ML insights, all programs, and more")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }
                    }
                }

                // Integrations
                if healthKit.isAvailable {
                    Section("Integrations") {
                        HealthKitRow(
                            isPro: proManager.isPro,
                            isAuthorized: healthKit.isAuthorized,
                            isEnabled: healthKit.isEnabled,
                            onAuthorize: { Task { await healthKit.requestAuthorization() } },
                            onToggle: { healthKit.setEnabled($0) },
                        )
                    }
                }

                // Data
                Section("Data") {
                    HStack(spacing: 12) {
                        Image(systemName: "square.and.arrow.up")
                            .foregroundColor(.accentColor)
                        VStack(alignment: .leading) {
                            Text("Export workouts as CSV").font(.subheadline.bold())
                            Text(proManager.isPro
                                ? "Open in Numbers, Excel, or any spreadsheet app"
                                : "Pro · Open in Numbers, Excel, or any spreadsheet app")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        Spacer()
                        if proManager.isPro {
                            Button("Export") { exportCsv() }
                                .buttonStyle(.borderedProminent)
                                .controlSize(.small)
                        } else {
                            NavigationLink("Upgrade", destination: UpgradeView())
                                .buttonStyle(.bordered)
                                .controlSize(.small)
                        }
                    }
                }

                // About
                Section("About") {
                    HStack {
                        Text("Version")
                        Spacer()
                        Text("1.0").foregroundColor(.secondary)
                    }
                    HStack {
                        Text("Data Storage")
                        Spacer()
                        Text("On-device only").foregroundColor(.secondary)
                    }
                    Text("Exercise images from free-exercise-db (github.com/yuhonas/free-exercise-db). Open source.")
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
            }
            .navigationTitle("Settings")
            .sheet(item: $shareItem) { item in
                ShareSheet(url: item.url)
            }
            .task { await healthKit.refresh() }
        }
    }

    private func exportCsv() {
        let descriptor = FetchDescriptor<Workout>(
            predicate: #Predicate { $0.endTime != nil },
            sortBy: [SortDescriptor(\.startTime)]
        )
        let workouts = (try? modelContext.fetch(descriptor)) ?? []
        let csv = CsvExporter.generate(from: workouts)
        if let url = CsvExporter.writeToTempFile(csv) {
            shareItem = ShareItem(url: url)
        }
    }
}

// MARK: - Apple Health row

private struct HealthKitRow: View {
    let isPro: Bool
    let isAuthorized: Bool
    let isEnabled: Bool
    let onAuthorize: () -> Void
    let onToggle: (Bool) -> Void

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "heart.fill")
                .foregroundColor(.red)
            VStack(alignment: .leading) {
                Text("Apple Health").font(.subheadline.bold())
                Text(subtitle).font(.caption).foregroundColor(.secondary)
            }
            Spacer()
            cta
        }
    }

    private var subtitle: String {
        switch (isPro, isAuthorized, isEnabled) {
        case (false, _, _): return "Pro · Sync workouts to Apple Health"
        case (true, false, _): return "Grant access to write workouts"
        case (true, true, true): return "Connected — finished workouts will sync"
        case (true, true, false): return "Connected — sync is paused"
        }
    }

    @ViewBuilder
    private var cta: some View {
        if !isPro {
            NavigationLink("Upgrade", destination: UpgradeView())
                .buttonStyle(.bordered)
                .controlSize(.small)
        } else if !isAuthorized {
            Button("Connect", action: onAuthorize)
                .buttonStyle(.borderedProminent)
                .controlSize(.small)
        } else {
            Toggle("", isOn: Binding(get: { isEnabled }, set: onToggle))
                .labelsHidden()
        }
    }
}

// MARK: - CSV export helpers

private struct ShareItem: Identifiable {
    let id = UUID()
    let url: URL
}

private struct ShareSheet: UIViewControllerRepresentable {
    let url: URL

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: [url], applicationActivities: nil)
    }

    func updateUIViewController(_ controller: UIActivityViewController, context: Context) {}
}

private enum CsvExporter {
    /// Header is shared with the Android exporter so a CSV opened on either
    /// platform has the same columns. The "Superset" column is always blank
    /// on iOS until supersets land here.
    private static let header =
        "Date,Duration (min),Exercise,Muscle Group,Superset,Set,Warmup,Weight (kg),Reps,RPE,Notes"

    static func generate(from workouts: [Workout]) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyy-MM-dd HH:mm"

        var rows: [String] = [header]
        for workout in workouts {
            guard let end = workout.endTime else { continue }
            let date = formatter.string(from: workout.startTime)
            let duration = String(Int(end.timeIntervalSince(workout.startTime) / 60))
            let notes = csvEscape(workout.notes ?? "")

            for we in workout.exercises.sorted(by: { $0.orderIndex < $1.orderIndex }) {
                guard let exercise = we.exercise else { continue }
                let exerciseName = csvEscape(exercise.name)
                let muscleGroup = exercise.muscleGroup.displayName
                let supersetCol = we.supersetGroup.map(String.init) ?? ""

                for set in we.sets.sorted(by: { $0.setNumber < $1.setNumber }) {
                    let row = [
                        date,
                        duration,
                        exerciseName,
                        muscleGroup,
                        supersetCol,
                        String(set.setNumber),
                        set.isWarmup ? "true" : "false",
                        formatWeight(set.weightKg),
                        String(set.reps),
                        set.rpe.map(String.init) ?? "",
                        notes,
                    ].joined(separator: ",")
                    rows.append(row)
                }
            }
        }
        return rows.joined(separator: "\n") + "\n"
    }

    static func writeToTempFile(_ csv: String) -> URL? {
        let fileName = "fittrack-workouts-\(Int(Date().timeIntervalSince1970)).csv"
        let url = FileManager.default.temporaryDirectory.appendingPathComponent(fileName)
        do {
            try csv.write(to: url, atomically: true, encoding: .utf8)
            return url
        } catch {
            return nil
        }
    }

    private static func csvEscape(_ value: String) -> String {
        if value.isEmpty { return "" }
        let needsQuote = value.contains(",") || value.contains("\"")
            || value.contains("\n") || value.contains("\r")
        if !needsQuote { return value }
        return "\"\(value.replacingOccurrences(of: "\"", with: "\"\""))\""
    }

    private static func formatWeight(_ weight: Double) -> String {
        if weight == weight.rounded() {
            return "\(Int(weight))"
        }
        return String(format: "%.2f", weight)
            .replacingOccurrences(of: "0+$", with: "", options: .regularExpression)
            .replacingOccurrences(of: "\\.$", with: "", options: .regularExpression)
    }
}
