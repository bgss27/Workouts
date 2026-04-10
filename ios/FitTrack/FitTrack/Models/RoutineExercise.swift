import Foundation
import SwiftData

@Model
final class RoutineExercise {
    var exercise: Exercise?
    var orderIndex: Int
    var suggestedSets: Int
    var suggestedReps: String
    var routine: Routine?

    init(exercise: Exercise, orderIndex: Int, suggestedSets: Int, suggestedReps: String) {
        self.exercise = exercise
        self.orderIndex = orderIndex
        self.suggestedSets = suggestedSets
        self.suggestedReps = suggestedReps
    }
}
