import Foundation
import SwiftData

@Model
final class Exercise {
    var name: String
    var muscleGroupRaw: String
    var secondaryMuscleGroupRaw: String?
    var isCustom: Bool
    /// Equipment requirement. **Optional** (nullable) so SwiftData can
    /// auto-migrate an existing store additively without a destructive reset
    /// — non-optional property additions are much riskier and tend to wipe
    /// data on real devices. Legacy rows with nil are treated as BARBELL
    /// (the most common gym staple) via the computed accessor below; a
    /// one-shot retag in `SeedData.retagAndExtendIfNeeded` fixes them up
    /// to their real equipment on next launch.
    var equipmentRaw: String?

    var muscleGroup: MuscleGroup {
        get { MuscleGroup(rawValue: muscleGroupRaw) ?? .chest }
        set { muscleGroupRaw = newValue.rawValue }
    }

    var secondaryMuscleGroup: MuscleGroup? {
        get { secondaryMuscleGroupRaw.flatMap { MuscleGroup(rawValue: $0) } }
        set { secondaryMuscleGroupRaw = newValue?.rawValue }
    }

    var equipment: Equipment {
        get { equipmentRaw.flatMap { Equipment(rawValue: $0) } ?? .barbell }
        set { equipmentRaw = newValue.rawValue }
    }

    init(
        name: String,
        muscleGroup: MuscleGroup,
        secondaryMuscleGroup: MuscleGroup? = nil,
        isCustom: Bool = false,
        equipment: Equipment = .barbell
    ) {
        self.name = name
        self.muscleGroupRaw = muscleGroup.rawValue
        self.secondaryMuscleGroupRaw = secondaryMuscleGroup?.rawValue
        self.isCustom = isCustom
        self.equipmentRaw = equipment.rawValue
    }
}
