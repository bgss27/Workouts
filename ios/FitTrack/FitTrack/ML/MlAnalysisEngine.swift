import Foundation
import SwiftData

@MainActor
class MlAnalysisEngine {
    private let context: ModelContext

    init(context: ModelContext) {
        self.context = context
    }

    func analyzeAllMuscleGroups() -> [MuscleInsightData] {
        MuscleGroup.allCases
            .filter { $0 != .cardio && $0 != .fullBody }
            .compactMap { analyzeMuscleGroup($0) }
            .filter { !$0.exerciseInsights.isEmpty }
            .sorted { $0.overallScore < $1.overallScore }
    }

    func analyzeMuscleGroup(_ group: MuscleGroup) -> MuscleInsightData {
        let exercises = fetchExercises(for: group)
        var exInsights: [ExerciseInsightData] = []
        var all1RMs: [Double] = []
        var allVolumes: [Double] = []
        var allReps: [Int] = []

        for exercise in exercises {
            let sets = fetchSets(for: exercise)
            guard sets.count >= 4 else { continue }
            let sessions = groupIntoSessions(sets)
            guard sessions.count >= 2 else { continue }

            let oneRMs = sessions.map { s -> Double in
                let working = s.filter { !$0.isWarmup }
                return working.map { estimateOneRepMax($0.weightKg, $0.reps) }.max() ?? 0
            }
            let volumes = sessions.map { s in s.filter { !$0.isWarmup }.reduce(0.0) { $0 + $1.weightKg * Double($1.reps) } }

            let reg = LinearRegression.fitIndexed(oneRMs)
            let plateaued = StatisticalUtils.isPlateaued(oneRMs)
            let predicted = reg.rSquared > 0.2 ? reg.predict(Double(oneRMs.count)) : (oneRMs.last ?? 0)

            exInsights.append(ExerciseInsightData(
                exerciseName: exercise.name,
                strengthTrend: reg.trend,
                estimated1RM: oneRMs.last ?? 0,
                predicted1RMNext: predicted,
                rSquared: reg.rSquared,
                isPlateaued: plateaued,
                sessionCount: sessions.count,
                bestWeight: sessions.flatMap { $0 }.filter { !$0.isWarmup }.map(\.weightKg).max() ?? 0
            ))

            all1RMs.append(contentsOf: oneRMs)
            allVolumes.append(contentsOf: volumes)
            sessions.flatMap { $0 }.filter { !$0.isWarmup }.forEach { allReps.append($0.reps) }
        }

        let strengthReg = all1RMs.count >= 3 ? LinearRegression.fitIndexed(all1RMs) : nil
        let volumeReg = allVolumes.count >= 3 ? LinearRegression.fitIndexed(allVolumes) : nil
        let plateaued = all1RMs.count >= 4 && StatisticalUtils.isPlateaued(all1RMs)
        let plateauDur = plateaued ? max(1, all1RMs.count - 3) : 0
        let fatigue = calculateFatigue(allVolumes)
        let recovery = classifyRecovery(fatigue)
        let phase = detectPhase(allReps, allVolumes)
        let weeklyVol = estimateWeeklyVolume(group)
        let balance = calculateBalance(group)
        let score = calculateScore(strengthReg, plateaued, weeklyVol, fatigue, balance, exInsights.count)
        let recs = generateRecommendations(group, strengthReg, volumeReg, plateaued, plateauDur, fatigue, weeklyVol, balance, phase, exInsights)

        return MuscleInsightData(
            muscleGroup: group, overallScore: score,
            strengthTrend: strengthReg?.trend ?? .insufficientData,
            volumeTrend: volumeReg?.trend ?? .insufficientData,
            strengthRegression: strengthReg, volumeRegression: volumeReg,
            weeklyVolumeSets: weeklyVol, isPlateaued: plateaued,
            plateauDurationSessions: plateauDur, fatigueScore: fatigue,
            recoveryStatus: recovery, balanceScore: balance,
            trainingPhase: phase, recommendations: recs.sorted { $0.priority < $1.priority },
            exerciseInsights: exInsights
        )
    }

