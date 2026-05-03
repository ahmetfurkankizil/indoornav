import SwiftUI
import Foundation

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

    // MARK: - Admin API

    /// Base URL for the VecturAI admin API.
    /// Simulator: localhost works. Real device: update to your Mac's LAN IP.
    static let adminAPIBaseURL = "http://192.168.1.18:8080"

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

    /// Load the bundled reviewed package on startup.
    /// Failure is non-fatal — the user can still scan a v2 QR to fetch a remote package.
    func loadPackage() {
        let result = BuildingPackageLoader.loadReviewedPackage()
        switch result {
        case .success(let config):
            reviewedConfig = config
        case .failure(let error):
            print("[FlowModel] Bundled package unavailable: \(error.description)")
            reviewedConfig = nil
        }
        state = .home
    }

    // MARK: - QR Handling

    /// Process a raw scanned QR string, handling both v1 (bundled) and v2 (remote) flows.
    /// Returns an error description string on failure, or nil on success.
    func handleQRScanned(_ raw: String) async -> String? {
        switch QRPayload.parse(raw) {
        case .failure(let error):
            return error.description
        case .success(let payload):
            switch payload.format! {
            case .bundled:
                guard let config = reviewedConfig else {
                    return "No local navigation package available"
                }
                if let error = payload.validate(against: config) { return error.description }
                confirmEntrance(fromPayload: payload)
                return nil
            case .remote(let token):
                return await fetchRemotePackage(token: token)
            }
        }
    }

    private func fetchRemotePackage(token: String) async -> String? {
        let urlString = "\(NavigationFlowModel.adminAPIBaseURL)/mobile/buildings/\(token)/nav-package"
        guard let url = URL(string: urlString) else { return "Invalid server URL" }

        let data: Data
        do {
            let (responseData, response) = try await URLSession.shared.data(from: url)
            guard let http = response as? HTTPURLResponse, http.statusCode == 200 else {
                return "Building not found or not published on server"
            }
            data = responseData
        } catch {
            return "Network error: \(error.localizedDescription)"
        }

        guard let jsonString = String(data: data, encoding: .utf8) else {
            return "Invalid response from server"
        }

        switch BuildingPackageLoader.parseUnifiedPackage(jsonString) {
        case .failure(let error):
            return "Failed to parse building data: \(error.description)"
        case .success(let config):
            reviewedConfig = config
            let marker = config.entranceMarkers.first
            validatedEntranceMarker = marker
            let displayName = marker?.displayName ?? "Entrance"
            confirmedEntrance = displayName
            state = .entranceConfirmed(entranceName: displayName)
            return nil
        }
    }

    // MARK: - State Transitions

    func startQRScan() {
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
