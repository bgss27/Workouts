import SwiftUI
import SwiftData

/// Month calendar showing workout history at a glance, with the same shortfall
/// missed-day rule the Android Calendar tab uses: for each past complete week,
/// if the count of training days fell short of the user's plan, mark the
/// first N empty days of that week as missed (where N = shortfall).
///
/// Sunday-anchored 6×7 grid, matching the macOS Calendar / Google Calendar
/// layout the user referenced when this feature was requested.
struct CalendarView: View {
    @Query(filter: #Predicate<Workout> { $0.endTime != nil },
           sort: \Workout.startTime, order: .reverse)
    private var completedWorkouts: [Workout]
    @Query private var userPlans: [UserPlan]

    @State private var displayedMonth: Date = Calendar.current.startOfMonth(for: Date())
    @State private var selectedDate: Date? = Calendar.current.startOfDay(for: Date())
    @State private var logPastDate: Date?

    private let cal = Calendar.current

    private var workoutsByDate: [Date: [Workout]] {
        Dictionary(grouping: completedWorkouts) { cal.startOfDay(for: $0.startTime) }
    }

    /// Days/week the user's plan calls for, per muscle group → summed gives
    /// expected workouts per week. For the missed-day rule we use the COUNT
    /// of plan days (one workout per day) rather than muscle frequency.
    private var planSize: Int {
        userPlans.count
    }

    private var grid: [CalendarDay] {
        Self.buildGrid(
            month: displayedMonth,
            workoutsByDate: workoutsByDate,
            planSize: planSize,
            today: cal.startOfDay(for: Date()),
            calendar: cal
        )
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 12) {
                    monthHeader
                    weekdayHeader
                    monthGrid
                    legend
                    Divider().padding(.horizontal)
                    selectedDayPanel
                }
                .padding(.vertical, 8)
            }
            .navigationTitle("Calendar")
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button {
                        logPastDate = selectedDate ?? Date()
                    } label: {
                        Image(systemName: "plus.circle")
                    }
                    .accessibilityLabel("Log past workout")
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Today") {
                        let today = Date()
                        displayedMonth = cal.startOfMonth(for: today)
                        selectedDate = cal.startOfDay(for: today)
                    }
                }
            }
            .sheet(item: Binding(
                get: { logPastDate.map { LogDateWrapper(date: $0) } },
                set: { logPastDate = $0?.date }
            )) { wrapper in
                LogPastWorkoutView(initialDate: wrapper.date)
            }
        }
    }

    /// Identifiable wrapper so `.sheet(item:)` can hold a Date.
    private struct LogDateWrapper: Identifiable {
        var id: TimeInterval { date.timeIntervalSince1970 }
        let date: Date
    }

    // MARK: - Subviews

    private var monthHeader: some View {
        HStack {
            Button {
                if let prev = cal.date(byAdding: .month, value: -1, to: displayedMonth) {
                    displayedMonth = prev
                }
            } label: {
                Image(systemName: "chevron.left")
                    .padding(8)
            }
            Spacer()
            Text(monthLabel(displayedMonth))
                .font(.title2.bold())
            Spacer()
            Button {
                if let next = cal.date(byAdding: .month, value: 1, to: displayedMonth) {
                    displayedMonth = next
                }
            } label: {
                Image(systemName: "chevron.right")
                    .padding(8)
            }
        }
        .padding(.horizontal)
    }

    private var weekdayHeader: some View {
        HStack {
            ForEach(["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"], id: \.self) { label in
                Text(label)
                    .font(.caption2)
                    .foregroundColor(.secondary)
                    .frame(maxWidth: .infinity)
            }
        }
        .padding(.horizontal, 8)
    }

    private var monthGrid: some View {
        VStack(spacing: 4) {
            ForEach(0..<6, id: \.self) { row in
                HStack(spacing: 4) {
                    ForEach(0..<7, id: \.self) { col in
                        let day = grid[row * 7 + col]
                        DayCell(
                            day: day,
                            isSelected: selectedDate.map { cal.isDate(day.date, inSameDayAs: $0) } ?? false
                        )
                        .onTapGesture {
                            selectedDate = day.date
                        }
                    }
                }
            }
        }
        .padding(.horizontal, 4)
    }

    private var legend: some View {
        HStack(spacing: 16) {
            HStack(spacing: 6) {
                Circle().fill(Color.fitTrackSuccess).frame(width: 8, height: 8)
                Text("Worked out").font(.caption2).foregroundColor(.secondary)
            }
            HStack(spacing: 6) {
                Circle().fill(Color.fitTrackDanger).frame(width: 8, height: 8)
                Text("Missed").font(.caption2).foregroundColor(.secondary)
            }
            Spacer()
        }
        .padding(.horizontal)
    }

    @ViewBuilder
    private var selectedDayPanel: some View {
        if let date = selectedDate, let day = grid.first(where: { cal.isDate($0.date, inSameDayAs: date) }) {
            VStack(alignment: .leading, spacing: 8) {
                Text(longDateLabel(date))
                    .font(.headline)
                    .padding(.horizontal)

                if !day.workouts.isEmpty {
                    ForEach(day.workouts, id: \.persistentModelID) { workout in
                        CalendarWorkoutRow(workout: workout)
                            .padding(.horizontal)
                    }
                } else if day.isMissed {
                    VStack(spacing: 10) {
                        HStack(spacing: 10) {
                            Image(systemName: "exclamationmark.triangle.fill")
                                .foregroundColor(.fitTrackDanger)
                            Text("Missed — no workout logged")
                                .font(.subheadline)
                        }
                        Button {
                            logPastDate = date
                        } label: {
                            Label("Log workout for this day", systemImage: "plus.circle.fill")
                                .font(.subheadline.bold())
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.borderedProminent)
                    }
                    .padding()
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color.fitTrackDanger.opacity(0.1))
                    .cornerRadius(10)
                    .padding(.horizontal)
                } else if cal.isDateInToday(date) {
                    VStack(spacing: 8) {
                        HStack(spacing: 10) {
                            Image(systemName: "dumbbell.fill")
                            Text("No workout yet today")
                                .font(.subheadline)
                            Spacer()
                            NavigationLink {
                                ActiveWorkoutView(routine: nil)
                            } label: {
                                Text("Start").font(.subheadline.bold())
                            }
                        }
                        Button {
                            logPastDate = date
                        } label: {
                            Label("Log a past workout for today", systemImage: "clock.arrow.circlepath")
                                .font(.caption)
                        }
                    }
                    .padding()
                    .background(Color(.secondarySystemBackground))
                    .cornerRadius(10)
                    .padding(.horizontal)
                } else if date > Date() {
                    Text("Upcoming")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .padding(.horizontal)
                } else {
                    VStack(spacing: 8) {
                        Text("Rest day — no workout logged")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                        Button {
                            logPastDate = date
                        } label: {
                            Label("Log workout for this day", systemImage: "plus.circle.fill")
                                .font(.subheadline.bold())
                        }
                        .buttonStyle(.bordered)
                    }
                    .padding(.horizontal)
                }
            }
        } else {
            Text("Tap a day to see what you did.")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .padding()
        }
    }

    // MARK: - Helpers

    private func monthLabel(_ date: Date) -> String {
        let f = DateFormatter()
        f.dateFormat = "MMMM yyyy"
        return f.string(from: date)
    }

    private func longDateLabel(_ date: Date) -> String {
        let f = DateFormatter()
        f.dateFormat = "EEEE, MMM d"
        return f.string(from: date)
    }
}

