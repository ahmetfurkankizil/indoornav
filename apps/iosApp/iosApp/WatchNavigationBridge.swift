import Foundation
import WatchConnectivity

/// Sends active iPhone AR navigation state to a paired Apple Watch companion app.
final class WatchNavigationBridge: NSObject, ObservableObject {
    static let shared = WatchNavigationBridge()

    var onEndRouteRequested: (() -> Void)?

    private var isConfigured = false
    private var lastPayload: WatchNavigationPayload?
    private var lastSentAt = Date.distantPast
    private let minimumUpdateInterval: TimeInterval = 0.55

    private override init() {
        super.init()
    }

    func configureIfNeeded() {
        guard !isConfigured, WCSession.isSupported() else { return }
        isConfigured = true
        WCSession.default.delegate = self
        WCSession.default.activate()
    }

    func sendNavigationState(_ payload: WatchNavigationPayload, force: Bool = false) {
        configureIfNeeded()
        guard isConfigured else { return }

        let now = Date()
        guard force || payload != lastPayload || now.timeIntervalSince(lastSentAt) >= minimumUpdateInterval else {
            return
        }

        lastPayload = payload
        lastSentAt = now

        guard let data = try? JSONEncoder().encode(payload) else { return }
        let message: [String: Any] = [WatchNavigationMessageKey.payload: data]

        do {
            try WCSession.default.updateApplicationContext(message)
        } catch {
            print("[WatchBridge] Failed to update watch context: \(error.localizedDescription)")
        }

        if WCSession.default.isReachable {
            WCSession.default.sendMessage(message, replyHandler: nil) { error in
                print("[WatchBridge] Failed to send watch message: \(error.localizedDescription)")
            }
        }
    }

    func sendHaptic(_ haptic: WatchNavigationHaptic) {
        configureIfNeeded()
        guard isConfigured else { return }

        let message: [String: Any] = [WatchNavigationMessageKey.haptic: haptic.rawValue]
        if WCSession.default.isReachable {
            WCSession.default.sendMessage(message, replyHandler: nil) { error in
                print("[WatchBridge] Failed to send watch haptic: \(error.localizedDescription)")
            }
        } else {
            WCSession.default.transferUserInfo(message)
        }
    }

    func endNavigation() {
        sendNavigationState(.idle, force: true)
        sendHaptic(.ended)
    }
}

extension WatchNavigationBridge: WCSessionDelegate {
    func session(
        _ session: WCSession,
        activationDidCompleteWith activationState: WCSessionActivationState,
        error: Error?
    ) {
        if let error {
            print("[WatchBridge] Activation failed: \(error.localizedDescription)")
        } else {
            print("[WatchBridge] Activated: \(activationState.rawValue)")
        }
    }

    func sessionDidBecomeInactive(_ session: WCSession) {}

    func sessionDidDeactivate(_ session: WCSession) {
        session.activate()
    }

    func session(_ session: WCSession, didReceiveMessage message: [String: Any]) {
        handle(message)
    }

    func session(_ session: WCSession, didReceiveMessage message: [String: Any], replyHandler: @escaping ([String: Any]) -> Void) {
        handle(message)
        replyHandler(["ok": true])
    }

    private func handle(_ message: [String: Any]) {
        guard let command = message[WatchNavigationMessageKey.command] as? String else { return }
        if command == WatchNavigationCommand.endRoute {
            DispatchQueue.main.async { [weak self] in
                self?.onEndRouteRequested?()
            }
        }
    }
}
