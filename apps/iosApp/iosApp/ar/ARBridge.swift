import Foundation
import Combine

/// Bridge between KMP shared navigation state and native iOS AR layer.
///
/// This class observes the shared NavigationState (via KMP interop) and
/// translates it into data that ARKit/RealityKit can consume.
///
/// TODO: Import shared KMP framework and observe NavigationState
/// TODO: Convert route segments from nav-graph coordinates to ARKit world coordinates
/// TODO: Provide entrance marker detection results back to shared state
/// TODO: Handle coordinate system alignment (entrance marker pose → ARKit world origin)
class ARBridge: ObservableObject {

    /// Human-readable label for the current navigation state.
    @Published var currentStateLabel: String = "Idle"

    /// Whether the entrance marker has been detected and world is aligned.
    @Published var isWorldAligned: Bool = false

    /// Current route segments transformed to AR world coordinates.
    /// TODO: Replace with actual 3D position data
    @Published var arRouteSegments: [ARRouteSegment] = []

    /// Current navigation instruction text.
    @Published var currentInstruction: String = ""

    // MARK: - Shared State Observation

    /// Starts observing the shared KMP NavigationState.
    ///
    /// TODO: Use shared framework's AppStore to collect NavigationState flow:
    /// ```swift
    /// func startObserving(appStore: AppStore) {
    ///     // Use Kotlin/Native flow collection
    ///     appStore.navigationState.collect { state in
    ///         DispatchQueue.main.async {
    ///             self.updateFromSharedState(state)
    ///         }
    ///     }
    /// }
    /// ```
    func startObserving() {
        // TODO: Connect to shared KMP state
    }

    // MARK: - Marker Detection Callbacks

    /// Called when the entrance marker is successfully detected by ARKit.
    ///
    /// TODO: Extract pose from ARKit anchor
    /// TODO: Notify shared state that alignment is complete
    /// TODO: Calculate world-to-navgraph coordinate transform
    ///
    /// - Parameter anchorName: The reference image name that was detected
    func onEntranceMarkerDetected(anchorName: String) {
        isWorldAligned = true
        currentStateLabel = "Navigating"
        // TODO: Parse QR payload, update shared NavigationState
    }

    /// Called when the user reaches close proximity to the destination.
    ///
    /// TODO: Update shared state to Arrived
    /// TODO: Record visit in history via shared layer
    func onDestinationReached() {
        currentStateLabel = "Arrived"
        // TODO: Transition shared NavigationState to Arrived
    }
}

// MARK: - AR Data Models

/// A route segment transformed into AR world coordinates.
///
/// TODO: Replace with actual SIMD types once ARKit integration is implemented
struct ARRouteSegment {
    let startX: Float
    let startY: Float
    let startZ: Float
    let endX: Float
    let endY: Float
    let endZ: Float
    let instruction: String
}
