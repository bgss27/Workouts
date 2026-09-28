import Foundation
import SwiftData

@Model
final class WorkoutExercise {
    var exercise: Exercise?
    var orderIndex: Int
    /// Identifies a superset group within this workout. All workout-exercises
    /// sharing the same non-nil value are performed back-to-back. nil =
    /// standalone (default). SwiftData migrates additively — existing rows
    /// get nil automatically.
    var supersetGroup: Int?
    @Relationship(deleteRule: .cascade) var sets: [WorkoutSet]
    var workout: Workout?

    init(exercise: Exercise, orderIndex: Int, supersetGroup: Int? = nil) {
        self.exercise = exercise
        self.orderIndex = orderIndex
        self.supersetGroup = supersetGroup
        self.sets = []
    }
}