// MARK: - Day cell

private struct CalendarDay {
    let date: Date
    let inMonth: Bool
    let isToday: Bool
    let workouts: [Workout]
    let isMissed: Bool
}

private struct DayCell: View {
    let day: CalendarDay
    let isSelected: Bool

    var body: some View {
        let hasWorkout = !day.workouts.isEmpty
        let isFuture = day.date > Calendar.current.startOfDay(for: Date())

        VStack(spacing: 2) {
            Text("\(Calendar.current.component(.day, from: day.date))")
                .font(.body)
                .fontWeight(day.isToday ? .bold : .regular)
                .foregroundColor(textColor(hasWorkout: hasWorkout, isFuture: isFuture))
            statusDot(hasWorkout: hasWorkout)
        }
        .frame(maxWidth: .infinity)
        .frame(height: 52)
        .background(background(hasWorkout: hasWorkout))
        .overlay(
            RoundedRectangle(cornerRadius: 8)
                .stroke(day.isToday && !isSelected ? Color.accentColor : .clear, lineWidth: 1.5)
        )
        .cornerRadius(8)
        .contentShape(Rectangle())
    }

    private func textColor(hasWorkout: Bool, isFuture: Bool) -> Color {
        if !day.inMonth { return .secondary.opacity(0.4) }
        if isSelected { return .white }
        if isFuture { return .secondary.opacity(0.7) }
        return .primary
    }

    private func background(hasWorkout: Bool) -> Color {
        if isSelected { return .accentColor }
        if hasWorkout { return .fitTrackSuccess.opacity(0.18) }
        if day.isMissed { return .fitTrackDanger.opacity(0.18) }
        return .clear
    }

    @ViewBuilder
    private func statusDot(hasWorkout: Bool) -> some View {
        if hasWorkout {
            Circle().fill(Color.fitTrackSuccess).frame(width: 6, height: 6)
        } else if day.isMissed {
            Circle().fill(Color.fitTrackDanger).frame(width: 6, height: 6)
        } else {
            Color.clear.frame(width: 6, height: 6)
        }
    }
}

