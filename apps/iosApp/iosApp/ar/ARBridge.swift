import Foundation
import Combine

/// Bridge between KMP shared navigation state and native iOS AR layer.
///
/// Connects the shared ArNavigationCoordinator to the native iOS AR
/// components (session manager, marker detector, route renderer).
///
/// TODO: Import shared KMP framework once iOS build is configured.
/// For now, this follows the ArNavigationBridge interface contract.
class ARBridge: ObservableObject {
    
    /// Human-readable label for the current session state.
    @Published var currentStateLabel: String = "Idle"
    
    /// Whether the entrance marker has been detected.
    @Published var isWorldAligned: Bool = false
    
    /// Current navigation instruction text.
    @Published var currentInstruction: String = ""
    
    /// Number of arrows currently renderable.
    @Published var arrowCount: Int = 0
    
    /// Current destination name.
    @Published var destinationName: String = ""
    
    // MARK: - ArNavigationBridge Implementation
    
    /// Start an AR session. Called by shared coordinator.
    /// TODO: Accept BuildingPackage and Room from KMP shared code.
    func startSession(buildingId: String, destinationName: String) {
        currentStateLabel = "Waiting for Marker"
        self.destinationName = destinationName
        currentInstruction = "Point camera at the entrance marker"
    }
    
    /// Stop the AR session.
    func stopSession() {
        currentStateLabel = "Idle"
        isWorldAligned = false
        currentInstruction = ""
        arrowCount = 0
    }
    
    /// Called when alignment is established.
    func onAlignmentEstablished(
        offsetX: Double, offsetY: Double, offsetZ: Double,
        rotationYDeg: Double
    ) {
        isWorldAligned = true
        currentStateLabel = "Aligned"
    }
    
    /// Called when renderable route is updated.
    func onRenderableRouteUpdated(arrowCount: Int, instruction: String) {
        self.arrowCount = arrowCount
        currentInstruction = instruction
        currentStateLabel = "Rendering Route"
    }
    
    /// Called when session state changes.
    func onSessionStateChanged(stateLabel: String) {
        currentStateLabel = stateLabel
    }
}
