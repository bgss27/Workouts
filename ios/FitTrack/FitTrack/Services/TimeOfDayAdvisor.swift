import Foundation

enum TimeOfDay: String {
    case morning = "Morning"
    case afternoon = "Afternoon"
    case evening = "Evening"

    var icon: String {
        switch self {
        case .morning: return "sun.max.fill"
        case .afternoon: return "cloud.sun.fill"
        case .evening: return "moon.stars.fill"
        }
    }

    static var current: TimeOfDay {
        let hour = Calendar.current.component(.hour, from: Date())
        switch hour {
        case 5...11: return .morning
        case 12...16: return .afternoon
        default: return .evening
        }
    }
}

struct WarmupAdvice {
    let warmupSetsPerExercise: Int
    let warmupReps: Int
    let warmupWeightPercent: Double
    let mobilityMinutes: Int
    let description: String
}

struct WarmupSet {
    let reps: Int
    let weightPercent: Double
    let note: String

    func calculateWeight(_ workingWeight: Double) -> Double {
        (workingWeight * weightPercent * 4).rounded() / 4.0
    }
}

struct TimeBasedGuidance {
    let timeOfDay: TimeOfDay
    let warmupAdvice: WarmupAdvice
    let intensityModifier: Double
    let tip: String
    let detailedTips: [String]
}

struct TimeOfDayAdvisor {

    static func getGuidance() -> TimeBasedGuidance {
        switch TimeOfDay.current {
        case .morning: return morningGuidance()
        case .afternoon: return afternoonGuidance()
        case .evening: return eveningGuidance()
        }
    }

    static func getWarmupSets(for time: TimeOfDay) -> [WarmupSet] {
        switch time {
        case .morning:
            return [
                WarmupSet(reps: 15, weightPercent: 0.0, note: "Empty bar / bodyweight"),
                WarmupSet(reps: 12, weightPercent: 0.30, note: "30% working weight"),
                WarmupSet(reps: 10, weightPercent: 0.50, note: "50% working weight"),
                WarmupSet(reps: 6, weightPercent: 0.70, note: "70% working weight"),
                WarmupSet(reps: 3, weightPercent: 0.85, note: "85% working weight")
            ]
        case .afternoon:
            return [
                WarmupSet(reps: 12, weightPercent: 0.40, note: "40% working weight"),
                WarmupSet(reps: 8, weightPercent: 0.60, note: "60% working weight"),
                WarmupSet(reps: 4, weightPercent: 0.80, note: "80% working weight")
            ]
        case .evening:
            return [
                WarmupSet(reps: 10, weightPercent: 0.50, note: "50% working weight"),
                WarmupSet(reps: 5, weightPercent: 0.70, note: "70% working weight")
            ]
        }
    }

    static func suggestWeight(_ base: Double, for time: TimeOfDay) -> Double {
        let modifier: Double = switch time {
        case .morning: 0.90
        case .afternoon: 0.95
        case .evening: 1.0
        }
        return (base * modifier * 4).rounded() / 4.0
    }

    private static func morningGuidance() -> TimeBasedGuidance {
        TimeBasedGuidance(
            timeOfDay: .morning,
            warmupAdvice: WarmupAdvice(
                warmupSetsPerExercise: 5, warmupReps: 12,
                warmupWeightPercent: 30.0, mobilityMinutes: 10,
                description: "Body temperature is low and joints are stiff. Take extra time with 5 progressive warmup sets and 10 min of mobility."
            ),
            intensityModifier: 0.90,
            tip: "Morning workout: Start lighter, warm up thoroughly",
            detailedTips: [
                "Spend 10-15 minutes on dynamic stretching and mobility",
                "Do 5 progressive warmup sets before working sets",
                "Start with 90% of your usual working weight",
                "Focus on higher reps (8-12) rather than maximal loads",
                "Drink water and have a light snack 30 min before",
                "Save PR attempts for afternoon or evening"
            ]
        )
    }

    private static func afternoonGuidance() -> TimeBasedGuidance {
        TimeBasedGuidance(
            timeOfDay: .afternoon,
            warmupAdvice: WarmupAdvice(
                warmupSetsPerExercise: 3, warmupReps: 10,
                warmupWeightPercent: 40.0, mobilityMinutes: 5,
                description: "Body temperature is rising and joints are loosened. Standard warmup is sufficient."
            ),
            intensityModifier: 0.95,
            tip: "Afternoon workout: Good balance of readiness and energy",
            detailedTips: [
                "5 minutes of light cardio or dynamic stretching",
                "3 progressive warmup sets per exercise",
                "Train at 95% of your peak capacity",
                "Great time for moderate-to-heavy training",
                "Reaction time and coordination are near peak"
            ]
        )
    }

