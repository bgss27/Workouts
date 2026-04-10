import Foundation
import SwiftData

@Model
final class Workout {
    var startTime: Date
    var endTime: Date?
    var notes: String?
    @Relationship(deleteRule: .cascade) var exercises: [WorkoutExercise]

    var isCompleted: Bool { endTime != nil }

    var duration: TimeInterval? {
        guard let end = endTime else { return nil }
        return end.timeIntervalSince(startTime)
    }

    var durationMinutes: Int? {
        guard let dur = duration else { return nil }
        return Int(dur / 60)
    }

    init(startTime: Date = Date(), endTime: Date? = nil, notes: String? = nil) {
        self.startTime = startTime
        self.endTime = endTime
        self.notes = notes
        self.exercises = []
    }
}