// MARK: - Grid + missed-day computation

extension CalendarView {
    /// Build the 6×7 grid for the displayed month. Sunday-anchored: the grid
    /// starts on the Sunday on-or-before the 1st of the month and runs 42
    /// cells. Each cell carries its workout list and a derived `isMissed`.
    fileprivate static func buildGrid(
        month: Date,
        workoutsByDate: [Date: [Workout]],
        planSize: Int,
        today: Date,
        calendar: Calendar
    ) -> [CalendarDay] {
        let firstOfMonth = calendar.startOfMonth(for: month)
        // weekday: Sunday=1 … Saturday=7. Pull back to most recent Sunday.
        let weekdayOf1st = calendar.component(.weekday, from: firstOfMonth) - 1 // 0-based
        guard let gridStart = calendar.date(byAdding: .day, value: -weekdayOf1st, to: firstOfMonth) else {
            return []
        }

        let missed = computeMissedDates(
            workoutsByDate: workoutsByDate,
            planSize: planSize,
            today: today,
            calendar: calendar
        )

        return (0..<42).map { offset in
            let date = calendar.date(byAdding: .day, value: offset, to: gridStart)!
            let startOfDay = calendar.startOfDay(for: date)
            return CalendarDay(
                date: startOfDay,
                inMonth: calendar.isDate(date, equalTo: month, toGranularity: .month),
                isToday: calendar.isDate(date, inSameDayAs: today),
                workouts: workoutsByDate[startOfDay] ?? [],
                isMissed: missed.contains(startOfDay)
            )
        }
    }

    /// Weekly shortfall rule: for each ISO week that ended before this week
    /// started, if completed-workout-days < planSize, mark the FIRST N empty
    /// days of that week as missed. Bounded to the last 8 weeks; never marks
    /// days before the user's first workout.
    fileprivate static func computeMissedDates(
        workoutsByDate: [Date: [Workout]],
        planSize: Int,
        today: Date,
        calendar: Calendar
    ) -> Set<Date> {
        guard planSize > 0, let firstEver = workoutsByDate.keys.min() else { return [] }
        // Monday of the current week.
        let weekday = calendar.component(.weekday, from: today)
        // Map Sunday=1…Saturday=7 to Monday-anchored offset (Monday=0, Sunday=6).
        let daysSinceMonday = (weekday + 5) % 7
        guard let currentWeekStart = calendar.date(byAdding: .day, value: -daysSinceMonday, to: today) else {
            return []
        }
        var out: Set<Date> = []
        for weeksAgo in 1...8 {
            guard let weekStart = calendar.date(byAdding: .weekOfYear, value: -weeksAgo, to: currentWeekStart) else { continue }
            guard let weekEnd = calendar.date(byAdding: .day, value: 6, to: weekStart) else { continue }
            if weekEnd < firstEver { continue }
            let weekDates: [Date] = (0..<7).compactMap {
                calendar.date(byAdding: .day, value: $0, to: weekStart).map { calendar.startOfDay(for: $0) }
            }
            let done = weekDates.filter { workoutsByDate[$0] != nil }.count
            let shortfall = planSize - done
            guard shortfall > 0 else { continue }
            let empty = weekDates.filter { workoutsByDate[$0] == nil }
            for day in empty.prefix(shortfall) {
                out.insert(day)
            }
        }
        return out
    }
}

// MARK: - Inline workout row

/// Small completed-workout summary card used inside the selected-day panel.
/// Inlined here (rather than reusing `WorkoutHistoryRow` from HomeView.swift)
/// because that struct is file-private. Keeps the calendar self-contained.
private struct CalendarWorkoutRow: View {
    let workout: Workout

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: "checkmark.circle.fill")
                .foregroundColor(.fitTrackSuccess)
            VStack(alignment: .leading, spacing: 2) {
                Text("Workout complete")
                    .font(.subheadline.bold())
                Text(supportLine)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            Spacer()
        }
        .padding()
        .background(Color.fitTrackSuccess.opacity(0.1))
        .cornerRadius(10)
    }

    private var supportLine: String {
        let exerciseCount = workout.exercises.count
        if let endTime = workout.endTime {
            let minutes = Int(endTime.timeIntervalSince(workout.startTime) / 60)
            return "\(exerciseCount) exercises · \(minutes)min"
        }
        return "\(exerciseCount) exercises · in progress"
    }
}

// MARK: - Calendar helper

private extension Calendar {
    func startOfMonth(for date: Date) -> Date {
        let components = self.dateComponents([.year, .month], from: date)
        return self.date(from: components) ?? date
    }
}
