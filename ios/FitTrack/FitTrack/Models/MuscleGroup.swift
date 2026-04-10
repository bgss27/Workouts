import Foundation

enum MuscleGroup: String, Codable, CaseIterable, Identifiable {
    case chest = "Chest"
    case back = "Back"
    case shoulders = "Shoulders"
    case biceps = "Biceps"
    case triceps = "Triceps"
    case legs = "Legs"
    case glutes = "Glutes"
    case abs = "Abs"
    case forearms = "Forearms"
    case calves = "Calves"
    case fullBody = "Full Body"
    case cardio = "Cardio"

    var id: String { rawValue }
    var displayName: String { rawValue }
}
