import Foundation

/// What a set of this exercise records. Mirrors Android's `TrackingMode`;
/// raw values are shared across platforms (CSV, future import).
enum TrackingMode: String, Codable, CaseIterable, Identifiable {
    case weightReps = "WEIGHT_REPS"
    case duration = "DURATION"
    case weightDuration = "WEIGHT_DURATION"
    case distanceDuration = "DISTANCE_DURATION"
    case weightDistance = "WEIGHT_DISTANCE"

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .weightReps: return "Weight & reps"
        case .duration: return "Duration"
        case .weightDuration: return "Weight & duration"
        case .distanceDuration: return "Distance & duration"
        case .weightDistance: return "Weight & distance"
        }
    }

    /// One-line hint for pickers.
    var example: String {
        switch self {
        case .weightReps: return "Bench press, squat"
        case .duration: return "Plank, dead hang"
        case .weightDuration: return "Weighted plank, suitcase hold"
        case .distanceDuration: return "Run, row, bike"
        case .weightDistance: return "Farmer's carry, sled push"
        }
    }

    var usesWeight: Bool { self == .weightReps || self == .weightDuration || self == .weightDistance }
    var usesReps: Bool { self == .weightReps }
    var usesDuration: Bool { self == .duration || self == .weightDuration || self == .distanceDuration }
    var usesDistance: Bool { self == .distanceDuration || self == .weightDistance }

    /// Carries are measured in metres/yards; cardio in km/miles.
    var distanceIsShort: Bool { self == .weightDistance }

    /// SF Symbol shown next to non-default modes.
    var symbol: String? {
        switch self {
        case .weightReps: return nil
        case .duration, .weightDuration: return "timer"
        case .distanceDuration: return "figure.run"
        case .weightDistance: return "arrow.left.and.right"
        }
    }
}

/// "1:30" ⇄ 90. Shared with Android's `DurationFormat`.
enum DurationFormat {
    /// Accepts "90" (seconds), "1:30" (m:ss) or "1:02:03" (h:mm:ss).
    static func parse(_ text: String) -> Int? {
        let trimmed = text.trimmingCharacters(in: .whitespaces)
        guard !trimmed.isEmpty else { return nil }
        let parts = trimmed.split(separator: ":", omittingEmptySubsequences: false).map(String.init)
        guard parts.count <= 3, parts.allSatisfy({ !$0.isEmpty && $0.allSatisfy(\.isNumber) }) else { return nil }
        let numbers = parts.compactMap { Int($0) }
        guard numbers.count == parts.count else { return nil }
        var total = 0
        for n in numbers { total = total * 60 + n }
        return total > 0 ? total : nil
    }

    /// "0:45", "1:30", "1:02:03".
    static func format(_ seconds: Int) -> String {
        let s = max(0, seconds)
        let h = s / 3600, m = (s % 3600) / 60, sec = s % 60
        return h > 0 ? String(format: "%d:%02d:%02d", h, m, sec) : String(format: "%d:%02d", m, sec)
    }
}

/// Distance entry/display units. Storage is always metres.
enum DistanceUnits {
    enum Unit: String {
        case meters = "m", kilometers = "km", yards = "yd", miles = "mi"

        var metersPerUnit: Double {
            switch self {
            case .meters: return 1
            case .kilometers: return 1000
            case .yards: return 0.9144
            case .miles: return 1609.344
            }
        }
    }

    /// Carries use short units; cardio uses long ones. Imperial follows the weight unit.
    static func unit(for mode: TrackingMode, weightUnit: String) -> Unit {
        let imperial = weightUnit == "lbs"
        if mode.distanceIsShort { return imperial ? .yards : .meters }
        return imperial ? .miles : .kilometers
    }

    static func toMeters(_ value: Double, unit: Unit) -> Double { value * unit.metersPerUnit }

    static func fromMeters(_ meters: Double, unit: Unit) -> Double { meters / unit.metersPerUnit }

    /// Value in `unit` with up to two decimals, zeros trimmed: "5.2", "30", "0.75".
    static func valueText(_ meters: Double, unit: Unit) -> String {
        let value = fromMeters(meters, unit: unit)
        let rounded = (value * 100).rounded() / 100
        if rounded == rounded.rounded() { return String(Int(rounded)) }
        var text = String(format: "%.2f", rounded)
        while text.hasSuffix("0") { text.removeLast() }
        if text.hasSuffix(".") { text.removeLast() }
        return text
    }

    /// "5.2 km", "30 m".
    static func format(_ meters: Double, unit: Unit) -> String {
        "\(valueText(meters, unit: unit)) \(unit.rawValue)"
    }
}

/// Human text for one logged set, in the user's display units.
enum SetFormat {
    static func weightText(_ weightKg: Double, unit: String) -> String {
        let value = unit == "lbs" ? weightKg * 2.2046226218 : weightKg
        let snapped = (value * 2).rounded() / 2
        return snapped == snapped.rounded() ? String(Int(snapped)) : String(format: "%.1f", snapped)
    }

    /// "60 kg × 8", "1:30", "40 kg × 1:00", "5.2 km in 28:10", "40 kg × 30 m".
    static func describe(
        mode: TrackingMode,
        weightKg: Double,
        reps: Int,
        durationSeconds: Int?,
        distanceMeters: Double?,
        weightUnit: String
    ) -> String {
        let weight = "\(weightText(weightKg, unit: weightUnit)) \(weightUnit)"
        let duration = DurationFormat.format(durationSeconds ?? 0)
        let distance = DistanceUnits.format(distanceMeters ?? 0, unit: DistanceUnits.unit(for: mode, weightUnit: weightUnit))
        switch mode {
        case .weightReps: return "\(weight) × \(reps)"
        case .duration: return duration
        case .weightDuration: return "\(weight) × \(duration)"
        case .distanceDuration: return "\(distance) in \(duration)"
        case .weightDistance: return "\(weight) × \(distance)"
        }
    }
}
