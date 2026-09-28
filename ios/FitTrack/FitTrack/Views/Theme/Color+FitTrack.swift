import SwiftUI
import UIKit

/// Semantic accent colors that aren't covered by Material 3's role-based scheme.
/// Used for trend / recommendation / completion accents. Light variants are
/// darker (better contrast on white), dark variants are lighter (don't burn
/// out on a dark surface).
///
/// Mirrors the Android `FitTrackColors`. Use these instead of `Color.green`
/// etc. so the accent reads correctly across both modes.
extension Color {
    /// Green-ish "things are going well" — completed workouts, on-track stats,
    /// improving trends.
    static let fitTrackSuccess = Color(uiColor: UIColor { traits in
        traits.userInterfaceStyle == .dark
            ? UIColor(red: 0x66 / 255, green: 0xBB / 255, blue: 0x6A / 255, alpha: 1.0)
            : UIColor(red: 0x2E / 255, green: 0x7D / 255, blue: 0x32 / 255, alpha: 1.0)
    })

    /// Amber "heads up" — plateaus, mid-range fatigue, missed-but-not-critical.
    static let fitTrackWarning = Color(uiColor: UIColor { traits in
        traits.userInterfaceStyle == .dark
            ? UIColor(red: 0xFF / 255, green: 0xA7 / 255, blue: 0x26 / 255, alpha: 1.0)
            : UIColor(red: 0xED / 255, green: 0x6C / 255, blue: 0x02 / 255, alpha: 1.0)
    })

    /// Red "something needs attention" — declining trends, missed workout
    /// days, deload prompts.
    static let fitTrackDanger = Color(uiColor: UIColor { traits in
        traits.userInterfaceStyle == .dark
            ? UIColor(red: 0xEF / 255, green: 0x53 / 255, blue: 0x50 / 255, alpha: 1.0)
            : UIColor(red: 0xD3 / 255, green: 0x2F / 255, blue: 0x2F / 255, alpha: 1.0)
    })
}
