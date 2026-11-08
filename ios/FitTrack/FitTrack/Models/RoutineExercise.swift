import Foundation
import SwiftData

@Model
final class RoutineExercise {
    var exercise: Exercise?
    var orderIndex: Int
    var suggestedSets: Int
    var suggestedReps: String
    /// Rest after each set, in seconds. nil = the global default from Settings.
    var restSeconds: Int?
    var routine: Routine?

    init(exercise: Exercise, orderIndex: Int, suggestedSets: Int, suggestedReps: String) {
        self.exercise = exercise
        self.orderIndex = orderIndex
        self.suggestedSets = suggestedSets
        self.suggestedReps = suggestedReps
    }
}
