import Foundation
import ARKit
import RealityKit
import Combine

/// Manages the ARKit session lifecycle and configuration.
///
/// Responsibilities:
/// - Configure world tracking with image detection
/// - Load reference images for entrance markers
/// - Manage session lifecycle (start, pause, resume, stop)
/// - Report tracking quality changes
class ARSessionManager: NSObject, ObservableObject {

    private(set) var arView: ARView?
    private var imageConfiguration: ARWorldTrackingConfiguration?
    
    /// Current tracking state description
    @Published var trackingStateDescription: String = "Not started"
    @Published var isSessionRunning: Bool = false
    
    var onTrackingStateChanged: ((Bool, String) -> Void)?
    
    override init() {
        super.init()
    }
    
    /// Configure and start the AR session with marker detection.
    /// - Parameters:
    ///   - arView: The RealityKit ARView to manage
    ///   - markerImageName: Name of the reference image in AR Resources
    ///   - markerPhysicalWidth: Physical width of the marker in meters
    func startSession(arView: ARView, markerImageName: String, markerPhysicalWidth: Double) {
        self.arView = arView
        
        let config = ARWorldTrackingConfiguration()
        config.planeDetection = [.horizontal]
        config.environmentTexturing = .automatic
        
        // Load reference images for marker detection
        if let referenceImages = ARReferenceImage.referenceImages(
            inGroupNamed: "AR Resources", bundle: .main
        ) {
            config.detectionImages = referenceImages
            config.maximumNumberOfTrackedImages = 1
            print("[ARSession] Loaded \(referenceImages.count) reference images")
        } else {
            // Create reference image programmatically if assets not bundled
            print("[ARSession] No bundled AR Resources found, marker detection disabled")
        }
        
        self.imageConfiguration = config
        
        arView.session.delegate = self
        arView.session.run(config, options: [.resetTracking, .removeExistingAnchors])
        
        isSessionRunning = true
        trackingStateDescription = "Initializing..."
        print("[ARSession] Session started")
    }
    
    /// Pause the AR session.
    func pauseSession() {
        arView?.session.pause()
        isSessionRunning = false
        trackingStateDescription = "Paused"
    }
    
    /// Resume the AR session.
    func resumeSession() {
        guard let config = imageConfiguration else { return }
        arView?.session.run(config)
        isSessionRunning = true
    }
    
    /// Stop and clean up the AR session.
    func stopSession() {
        arView?.session.pause()
        arView = nil
        imageConfiguration = nil
        isSessionRunning = false
        trackingStateDescription = "Stopped"
        print("[ARSession] Session stopped")
    }
}

// MARK: - ARSessionDelegate

extension ARSessionManager: ARSessionDelegate {
    
    func session(_ session: ARSession, cameraDidChangeTrackingState camera: ARCamera) {
        switch camera.trackingState {
        case .notAvailable:
            trackingStateDescription = "Tracking not available"
            onTrackingStateChanged?(true, "Not available")
        case .limited(let reason):
            let reasonStr: String
            switch reason {
            case .initializing:
                reasonStr = "Initializing"
            case .excessiveMotion:
                reasonStr = "Move slower"
            case .insufficientFeatures:
                reasonStr = "More visual detail needed"
            case .relocalizing:
                reasonStr = "Relocalizing"
            @unknown default:
                reasonStr = "Unknown"
            }
            trackingStateDescription = "Limited: \(reasonStr)"
            onTrackingStateChanged?(true, reasonStr)
        case .normal:
            trackingStateDescription = "Normal"
            onTrackingStateChanged?(false, "")
        }
    }
    
    func session(_ session: ARSession, didFailWithError error: Error) {
        trackingStateDescription = "Error: \(error.localizedDescription)"
        print("[ARSession] Failed: \(error)")
    }
    
    func sessionWasInterrupted(_ session: ARSession) {
        trackingStateDescription = "Interrupted"
        print("[ARSession] Interrupted")
    }
    
    func sessionInterruptionEnded(_ session: ARSession) {
        trackingStateDescription = "Resuming..."
        print("[ARSession] Interruption ended")
    }
}
