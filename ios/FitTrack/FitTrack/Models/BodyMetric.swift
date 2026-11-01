import Foundation
import SwiftData

/// One body-metrics check-in. Every measurement is optional so the user can
/// log just a weight on a busy morning and a full tape-measure session on
/// the weekend. Values are stored canonically (kg / cm / percent); unit
/// conversion for display lives in the view. Mirrors Android's `BodyMetric`.
@Model
final class BodyMetric {
    var recordedAt: Date
    var weightKg: Double?
    var bodyFatPercent: Double?
    var waistCm: Double?
    var chestCm: Double?
    var hipsCm: Double?
    var armCm: Double?
    var thighCm: Double?
    var notes: String?

    init(
        recordedAt: Date = Date(),
        weightKg: Double? = nil,
        bodyFatPercent: Double? = nil,
        waistCm: Double? = nil,
        chestCm: Double? = nil,
        hipsCm: Double? = nil,
        armCm: Double? = nil,
        thighCm: Double? = nil,
        notes: String? = nil
    ) {
        self.recordedAt = recordedAt
        self.weightKg = weightKg
        self.bodyFatPercent = bodyFatPercent
        self.waistCm = waistCm
        self.chestCm = chestCm
        self.hipsCm = hipsCm
        self.armCm = armCm
        self.thighCm = thighCm
        self.notes = notes
    }

    var hasAnyValue: Bool {
        BodyMetricType.allCases.contains { $0.value(of: self) != nil }
    }
}

/// Which measurement a chart or summary is looking at.
enum BodyMetricType: String, CaseIterable, Identifiable {
    case weight, bodyFat, waist, chest, hips, arm, thigh
    var id: String { rawValue }

    enum Kind { case mass, length, percent }

    var displayName: String {
        switch self {
        case .weight: return "Weight"
        case .bodyFat: return "Body Fat"
        case .waist: return "Waist"
        case .chest: return "Chest"
        case .hips: return "Hips"
        case .arm: return "Arm"
        case .thigh: return "Thigh"
        }
    }

    var kind: Kind {
        switch self {
        case .weight: return .mass
        case .bodyFat: return .percent
        default: return .length
        }
    }

    func value(of metric: BodyMetric) -> Double? {
        switch self {
        case .weight: return metric.weightKg
        case .bodyFat: return metric.bodyFatPercent
        case .waist: return metric.waistCm
        case .chest: return metric.chestCm
        case .hips: return metric.hipsCm
        case .arm: return metric.armCm
        case .thigh: return metric.thighCm
        }
    }

    func set(_ value: Double?, on metric: BodyMetric) {
        switch self {
        case .weight: metric.weightKg = value
        case .bodyFat: metric.bodyFatPercent = value
        case .waist: metric.waistCm = value
        case .chest: metric.chestCm = value
        case .hips: metric.hipsCm = value
        case .arm: metric.armCm = value
        case .thigh: metric.thighCm = value
        }
    }
}
