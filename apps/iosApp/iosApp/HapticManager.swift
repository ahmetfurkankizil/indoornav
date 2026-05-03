import UIKit

/// Minimal haptic feedback for key navigation moments.
/// All haptics are gated by `isEnabled` and use standard iOS feedback generators.
final class HapticManager {
    static let shared = HapticManager()

    /// Set to false to silence all haptics (e.g., for testing or user preference).
    var isEnabled: Bool = true

    private let lightImpact = UIImpactFeedbackGenerator(style: .light)
    private let mediumImpact = UIImpactFeedbackGenerator(style: .medium)
    private let notification = UINotificationFeedbackGenerator()

    private init() {
        // Pre-warm generators so the first fire is instant
        lightImpact.prepare()
        mediumImpact.prepare()
        notification.prepare()
    }

    /// Medium impact when AR alignment locks and navigation begins.
    func routeStarted() {
        guard isEnabled else { return }
        mediumImpact.impactOccurred()
        WatchNavigationBridge.shared.sendHaptic(.routeStarted)
    }

    /// Warning notification when the user is within ~2 m of a turn.
    func turnImminent() {
        guard isEnabled else { return }
        notification.notificationOccurred(.warning)
        WatchNavigationBridge.shared.sendHaptic(.turnImminent)
    }

    /// Light impact when tracking degrades and the system is re-centering.
    func recentering() {
        guard isEnabled else { return }
        lightImpact.impactOccurred()
        WatchNavigationBridge.shared.sendHaptic(.recentering)
    }

    /// Success notification when the user arrives at the destination.
    func arrived() {
        guard isEnabled else { return }
        notification.notificationOccurred(.success)
        WatchNavigationBridge.shared.sendHaptic(.arrived)
    }
}
