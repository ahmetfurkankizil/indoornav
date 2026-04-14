import Foundation
import ARKit
import RealityKit
import Combine

/// Manages the ARKit session lifecycle and configuration.
///
/// Phase 5: On real devices, the AR Resources asset catalog must contain the
/// expected reference image. If missing, the session reports an error via
/// `onMarkerAssetMissing` — no silent fallback. Simulator skips this check.
class ARSessionManager: NSObject, ObservableObject {

    private(set) var arView: ARView?
    private var imageConfiguration: ARWorldTrackingConfiguration?

    /// Current tracking state description
    @Published var trackingStateDescription: String = "Not started"
    @Published var isSessionRunning: Bool = false

    var onTrackingStateChanged: ((Bool, String) -> Void)?

    /// Called if the required marker reference image is missing from the bundle.
    var onMarkerAssetMissing: ((String) -> Void)?

    /// Whether the AR Resources group was found and loaded.
    private(set) var hasLoadedReferenceImages: Bool = false

    override init() {
        super.init()
    }

    /// Names of reference images actually loaded from the asset catalog.
    private(set) var loadedImageNames: [String] = []

    /// The marker image name this session expects.
    private(set) var expectedMarkerName: String?

    /// Configure and start the AR session with marker detection.
    func startSession(arView: ARView, markerImageName: String, markerPhysicalWidth: Double) {
        self.arView = arView
        self.expectedMarkerName = markerImageName
        self.loadedImageNames = []

        print("[ARSession] --- Session Start ---")
        print("[ARSession] Expected marker: '\(markerImageName)' (physicalWidth: \(markerPhysicalWidth)m)")
        print("[ARSession] ARWorldTrackingConfiguration.isSupported: \(ARWorldTrackingConfiguration.isSupported)")

        let config = ARWorldTrackingConfiguration()
        config.planeDetection = [.horizontal]
        config.environmentTexturing = .automatic

        // Load reference images for marker detection
        if let referenceImages = ARReferenceImage.referenceImages(
            inGroupNamed: "AR Resources", bundle: .main
        ) {
            let names = referenceImages.compactMap { $0.name }
            self.loadedImageNames = names
            let hasExpectedImage = names.contains(markerImageName)

            print("[ARSession] AR Resources loaded: \(referenceImages.count) image(s)")
            print("[ARSession] Loaded image names: \(names)")
            print("[ARSession] Expected '\(markerImageName)' present: \(hasExpectedImage)")

            for img in referenceImages {
                let w = img.physicalSize.width
                let h = img.physicalSize.height
                print("[ARSession]   image '\(img.name ?? "<unnamed>")' physicalSize: \(w)m × \(h)m")
            }

            #if !targetEnvironment(simulator)
            if !hasExpectedImage {
                print("[ARSession] ERROR: Reference image '\(markerImageName)' not found in AR Resources")
                print("[ARSession] Available images: \(names). Check that entrance_markers.json referenceImageName matches the asset catalog entry.")
                onMarkerAssetMissing?(markerImageName)
                return
            }
            #endif

            // physicalSize is read-only on ARReferenceImage loaded from asset catalogs.
            // If the asset catalog didn't preserve the physical width, rebuild the
            // reference image set from scratch using the package value.
            var finalImages = referenceImages
            let expectedImg = referenceImages.first { $0.name == markerImageName }
            if let eImg = expectedImg, eImg.physicalSize.width <= 0 || eImg.physicalSize.height <= 0 {
                print("[ARSession]   physicalSize is zero for '\(markerImageName)' — rebuilding from UIImage")
                // ARReferenceImage does NOT have a .cgImage property, so we
                // load the same image from a regular Image Set in Assets.xcassets.
                if let uiImage = UIImage(named: "entrance_marker_display"),
                   let cg = uiImage.cgImage {
                    let corrected = ARReferenceImage(cg, orientation: .up,
                                                     physicalWidth: CGFloat(markerPhysicalWidth))
                    corrected.name = markerImageName
                    finalImages = finalImages.filter { $0.name != markerImageName }
                    finalImages.insert(corrected)
                    print("[ARSession]   Rebuilt '\(markerImageName)' with physicalWidth: \(markerPhysicalWidth)m ✓")
                } else {
                    print("[ARSession]   ERROR: Could not load UIImage for '\(markerImageName)'. Add it as a regular Image Set in Assets.xcassets.")
                }
            }

            config.detectionImages = finalImages
            config.maximumNumberOfTrackedImages = 1
            hasLoadedReferenceImages = true
        } else {
            #if targetEnvironment(simulator)
            // Simulator: no AR Resources is expected, proceed without image detection
            print("[ARSession] Simulator — no AR Resources, marker detection via simulate button only")
            hasLoadedReferenceImages = false
            #else
            // Real device: AR Resources must exist
            print("[ARSession] ERROR: AR Resources asset catalog not found in bundle")
            print("[ARSession] Ensure Assets.xcassets contains 'AR Resources.arresourcegroup' with the entrance marker image.")
            onMarkerAssetMissing?(markerImageName)
            return
            #endif
        }

        self.imageConfiguration = config

        arView.session.delegate = self
        arView.session.run(config, options: [.resetTracking, .removeExistingAnchors])

        isSessionRunning = true
        trackingStateDescription = "Initializing..."
        print("[ARSession] Session running. Waiting for ARKit to detect '\(markerImageName)' in camera feed...")
        print("[ARSession] Tip: The PRINTED poster must be the EXACT image in the asset catalog at the correct physical size (\(markerPhysicalWidth)m).")
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
        hasLoadedReferenceImages = false
        trackingStateDescription = "Stopped"
        print("[ARSession] Session stopped")
    }

    /// Check if a specific reference image exists in the bundled AR Resources.
    static func hasReferenceImage(named name: String) -> Bool {
        guard let images = ARReferenceImage.referenceImages(
            inGroupNamed: "AR Resources", bundle: .main
        ) else { return false }
        return images.contains { $0.name == name }
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
        print("[ARSession] FAILED: \(error)")
        if let arError = error as? ARError {
            print("[ARSession] ARError code: \(arError.code.rawValue)")
        }
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
