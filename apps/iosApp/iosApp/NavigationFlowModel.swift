import SwiftUI

/// App-level navigation state machine for the iOS truth-path flow.
///
/// States:
///   home → qrScan → entranceConfirmed → destinationSelect → routePreview → arNavigation
///   packageError (terminal until retry)
///
/// This model is the single source of truth for where the user is in the flow.
/// AR cannot start without an explicit destination selection.
///
/// Phase 4: QR payload is validated against the reviewed package. The validated
/// entrance marker is passed through to the AR alignment flow.
@MainActor
class NavigationFlowModel: ObservableObject {

    enum FlowState: Equatable {
        case home
        case qrScan
        case entranceConfirmed(entranceName: String)
        case destinationSelect
        case routePreview
        case arNavigation
        case packageError(message: String)
    }

    @Published var state: FlowState = .home

    // MARK: - Session Data

    /// The entrance name confirmed after QR scan.
    @Published var confirmedEntrance: String = ""

    /// The validated entrance marker from the reviewed package.
    @Published var validatedEntranceMarker: BuildingPackageLoader.PackageMarker?

    /// The loaded reviewed package config. Populated on app start.
    @Published var reviewedConfig: BuildingPackageLoader.ReviewedConfig?

    /// Available rooms from the reviewed package.
    var availableRooms: [BuildingPackageLoader.PackageRoom] {
        reviewedConfig?.rooms ?? []
    }

    /// The user-selected destination room. Must be non-nil before AR can start.
    @Published var selectedRoom: BuildingPackageLoader.PackageRoom?

    /// Precomputed route package for the selected destination.
    @Published var routePackage: BuildingPackageLoader.LoadedPackage?

    // MARK: - Initialization

    init() {
        loadPackage()
    }

    // MARK: - Package Loading

    /// Load the reviewed package on startup. If it fails, the app enters
    /// a packageError state and will not silently fall back to draft data.
    func loadPackage() {
        let result = BuildingPackageLoader.loadReviewedPackage()
        switch result {
        case .success(let config):
            reviewedConfig = config
            state = .home
        case .failure(let error):
            reviewedConfig = nil
            state = .packageError(message: error.description)
        }
    }

    // MARK: - QR Payload Validation

    /// Validate a scanned QR payload against the loaded reviewed package.
    /// Returns nil if valid, or the validation error if invalid.
    func validateQRPayload(_ payload: QRPayload) -> QRPayload.PayloadError? {
        guard let config = reviewedConfig else {
            return .notJSON // no config loaded
        }
        return payload.validate(against: config)
    }

    // MARK: - State Transitions

    func startQRScan() {
        guard reviewedConfig != nil else { return }
        state = .qrScan
    }

    /// Confirm entrance from a validated QR payload.
    /// Looks up the entrance marker in the reviewed package.
    func confirmEntrance(fromPayload payload: QRPayload) {
        guard let config = reviewedConfig else { return }

        let marker = config.entranceMarkers.first { $0.id == payload.entranceId }
        validatedEntranceMarker = marker

        let displayName = marker?.displayName ?? "Entrance"
        confirmedEntrance = displayName
        state = .entranceConfirmed(entranceName: displayName)
    }

    /// Legacy entrance confirmation (for backward compatibility / testing).
    func confirmEntrance(name: String) {
        confirmedEntrance = name
        // Use first entrance marker as default
        validatedEntranceMarker = reviewedConfig?.entranceMarkers.first
        state = .entranceConfirmed(entranceName: name)
    }

    func proceedToDestinationSelect() {
        state = .destinationSelect
    }

    func selectDestination(_ room: BuildingPackageLoader.PackageRoom) {
        selectedRoom = room
        if let config = reviewedConfig {
            routePackage = BuildingPackageLoader.computeRoute(
                config: config,
                destinationRoomId: room.id
            )
        }
        state = .routePreview
    }

    func startNavigation() {
        guard selectedRoom != nil, routePackage != nil else { return }
        state = .arNavigation
    }

    func endNavigation() {
        selectedRoom = nil
        routePackage = nil
        confirmedEntrance = ""
        validatedEntranceMarker = nil
        state = .home
    }

    func goBackToDestinationSelect() {
        selectedRoom = nil
        routePackage = nil
        state = .destinationSelect
    }

    func retryPackageLoad() {
        loadPackage()
    }
}
