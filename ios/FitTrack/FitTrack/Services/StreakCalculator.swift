import Foundation

/// Weekly training streak: consecutive Monday-anchored weeks with at least
/// one completed workout. Mirrors Android's `StreakCalculator`.
struct Streak: Equatable {
    /// Weeks in a row ending at this week, or last week if this week is still empty.
    let currentWeeks: Int
    let longestWeeks: Int
    let workoutsThisWeek: Int

    static let empty = Streak(currentWeeks: 0, longestWeeks: 0, workoutsThisWeek: 0)
}

enum StreakCalculator {
    /// Monday 00:00 of the week containing `date`.
    static func startOfWeek(_ date: Date, calendar: Calendar = .current) -> Date {
        let weekday = calendar.component(.weekday, from: date)
        let daysSinceMonday = (weekday + 5) % 7
        return calendar.date(byAdding: .day, value: -daysSinceMonday, to: calendar.startOfDay(for: date)) ?? date
    }

    /// `completedStartTimes` are start times of finished workouts, any order.
    /// The current week counts once it has a workout; an empty current week
    /// doesn't break the streak until the week is over, so a Monday-morning
    /// Home visit still shows last week's run.
    static func compute(completedStartTimes: [Date], today: Date = Date(), calendar: Calendar = .current) -> Streak {
        guard !completedStartTimes.isEmpty else { return .empty }
        let weekStarts = Set(completedStartTimes.map { startOfWeek($0, calendar: calendar) })
        let thisWeek = startOfWeek(today, calendar: calendar)
        let workoutsThisWeek = completedStartTimes.filter { startOfWeek($0, calendar: calendar) == thisWeek }.count

        func minusWeek(_ d: Date) -> Date { calendar.date(byAdding: .day, value: -7, to: d) ?? d }

        var cursor = weekStarts.contains(thisWeek) ? thisWeek : minusWeek(thisWeek)
        var current = 0
        while weekStarts.contains(cursor) {
            current += 1
            cursor = minusWeek(cursor)
        }

        var longest = 0
        var run = 0
        var prev: Date?
        for w in weekStarts.sorted() {
            if let p = prev, calendar.dateComponents([.day], from: p, to: w).day == 7 {
                run += 1
            } else {
                run = 1
            }
            longest = max(longest, run)
            prev = w
        }
        return Streak(currentWeeks: current, longestWeeks: longest, workoutsThisWeek: workoutsThisWeek)
    }
}
