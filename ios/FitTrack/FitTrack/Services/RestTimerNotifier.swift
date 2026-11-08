import Foundation
import UserNotifications

/// Local-notification backing for the rest timer, so the "rest over" alert
/// reaches the user when the phone is locked between sets. Mirrors
/// Android's `RestTimerNotifier`.
///
/// While the app is in the foreground the in-app beep/haptic already fires,
/// so the delegate suppresses the banner there.
final class RestTimerNotifier: NSObject, UNUserNotificationCenterDelegate, @unchecked Sendable {
    static let shared = RestTimerNotifier()

    /// Settings toggle; default on.
    static let alertsEnabledKey = "rest_alerts_enabled"
    /// Global default rest length in seconds; default 90.
    static let defaultRestKey = "rest_default_seconds"
    static let restChoices = [30, 45, 60, 90, 120, 150, 180, 240, 300]

    private static let requestID = "fittrack.rest_timer"

    static var alertsEnabled: Bool {
        let defaults = UserDefaults.standard
        return defaults.object(forKey: alertsEnabledKey) == nil ? true : defaults.bool(forKey: alertsEnabledKey)
    }

    static var defaultRestSeconds: Int {
        let stored = UserDefaults.standard.integer(forKey: defaultRestKey)
        return stored > 0 ? stored : 90
    }

    /// "45s" under a minute, otherwise "m:ss".
    static func label(_ seconds: Int) -> String {
        seconds < 60 ? "\(seconds)s" : String(format: "%d:%02d", seconds / 60, seconds % 60)
    }

    /// Call once at launch so foreground presentation is suppressed.
    func install() {
        UNUserNotificationCenter.current().delegate = self
    }

    /// Replace any pending alert with one for `endsAt`. Asks for permission
    /// the first time; a declined prompt simply means no alert.
    func schedule(endsAt: Date, exerciseName: String?) {
        let center = UNUserNotificationCenter.current()
        center.removePendingNotificationRequests(withIdentifiers: [Self.requestID])
        guard Self.alertsEnabled else { return }
        let delay = endsAt.timeIntervalSinceNow
        guard delay > 1 else { return }

        center.getNotificationSettings { settings in
            switch settings.authorizationStatus {
            case .notDetermined:
                center.requestAuthorization(options: [.alert, .sound]) { granted, _ in
                    if granted { Self.add(delay: delay, exerciseName: exerciseName) }
                }
            case .authorized, .provisional, .ephemeral:
                Self.add(delay: delay, exerciseName: exerciseName)
            default:
                break
            }
        }
    }

    func cancel() {
        let center = UNUserNotificationCenter.current()
        center.removePendingNotificationRequests(withIdentifiers: [Self.requestID])
        center.removeDeliveredNotifications(withIdentifiers: [Self.requestID])
    }

    private static func add(delay: TimeInterval, exerciseName: String?) {
        let content = UNMutableNotificationContent()
        content.title = "Rest over"
        content.body = exerciseName.map { "Back to \($0)" } ?? "Next set"
        content.sound = .default
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: max(1, delay), repeats: false)
        let request = UNNotificationRequest(identifier: requestID, content: content, trigger: trigger)
        UNUserNotificationCenter.current().add(request)
    }

    // MARK: UNUserNotificationCenterDelegate

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        // Foreground: the in-app timer already beeps and buzzes.
        completionHandler([])
    }
}
