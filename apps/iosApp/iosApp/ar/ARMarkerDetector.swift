import Foundation
import ARKit

/// Detects entrance marker reference images and reports alignment events.
///
/// Works as an ARSession delegate extension, watching for ARImageAnchor
/// additions and reporting the first stable detection.
class ARMarkerDetector: NSObject {
    
    /// Expected marker reference image name (from entrance_markers.json)
    var expectedMarkerName: String?
    
    /// Marker metadata from the building package
    var markerBuildingX: Double = 0.0
    var markerBuildingY: Double = 0.0
    var markerBuildingZ: Double = 0.0
    var markerBuildingRotationYDeg: Double = 0.0
    var markerNearestNodeId: String = ""
    var markerId: String = ""
    
    /// Callback when marker is detected with alignment data
    var onMarkerDetected: ((MarkerDetectionEvent) -> Void)?
    
    /// Whether a marker has already been detected (to avoid re-triggering)
    private(set) var hasDetectedMarker: Bool = false
    
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
        self.markerId = markerId
        self.expectedMarkerName = markerName
        self.markerBuildingX = buildingX
        self.markerBuildingY = buildingY
        self.markerBuildingZ = buildingZ
        self.markerBuildingRotationYDeg = buildingRotationYDeg
        self.markerNearestNodeId = nearestNodeId
        self.hasDetectedMarker = false
    }
    
    /// Process an ARImageAnchor when added to the session.
    /// Call this from ARSessionDelegate.session(_:didAdd:)
    func processAnchor(_ anchor: ARAnchor) {
        guard !hasDetectedMarker,
              let imageAnchor = anchor as? ARImageAnchor else { return }
        
        // Check if this is our expected marker
        let detectedName = imageAnchor.referenceImage.name ?? ""
        print("[MarkerDetector] Detected image: '\(detectedName)'")
        
        // Accept any detected image for now (single-building MVP)
        // Future: match against expectedMarkerName
        
        // Extract pose from anchor transform
        let transform = imageAnchor.transform
        let arX = Double(transform.columns.3.x)
        let arY = Double(transform.columns.3.y)
        let arZ = Double(transform.columns.3.z)
        
        // Extract Y-rotation from the transform matrix
        // For Y-up, rotation around Y: atan2(m[0][2], m[0][0])
        let arRotationYRad = atan2(Double(transform.columns.0.z), Double(transform.columns.0.x))
        let arRotationYDeg = arRotationYRad * 180.0 / .pi
        
        hasDetectedMarker = true
        
        let event = MarkerDetectionEvent(
            markerId: markerId,
            entranceNodeId: markerNearestNodeId,
            markerBuildingX: markerBuildingX,
            markerBuildingY: markerBuildingY,
            markerBuildingZ: markerBuildingZ,
            markerArX: arX,
            markerArY: arY,
            markerArZ: arZ,
            markerArRotationYDeg: arRotationYDeg,
            markerBuildingRotationYDeg: markerBuildingRotationYDeg,
            confidence: Double(imageAnchor.isTracked ? 1.0 : 0.5)
        )
        
        print("[MarkerDetector] Marker aligned — AR pos: (\(arX), \(arY), \(arZ))")
        onMarkerDetected?(event)
    }
    
    /// Reset to allow re-detection.
    func reset() {
        hasDetectedMarker = false
    }
}

/// Event data from marker detection (mirrors MarkerAlignmentResult in shared code).
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
}
