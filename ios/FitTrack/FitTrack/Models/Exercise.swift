import Foundation
import SwiftData

@Model
final class Exercise {
    var name: String
    var muscleGroupRaw: String
    var secondaryMuscleGroupRaw: String?
    var isCustom: Bool

    var muscleGroup: MuscleGroup {
        get { MuscleGroup(rawValue: muscleGroupRaw) ?? .chest }
        set { muscleGroupRaw = newValue.rawValue }
    }

    var secondaryMuscleGroup: MuscleGroup? {
        get { secondaryMuscleGroupRaw.flatMap { MuscleGroup(rawValue: $0) } }
        set { secondaryMuscleGroupRaw = newValue?.rawValue }
    }

    init(name: String, muscleGroup: MuscleGroup, secondaryMuscleGroup: MuscleGroup? = nil, isCustom: Bool = false) {
        self.name = name
        self.muscleGroupRaw = muscleGroup.rawValue
        self.secondaryMuscleGroupRaw = secondaryMuscleGroup?.rawValue
        self.isCustom = isCustom
    }
}
