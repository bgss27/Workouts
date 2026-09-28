import Foundation

/// Equipment requirement for an exercise. Used to filter the catalog when the
/// user wants a workout they can do with what they have on hand — at home with
/// nothing, with dumbbells, with a pull-up bar, etc.
///
/// Single equipment per exercise: variants that need different equipment
/// (e.g. Barbell Curl vs Dumbbell Curl) are separate exercises in the catalog.
/// Mirrors the Android `Equipment` enum.
enum Equipment: String, CaseIterable, Identifiable, Codable {
    case bodyweight = "BODYWEIGHT"
    case dumbbell = "DUMBBELL"
    case barbell = "BARBELL"
    case machine = "MACHINE"
    case band = "BAND"
    case pullUpBar = "PULL_UP_BAR"

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .bodyweight: return "Bodyweight"
        case .dumbbell: return "Dumbbell"
        case .barbell: return "Barbell"
        case .machine: return "Machine / Cable"
        case .band: return "Resistance Band"
        case .pullUpBar: return "Pull-Up Bar"
        }
    }
}
