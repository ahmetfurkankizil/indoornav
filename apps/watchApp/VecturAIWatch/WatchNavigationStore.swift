import Foundation
import WatchConnectivity
import WatchKit

final class WatchNavigationStore: NSObject, ObservableObject {
    @Published var payload: WatchNavigationPayload = .idle
    @Published var connectionStatus: String = "Waiting for iPhone"

    override init() {
        super.init()
        configureSession()
    }

    func endRouteOnPhone() {
        guard WCSession.isSupported() else { return }
        let message: [String: Any] = [WatchNavigationMessageKey.command: WatchNavigationCommand.endRoute]
        if WCSession.default.isReachable {
            WCSession.default.sendMessage(message, replyHandler: nil, errorHandler: nil)
        } else {
            WCSession.default.transferUserInfo(message)
        }
    }

    private func configureSession() {
        guard WCSession.isSupported() else {
            connectionStatus = "WatchConnectivity unavailable"
            return
        }
        WCSession.default.delegate = self
        WCSession.default.activate()
    }

    private func apply(message: [String: Any]) {
        if let data = message[WatchNavigationMessageKey.payload] as? Data {
            decodePayload(data)
        }

        if let rawHaptic = message[WatchNavigationMessageKey.haptic] as? String,
           let haptic = WatchNavigationHaptic(rawValue: rawHaptic) {
            play(haptic)
        }
    }

    private func decodePayload(_ data: Data) {
        guard let next = try? JSONDecoder().decode(WatchNavigationPayload.self, from: data) else {
            return
        }
        DispatchQueue.main.async {
            self.payload = next
            self.connectionStatus = next.isActive || next.hasArrived ? "Connected" : "Ready"
        }
    }

    private func play(_ haptic: WatchNavigationHaptic) {
        DispatchQueue.main.async {
            switch haptic {
            case .routeStarted:
                WKInterfaceDevice.current().play(.start)
            case .turnImminent:
                WKInterfaceDevice.current().play(.notification)
            case .recentering:
                WKInterfaceDevice.current().play(.retry)
            case .arrived:
                WKInterfaceDevice.current().play(.success)
            case .ended:
                WKInterfaceDevice.current().play(.stop)
            }
        }
    }
}

extension WatchNavigationStore: WCSessionDelegate {
    func session(
        _ session: WCSession,
        activationDidCompleteWith activationState: WCSessionActivationState,
        error: Error?
    ) {
        DispatchQueue.main.async {
            if let error {
                self.connectionStatus = error.localizedDescription
            } else {
                self.connectionStatus = activationState == .activated ? "Ready" : "Waiting for iPhone"
            }
        }
    }

    func session(_ session: WCSession, didReceiveApplicationContext applicationContext: [String: Any]) {
        apply(message: applicationContext)
    }

    func session(_ session: WCSession, didReceiveMessage message: [String: Any]) {
        apply(message: message)
    }

    func session(_ session: WCSession, didReceiveUserInfo userInfo: [String: Any] = [:]) {
        apply(message: userInfo)
    }
}
