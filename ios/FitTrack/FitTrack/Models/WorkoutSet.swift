import Foundation
import SwiftData

@Model
final class WorkoutSet {
    var setNumber: Int
    var reps: Int
    var weightKg: Double
    var isWarmup: Bool
    var rpe: Int?
    var workoutExercise: WorkoutExercise?

    init(setNumber: Int, reps: Int, weightKg: Double, isWarmup: Bool = false, rpe: Int? = nil) {
        self.setNumber = setNumber
        self.reps = reps
        self.weightKg = weightKg
        self.isWarmup = isWarmup
        self.rpe = rpe
    }
}
