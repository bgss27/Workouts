import Foundation
import SwiftData

@Model
final class WorkoutExercise {
    var exercise: Exercise?
    var orderIndex: Int
    @Relationship(deleteRule: .cascade) var sets: [WorkoutSet]
    var workout: Workout?

    init(exercise: Exercise, orderIndex: Int) {
        self.exercise = exercise
        self.orderIndex = orderIndex
        self.sets = []
    }
}
