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
