import Foundation
import SwiftData

@Model
final class Routine {
    var name: String
    var routineDescription: String
    var targetMuscleGroups: String
    var isPreBuilt: Bool
    var difficulty: String
    var daysPerWeek: Int
    var programName: String?
    var dayOrder: Int
    @Relationship(deleteRule: .cascade) var exercises: [RoutineExercise]

    init(
        name: String,
        description: String,
        targetMuscleGroups: String,
        isPreBuilt: Bool = true,
        difficulty: String = "intermediate",
        daysPerWeek: Int = 0,
        programName: String? = nil,
        dayOrder: Int = 0
    ) {
        self.name = name
        self.routineDescription = description
        self.targetMuscleGroups = targetMuscleGroups
        self.isPreBuilt = isPreBuilt
        self.difficulty = difficulty
        self.daysPerWeek = daysPerWeek
        self.programName = programName
        self.dayOrder = dayOrder
        self.exercises = []
    }
}
