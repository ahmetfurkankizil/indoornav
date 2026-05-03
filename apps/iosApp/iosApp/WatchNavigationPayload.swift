import Foundation

/// Compact navigation snapshot mirrored from iPhone AR navigation to Apple Watch.
struct WatchNavigationPayload: Codable, Equatable {
    let isActive: Bool
    let hasArrived: Bool
    let destinationName: String
    let nextActionText: String
    let nextActionIcon: String
    let nextActionDistanceMeters: Double?
    let remainingDistanceMeters: Double
    let etaSeconds: Double
    let progress: Double
    let trackingStatus: String
    let isLowConfidence: Bool

    static let idle = WatchNavigationPayload(
        isActive: false,
        hasArrived: false,
        destinationName: "",
        nextActionText: "Ready",
        nextActionIcon: "location",
        nextActionDistanceMeters: nil,
        remainingDistanceMeters: 0,
        etaSeconds: 0,
        progress: 0,
        trackingStatus: "Idle",
        isLowConfidence: false
    )
}

enum WatchNavigationHaptic: String, Codable {
    case routeStarted
    case turnImminent
    case recentering
    case arrived
    case ended
}

enum WatchNavigationMessageKey {
    static let payload = "payload"
    static let haptic = "haptic"
    static let command = "command"
}

enum WatchNavigationCommand {
    static let endRoute = "endRoute"
}