    // MARK: - Helpers

    private func fetchExercises(for group: MuscleGroup) -> [Exercise] {
        let raw = group.rawValue
        let descriptor = FetchDescriptor<Exercise>(predicate: #Predicate { $0.muscleGroupRaw == raw })
        return (try? context.fetch(descriptor)) ?? []
    }

    private func fetchSets(for exercise: Exercise) -> [WorkoutSet] {
        let descriptor = FetchDescriptor<WorkoutSet>()
        let allSets = (try? context.fetch(descriptor)) ?? []
        return allSets.filter { $0.workoutExercise?.exercise?.persistentModelID == exercise.persistentModelID }
    }

    private func groupIntoSessions(_ sets: [WorkoutSet]) -> [[WorkoutSet]] {
        Dictionary(grouping: sets) { $0.workoutExercise?.persistentModelID }
            .values.map { Array($0) }
    }

    private func estimateOneRepMax(_ weight: Double, _ reps: Int) -> Double {
        guard reps > 0, weight > 0 else { return 0 }
        if reps == 1 { return weight }
        return weight * (1 + Double(reps) / 30.0)
    }

    private func calculateFatigue(_ volumes: [Double]) -> Double {
        guard volumes.count >= 3 else { return 0 }
        let overall = StatisticalUtils.mean(volumes)
        let recent = StatisticalUtils.mean(Array(volumes.suffix(3)))
        guard overall > 0 else { return 0 }
        let ratio = recent / overall
        return min(100, max(0, (ratio - 0.85) * 200))
    }

    private func classifyRecovery(_ fatigue: Double) -> RecoveryStatus {
        switch fatigue {
        case ..<25: return .fresh
        case ..<50: return .moderate
        case ..<75: return .fatigued
        default: return .overtrained
        }
    }

    private func detectPhase(_ reps: [Int], _ volumes: [Double]) -> TrainingPhase {
        guard reps.count >= 5 else { return .beginner }
        let avg = Double(reps.suffix(20).reduce(0, +)) / Double(min(reps.count, 20))
        if volumes.count >= 4 {
            let recent = StatisticalUtils.mean(Array(volumes.suffix(3)))
            let prior = StatisticalUtils.mean(Array(volumes.dropLast(3).suffix(3)))
            if prior > 0 && recent / prior < 0.7 { return .deload }
        }
        switch avg {
        case ..<6: return .strength
        case ..<13: return .hypertrophy
        default: return .endurance
        }
    }

    private func estimateWeeklyVolume(_ group: MuscleGroup) -> Double {
        let fourWeeksAgo = Calendar.current.date(byAdding: .day, value: -28, to: Date())!
        let descriptor = FetchDescriptor<Workout>(predicate: #Predicate { $0.startTime >= fourWeeksAgo && $0.endTime != nil })
        let workouts = (try? context.fetch(descriptor)) ?? []
        var total = 0
        for w in workouts {
            for we in w.exercises {
                if we.exercise?.muscleGroup == group {
                    total += we.sets.filter { !$0.isWarmup }.count
                }
            }
        }
        return Double(total) / 4.0
    }

    private func calculateBalance(_ group: MuscleGroup) -> Double {
        let vols = MuscleGroup.allCases.filter { $0 != .cardio && $0 != .fullBody }
            .map { estimateWeeklyVolume($0) }.filter { $0 > 0 }
        guard !vols.isEmpty else { return 50 }
        let avg = vols.reduce(0, +) / Double(vols.count)
        guard avg > 0 else { return 50 }
        return min(100, (estimateWeeklyVolume(group) / avg) * 50)
    }

    private func calculateScore(_ reg: LinearRegressionResult?, _ plateaued: Bool, _ vol: Double, _ fatigue: Double, _ balance: Double, _ exCount: Int) -> Double {
        var s = 50.0
        if let r = reg {
            switch r.trend {
            case .improving: s += 25
            case .slightlyImproving: s += 15
            case .flat: s += 5
            case .slightlyDeclining: s -= 5
            case .declining: s -= 15
            default: break
            }
        }
        s += (10...20 ~= vol) ? 15 : (vol > 0 ? 7 : -10)
        if plateaued { s -= 10 }
        s -= fatigue / 10
        s += (balance - 50) / 10
        s += Double(min(exCount, 5))
        return min(100, max(0, s))
    }

    private func generateRecommendations(_ group: MuscleGroup, _ sReg: LinearRegressionResult?, _ vReg: LinearRegressionResult?, _ plateaued: Bool, _ platDur: Int, _ fatigue: Double, _ vol: Double, _ balance: Double, _ phase: TrainingPhase, _ exInsights: [ExerciseInsightData]) -> [MlRecommendation] {
        var recs: [MlRecommendation] = []
        let name = group.displayName

        if plateaued && platDur >= 3 {
            let conf = min(1.0, Double(platDur) / 8.0)
            if fatigue > 60 {
                recs.append(MlRecommendation(type: .deload, title: "Deload \(name)", description: "Strength plateaued for \(platDur) sessions with \(Int(fatigue))% fatigue. Reduce volume 40-50% for a week.", confidence: conf, priority: 1))
            } else {
                recs.append(MlRecommendation(type: .increaseVolume, title: "Break \(name) Plateau", description: "Strength flat for \(platDur) sessions. Add 2-3 sets/week for new stimulus.", confidence: conf, priority: 1))
                let stalled = exInsights.filter(\.isPlateaued)
                if !stalled.isEmpty {
                    recs.append(MlRecommendation(type: .changeExercise, title: "Swap Stalled Exercises", description: "\(stalled.map(\.exerciseName).joined(separator: ", ")) plateaued. Try different variations.", confidence: conf * 0.8, priority: 2))
                }
            }
        }

        if let r = sReg, r.trend == .declining, r.rSquared > 0.3 {
            recs.append(MlRecommendation(type: .recovery, title: "\(name) Declining", description: "Downward trend (R²=\(String(format: "%.2f", r.rSquared))). Consider extra recovery.", confidence: r.rSquared, priority: 1))
        } else if let r = sReg, r.trend == .improving, r.rSquared > 0.5 {
            recs.append(MlRecommendation(type: .increaseWeight, title: "\(name) Progressing Well", description: "Strong upward trend. Continue current approach.", confidence: r.rSquared, priority: 5))
        }

        if vol > 0 && vol < 10 {
            recs.append(MlRecommendation(type: .increaseVolume, title: "Increase \(name) Volume", description: "\(String(format: "%.0f", vol)) sets/week. Aim for 10-20 for optimal growth.", confidence: 0.7, priority: 3))
        }
        if balance < 30 {
            recs.append(MlRecommendation(type: .muscleBalance, title: "\(name) Undertrained", description: "Balance score \(Int(balance))/100 vs other groups.", confidence: 0.65, priority: 2))
        }
        if phase == .hypertrophy && plateaued {
            recs.append(MlRecommendation(type: .periodizationShift, title: "Try Strength Block", description: "Switch to 3-5 reps at 85%+ for 4-6 weeks to break plateau.", confidence: 0.6, priority: 3))
        }

        for ex in exInsights where !ex.isPlateaued && ex.strengthTrend == .improving && ex.predicted1RMNext > ex.estimated1RM * 1.02 {
            let inc = ex.predicted1RMNext - ex.estimated1RM
            recs.append(MlRecommendation(type: .increaseWeight, title: "Increase \(ex.exerciseName)", description: "Ready for +\(String(format: "%.1f", inc))kg (\(Int(ex.rSquared * 100))% confidence).", confidence: ex.rSquared, priority: 4))
        }

        return recs
    }
}
