import Foundation
import SwiftData

@Model
final class UserPlan {
    var routine: Routine?
    var programName: String?
    var dayOrder: Int
    var addedAt: Date

    init(routine: Routine, programName: String? = nil, dayOrder: Int = 0) {
        self.routine = routine
        self.programName = programName
        self.dayOrder = dayOrder
        self.addedAt = Date()
    }
}
