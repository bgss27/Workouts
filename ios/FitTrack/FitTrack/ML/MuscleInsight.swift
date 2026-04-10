import Foundation

enum RecoveryStatus: String {
    case fresh = "Well Recovered"
    case moderate = "Moderately Recovered"
    case fatigued = "Needs Recovery"
    case overtrained = "Overtrained"
}

enum TrainingPhase: String {
    case beginner = "Beginner"
    case strength = "Strength Phase"
    case hypertrophy = "Hypertrophy Phase"
    case endurance = "Endurance Phase"
    case deload = "Deload Phase"
    case maintenance = "Maintenance"

    var description: String {
        switch self {
        case .beginner: return "Focus on learning movement patterns and building base strength"
        case .strength: return "Low reps, heavy weight - building maximal strength"
        case .hypertrophy: return "Moderate reps and weight - maximizing muscle growth"
        case .endurance: return "High reps, lower weight - building muscular endurance"
        case .deload: return "Reduced volume/intensity for recovery"
        case .maintenance: return "Stable training to maintain current fitness"
        }
    }
}

struct MuscleInsightData: Identifiable {
    let id = UUID()
    let muscleGroup: MuscleGroup
    let overallScore: Double
    let strengthTrend: TrendDirection
    let volumeTrend: TrendDirection
    let strengthRegression: LinearRegressionResult?
    let volumeRegression: LinearRegressionResult?
    let weeklyVolumeSets: Double
    let isPlateaued: Bool
    let plateauDurationSessions: Int
    let fatigueScore: Double
    let recoveryStatus: RecoveryStatus
    let balanceScore: Double
    let trainingPhase: TrainingPhase
    let recommendations: [MlRecommendation]
    let exerciseInsights: [ExerciseInsightData]
}

struct ExerciseInsightData: Identifiable {
    let id = UUID()
    let exerciseName: String
    let strengthTrend: TrendDirection
    let estimated1RM: Double
    let predicted1RMNext: Double
    let rSquared: Double
    let isPlateaued: Bool
    let sessionCount: Int
    let bestWeight: Double
}

struct MlRecommendation: Identifiable {
    let id = UUID()
    let type: RecommendationType
    let title: String
    let description: String
    let confidence: Double
    let priority: Int
}

enum RecommendationType: String {
    case increaseWeight, increaseVolume, increaseFrequency
    case deload, changeExercise, periodizationShift
    case muscleBalance, recovery
}
