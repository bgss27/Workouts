import Foundation

/// Improvement-tip surfaced on Home. Mirrors the Android `Suggestion` sealed
/// class — Swift uses a struct with a `kind` discriminator so SwiftUI can
/// pattern-match on it without exhaustive enum-with-associated-values
/// gymnastics in the UI layer.
struct Suggestion: Identifiable, Equatable {
    let id = UUID()
    let kind: Kind
    let title: String
    let description: String
    /// Lower = more important. Used for sorting the list before display.
    let priority: Int
    /// For actionable suggestions — the muscle group the action targets.
    let muscleGroup: MuscleGroup?

    /// Fields specific to `.increaseFrequency` — kept on the parent struct
    /// (rather than associated-value enum) so call sites don't need to
    /// switch on kind to read them.
    let sessionsLast4Weeks: Int
    let suggestedFreqPerWeek: Int

    enum Kind: Equatable {
        case increaseWeight
        case increaseVolume
        case increaseFrequency
        case deload
        case tryExercise
        case muscleGroupSummary
    }

    /// Whether tapping this suggestion does anything. Only frequency
    /// suggestions are wired to the swap flow today.
    var isActionable: Bool { kind == .increaseFrequency }

    // MARK: - Builders mirroring Android's data classes

    static func increaseFrequency(muscleGroup: MuscleGroup, sessionsLast4Weeks: Int, suggestedFreqPerWeek: Int) -> Suggestion {
        let group = muscleGroup.displayName
        let title = "Train \(group) More Often"
        let description: String
        if sessionsLast4Weeks == 0 {
            description = "You haven't trained \(group) in the last 4 weeks. Your plan calls for \(suggestedFreqPerWeek)x/week."
        } else {
            let expected = suggestedFreqPerWeek * 4
            let plural = sessionsLast4Weeks == 1 ? "time" : "times"
            description = "You've trained \(group) \(sessionsLast4Weeks) \(plural) in the last 4 weeks. Your plan calls for \(suggestedFreqPerWeek)x/week (\(expected) sessions)."
        }
        return Suggestion(
            kind: .increaseFrequency,
            title: title,
            description: description,
            priority: 3,
            muscleGroup: muscleGroup,
            sessionsLast4Weeks: sessionsLast4Weeks,
            suggestedFreqPerWeek: suggestedFreqPerWeek
        )
    }
}
