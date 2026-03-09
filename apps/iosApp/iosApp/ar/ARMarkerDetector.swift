import Foundation
import ARKit

/// Detects entrance and checkpoint marker reference images and reports alignment events.
///
/// Works as an ARSession delegate extension, watching for ARImageAnchor
/// additions. Entrance markers trigger session alignment; checkpoint markers
/// emit correction events without restarting the session.
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
    
    /// Fallback entrance marker metadata (for single-marker backward compat)
    var expectedMarkerName: String?
    var markerBuildingX: Double = 0.0
    var markerBuildingY: Double = 0.0
    var markerBuildingZ: Double = 0.0
    var markerBuildingRotationYDeg: Double = 0.0
    var markerNearestNodeId: String = ""
    var markerId: String = ""
    
    /// Callback when entrance marker is detected with alignment data
    var onMarkerDetected: ((MarkerDetectionEvent) -> Void)?
    
    /// Callback when checkpoint marker is detected (does NOT restart session)
    var onCheckpointDetected: ((MarkerDetectionEvent) -> Void)?
    
    /// Whether an entrance marker has already been detected (to avoid re-triggering)
    private(set) var hasDetectedMarker: Bool = false
    
    /// Configure the detector with entrance marker data (backward-compatible single-marker).
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
    
    /// Configure additional checkpoint markers.
    func configureCheckpoints(_ checkpoints: [(id: String, refImageName: String, buildingX: Double, buildingY: Double, buildingZ: Double, rotationYDeg: Double, nearestNodeId: String)]) {
        for cp in checkpoints {
            knownMarkers[cp.refImageName] = KnownMarker(
                markerId: cp.id,
                role: .checkpoint,
                buildingX: cp.buildingX,
                buildingY: cp.buildingY,
                buildingZ: cp.buildingZ,
                buildingRotationYDeg: cp.rotationYDeg,
                nearestNodeId: cp.nearestNodeId
            )
        }
    }
    
    /// Process an ARImageAnchor when added to the session.
    /// Call this from ARSessionDelegate.session(_:didAdd:)
    func processAnchor(_ anchor: ARAnchor) {
        guard let imageAnchor = anchor as? ARImageAnchor else { return }
        
        // Extract pose from anchor transform
        let transform = imageAnchor.transform
        let arX = Double(transform.columns.3.x)
        let arY = Double(transform.columns.3.y)
        let arZ = Double(transform.columns.3.z)
        
        // Extract Y-rotation from the transform matrix
        let arRotationYRad = atan2(Double(transform.columns.0.z), Double(transform.columns.0.x))
        let arRotationYDeg = arRotationYRad * 180.0 / .pi
        
        let detectedName = imageAnchor.referenceImage.name ?? ""
        let confidence = Double(imageAnchor.isTracked ? 1.0 : 0.5)
        
        print("[MarkerDetector] Detected image: '\(detectedName)'")
        
        // Try to match against known markers
        if let known = knownMarkers[detectedName] {
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
                guard !hasDetectedMarker else { return }
                hasDetectedMarker = true
                print("[MarkerDetector] Entrance marker aligned — AR pos: (\(arX), \(arY), \(arZ))")
                onMarkerDetected?(event)
                
            case .checkpoint:
                print("[MarkerDetector] Checkpoint marker observed: \(known.markerId) — AR pos: (\(arX), \(arY), \(arZ))")
                onCheckpointDetected?(event)
            }
            return
        }
        
        // Fallback: accept any detected image for entrance (single-building MVP backward compat)
        guard !hasDetectedMarker else { return }
        
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
            confidence: confidence,
            role: .entrance
        )
        
        print("[MarkerDetector] Marker aligned — AR pos: (\(arX), \(arY), \(arZ))")
        onMarkerDetected?(event)
    }
    
    /// Reset to allow re-detection.
    func reset() {
        hasDetectedMarker = false
    }
    
    /// Full reset including all marker registrations.
    func fullReset() {
        hasDetectedMarker = false
        knownMarkers.removeAll()
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
    let role: ARMarkerDetector.MarkerDetectionRole
}