    private static func eveningGuidance() -> TimeBasedGuidance {
        TimeBasedGuidance(
            timeOfDay: .evening,
            warmupAdvice: WarmupAdvice(
                warmupSetsPerExercise: 2, warmupReps: 8,
                warmupWeightPercent: 50.0, mobilityMinutes: 3,
                description: "Peak performance window. Body temperature and flexibility are at their highest."
            ),
            intensityModifier: 1.0,
            tip: "Evening workout: Peak performance — go heavy!",
            detailedTips: [
                "Minimal warmup needed — 2 progressive sets is enough",
                "Body temperature and flexibility are at peak",
                "Best time for PR attempts and heavy compounds",
                "Strength output is 5-10% higher than morning",
                "Avoid intense training within 2 hours of bedtime"
            ]
        )
    }
}

// MARK: - Warmup catalog

/// A single warmup or mobility movement. `prescription` is the volume cue
/// ("2 × 10 reps"); `note` is an optional one-liner shown in smaller text.
struct WarmupExercise: Identifiable {
    let id = UUID()
    let name: String
    let prescription: String
    let note: String?

    init(_ name: String, _ prescription: String, note: String? = nil) {
        self.name = name
        self.prescription = prescription
        self.note = note
    }
}

struct WarmupSection: Identifiable {
    let id = UUID()
    let title: String
    let exercises: [WarmupExercise]
}

/// Surfaces concrete warmup exercises tailored to the muscle groups being
/// trained. Mirror of Android's `WarmupCatalog.kt` — keep both in sync.
enum WarmupCatalog {

    private static let maxActivationSections = 3

    private static let general: [WarmupExercise] = [
        WarmupExercise("Arm circles", "30s forward, 30s backward"),
        WarmupExercise("Leg swings", "10 each side, front/back & lateral"),
        WarmupExercise("Hip circles", "10 each direction"),
        WarmupExercise("Bodyweight squats", "10 reps", note: "Slow and controlled"),
        WarmupExercise("Cat-cow stretches", "10 reps"),
    ]

    private static let byMuscleGroup: [MuscleGroup: [WarmupExercise]] = [
        .chest: [
            WarmupExercise("Scapular push-ups", "2 × 10 reps"),
            WarmupExercise("Band pull-aparts", "2 × 15 reps"),
            WarmupExercise("Wall slides", "2 × 10 reps"),
        ],
        .back: [
            WarmupExercise("Dead hangs", "2 × 15 seconds"),
            WarmupExercise("Scapular pulls", "2 × 10 reps", note: "Hang and pull shoulders down"),
            WarmupExercise("Band rows", "2 × 15 reps"),
        ],
        .shoulders: [
            WarmupExercise("Band shoulder dislocates", "2 × 10 reps", note: "Or broomstick / PVC"),
            WarmupExercise("Wall slides", "2 × 10 reps"),
            WarmupExercise("Empty-bar overhead press", "2 × 10 reps"),
        ],
        .biceps: [
            WarmupExercise("Light dumbbell curls", "2 × 10 reps", note: "20–30% of working weight"),
            WarmupExercise("Band curls", "2 × 15 reps"),
        ],
        .triceps: [
            WarmupExercise("Light tricep pushdowns", "2 × 10 reps", note: "30–40% of working weight"),
            WarmupExercise("Band pushdowns", "2 × 15 reps"),
            WarmupExercise("Diamond push-ups (knees)", "2 × 10 reps"),
        ],
        .legs: [
            WarmupExercise("Bodyweight squats", "2 × 10 reps"),
            WarmupExercise("Walking lunges", "10 steps per leg"),
            WarmupExercise("Goblet squats (light)", "2 × 10 reps", note: "Light dumbbell or kettlebell"),
            WarmupExercise("Leg swings", "10 each direction"),
        ],
        .glutes: [
            WarmupExercise("Glute bridges", "2 × 15 reps"),
            WarmupExercise("Clamshells", "2 × 10 per side"),
            WarmupExercise("Band lateral walks", "10 steps per side"),
        ],
        .abs: [
            WarmupExercise("Plank", "2 × 30 seconds"),
            WarmupExercise("Dead bug", "2 × 10 reps"),
            WarmupExercise("Cat-cow", "10 reps"),
        ],
        .calves: [
            WarmupExercise("Bodyweight calf raises", "2 × 15 reps"),
            WarmupExercise("Ankle circles", "10 each direction"),
        ],
        .forearms: [
            WarmupExercise("Wrist circles", "10 each direction"),
            WarmupExercise("Light grip squeezes", "30 seconds"),
        ],
    ]

    /// Build the warmup checklist for a workout that targets `groups`.
    /// General dynamic warmup is always included; per-muscle activation
    /// follows for up to `maxActivationSections` groups (preserves order,
    /// dedupes).
    static func forMuscleGroups(_ groups: [MuscleGroup]) -> [WarmupSection] {
        var sections: [WarmupSection] = [
            WarmupSection(title: "Dynamic warmup · ~5 min", exercises: general)
        ]
        var seen = Set<MuscleGroup>()
        var unique: [MuscleGroup] = []
        for g in groups where !seen.contains(g) {
            seen.insert(g)
            unique.append(g)
            if unique.count >= maxActivationSections { break }
        }
        for group in unique {
            if let exercises = byMuscleGroup[group] {
                sections.append(WarmupSection(
                    title: "Activation · \(group.displayName)",
                    exercises: exercises
                ))
            }
        }
        return sections
    }
}
