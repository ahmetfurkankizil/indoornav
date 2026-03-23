import Foundation
import ARKit

/// Detects entrance marker reference images and reports alignment events.
///
/// Phase 5: On real devices, only accepts the specifically configured entrance
/// marker image — no fallback to "any image anchor". Simulator retains a
/// simulated alignment path.
class ARMarkerDetector: NSObject {

    /// Known markers: key is referenceImageName, value is metadata + role
    struct KnownMarker {
        let markerId: String
        let role: MarkerDetectionRole
        let buildingX: Double
        let buildingY: Double
        let buildingZ: Double
        let buildingRotationYDeg: Double
        let nearestNodeId: String
    }

    enum MarkerDetectionRole {
        case entrance
        case checkpoint
    }

    /// Registered markers by reference image name
    private var knownMarkers: [String: KnownMarker] = [:]

    /// Expected entrance marker reference image name (for strict matching)
    private(set) var expectedMarkerName: String?

    /// Callback when entrance marker is detected with alignment data
    var onMarkerDetected: ((MarkerDetectionEvent) -> Void)?

    /// Callback when checkpoint marker is detected (does NOT restart session)
    var onCheckpointDetected: ((MarkerDetectionEvent) -> Void)?

    /// Whether an entrance marker has already been detected (to avoid re-triggering)
    private(set) var hasDetectedMarker: Bool = false

    // MARK: - Detection Diagnostics

    /// Total ARImageAnchor candidates received from ARKit
    private(set) var totalCandidatesSeen: Int = 0
    /// Candidates rejected because their name didn't match a known marker
    private(set) var rejectedCandidates: Int = 0
    /// Names of rejected candidate images (for diagnostics)
    private(set) var rejectedNames: [String] = []

    /// Categorized detection failure reason.
    enum DetectionFailureReason {
        /// AR session running but ARKit never delivered any image anchor
        case noCandidatesSeen
        /// ARKit delivered image anchors but none matched the expected marker
        case candidatesRejected(seen: Int, names: [String])
        /// The expected marker asset was never loaded into the session
        case assetMissing
    }

    /// Returns the failure reason based on current diagnostics.
    var detectionFailureReason: DetectionFailureReason {
        if totalCandidatesSeen == 0 {
            return .noCandidatesSeen
        } else {
            return .candidatesRejected(seen: totalCandidatesSeen, names: rejectedNames)
        }
    }

    /// Configure the detector with entrance marker data.
    func configure(
        markerId: String,
        markerName: String?,
        buildingX: Double,
        buildingY: Double,
        buildingZ: Double,
        buildingRotationYDeg: Double,
        nearestNodeId: String
    ) {
        self.expectedMarkerName = markerName
        self.hasDetectedMarker = false
        knownMarkers.removeAll()

        // Register as known marker
        if let name = markerName {
            knownMarkers[name] = KnownMarker(
                markerId: markerId,
                role: .entrance,
                buildingX: buildingX,
                buildingY: buildingY,
                buildingZ: buildingZ,
                buildingRotationYDeg: buildingRotationYDeg,
                nearestNodeId: nearestNodeId
            )
        }
    }

    /// Process an ARImageAnchor when added to the session.
    /// On real devices, only accepts images that match a registered known marker.
    func processAnchor(_ anchor: ARAnchor) {
        guard let imageAnchor = anchor as? ARImageAnchor else { return }
        guard !hasDetectedMarker else { return }

        totalCandidatesSeen += 1

        // Extract pose from anchor transform
        let transform = imageAnchor.transform
        let arX = Double(transform.columns.3.x)
        let arY = Double(transform.columns.3.y)
        let arZ = Double(transform.columns.3.z)

        // Extract Y-rotation from the transform matrix
        let arRotationYRad = atan2(Double(transform.columns.0.z), Double(transform.columns.0.x))
        let arRotationYDeg = arRotationYRad * 180.0 / .pi

        let detectedName = imageAnchor.referenceImage.name ?? "<unnamed>"
        let confidence = Double(imageAnchor.isTracked ? 1.0 : 0.5)
        let imgSize = imageAnchor.referenceImage.physicalSize

        print("[MarkerDetector] Image anchor #\(totalCandidatesSeen): '\(detectedName)' tracked=\(imageAnchor.isTracked) physSize=\(imgSize.width)×\(imgSize.height)m pos=(\(String(format: "%.2f", arX)), \(String(format: "%.2f", arY)), \(String(format: "%.2f", arZ)))")

        // Strict matching: only accept known registered markers
        guard let known = knownMarkers[detectedName] else {
            rejectedCandidates += 1
            if !rejectedNames.contains(detectedName) {
                rejectedNames.append(detectedName)
            }
            print("[MarkerDetector] REJECTED: '\(detectedName)' not in knownMarkers (expected: '\(expectedMarkerName ?? "none")', registered: \(Array(knownMarkers.keys)))")
            return
        }

        let event = MarkerDetectionEvent(
            markerId: known.markerId,
            entranceNodeId: known.nearestNodeId,
            markerBuildingX: known.buildingX,
            markerBuildingY: known.buildingY,
            markerBuildingZ: known.buildingZ,
            markerArX: arX,
            markerArY: arY,
            markerArZ: arZ,
            markerArRotationYDeg: arRotationYDeg,
            markerBuildingRotationYDeg: known.buildingRotationYDeg,
            confidence: confidence,
            role: known.role
        )

        switch known.role {
        case .entrance:
            hasDetectedMarker = true
            print("[MarkerDetector] Entrance marker aligned — AR pos: (\(arX), \(arY), \(arZ))")
            onMarkerDetected?(event)

        case .checkpoint:
            print("[MarkerDetector] Checkpoint marker observed: \(known.markerId)")
            onCheckpointDetected?(event)
        }
    }

    /// Reset to allow re-detection.
    func reset() {
        hasDetectedMarker = false
        totalCandidatesSeen = 0
        rejectedCandidates = 0
        rejectedNames = []
    }
}

/// Event data from marker detection.
struct MarkerDetectionEvent {
    let markerId: String
    let entranceNodeId: String
    let markerBuildingX: Double
    let markerBuildingY: Double
    let markerBuildingZ: Double
    let markerArX: Double
    let markerArY: Double
    let markerArZ: Double
    let markerArRotationYDeg: Double
    let markerBuildingRotationYDeg: Double
    let confidence: Double
    let role: ARMarkerDetector.MarkerDetectionRole
}
