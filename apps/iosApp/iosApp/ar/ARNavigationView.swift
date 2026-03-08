import SwiftUI
import ARKit
import RealityKit

/// Native ARKit/RealityKit navigation view for iOS.
///
/// Hosts the camera-based AR experience:
/// - Detects the entrance marker to establish world alignment
/// - Renders navigation arrows in the AR scene
/// - Shows debug overlay with session state
struct ARNavigationView: View {
    @Binding var isPresented: Bool
    @StateObject private var viewModel = ARNavigationViewModel()
    
    // TODO: Receive these from shared coordinator via DI
    var buildingId: String = "demo-office-01"
    var destinationName: String = ""
    
    var body: some View {
        ZStack {
            // Real AR view
            ARViewContainer(viewModel: viewModel)
                .ignoresSafeArea()
            
            // Debug overlay
            VStack {
                // Top bar: close + state
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(viewModel.sessionStateLabel)
                            .font(.caption)
                            .fontWeight(.semibold)
                            .foregroundStyle(.white)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(
                                Capsule().fill(viewModel.stateColor.opacity(0.8))
                            )
                        
                        if !viewModel.trackingState.isEmpty {
                            Text("Tracking: \(viewModel.trackingState)")
                                .font(.caption2)
                                .foregroundStyle(.white.opacity(0.7))
                        }
                    }
                    
                    Spacer()
                    
                    Button(action: {
                        viewModel.stopSession()
                        isPresented = false
                    }) {
                        Image(systemName: "xmark.circle.fill")
                            .font(.title)
                            .foregroundStyle(.white.opacity(0.8))
                    }
                }
                .padding()
                
                Spacer()
                
                // Bottom: instruction + debug info
                VStack(spacing: 12) {
                    // Current instruction
                    if !viewModel.currentInstruction.isEmpty {
                        Text(viewModel.currentInstruction)
                            .font(.title3)
                            .fontWeight(.semibold)
                            .foregroundStyle(.white)
                            .padding(.horizontal, 20)
                            .padding(.vertical, 12)
                            .background(.ultraThinMaterial)
                            .clipShape(RoundedRectangle(cornerRadius: 16))
                    }
                    
                    // Destination
                    if !viewModel.destinationLabel.isEmpty {
                        Text("→ \(viewModel.destinationLabel)")
                            .font(.callout)
                            .foregroundStyle(.white.opacity(0.8))
                    }
                    
                    // Debug panel
                    if viewModel.showDebugPanel {
                        debugPanel
                    }
                    
                    // Action buttons
                    HStack(spacing: 16) {
                        // Toggle debug
                        Button(action: { viewModel.showDebugPanel.toggle() }) {
                            Image(systemName: "ladybug")
                                .font(.title3)
                                .foregroundStyle(.white)
                                .padding(12)
                                .background(.ultraThinMaterial)
                                .clipShape(Circle())
                        }
                        
                        // Simulate alignment (debug)
                        if !viewModel.isAligned {
                            Button(action: { viewModel.simulateAlignment() }) {
                                Label("Simulate Scan", systemImage: "qrcode.viewfinder")
                                    .font(.callout)
                                    .fontWeight(.medium)
                                    .foregroundStyle(.white)
                                    .padding(.horizontal, 16)
                                    .padding(.vertical, 10)
                                    .background(.blue.opacity(0.8))
                                    .clipShape(Capsule())
                            }
                        }
                    }
                }
                .padding(.bottom, 40)
            }
        }
        .onAppear {
            viewModel.startSession()
        }
    }
    
    private var debugPanel: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("Debug Info")
                .font(.caption)
                .fontWeight(.bold)
            Text("State: \(viewModel.sessionStateLabel)")
                .font(.caption2)
            Text("Marker: \(viewModel.isAligned ? "✓ Detected" : "✗ Waiting")")
                .font(.caption2)
            Text("Arrows: \(viewModel.arrowCount)")
                .font(.caption2)
            Text("Destination: \(viewModel.destinationLabel)")
                .font(.caption2)
            Text("Tracking: \(viewModel.trackingState)")
                .font(.caption2)
        }
        .foregroundStyle(.white)
        .padding(12)
        .background(.black.opacity(0.6))
        .clipShape(RoundedRectangle(cornerRadius: 10))
        .padding(.horizontal)
    }
}

// MARK: - ARView Container

/// UIViewRepresentable wrapping RealityKit ARView.
struct ARViewContainer: UIViewRepresentable {
    @ObservedObject var viewModel: ARNavigationViewModel
    
    func makeUIView(context: Context) -> ARView {
        let arView = ARView(frame: .zero)
        arView.automaticallyConfigureSession = false
        
        // Store reference for session management
        viewModel.setupARView(arView)
        
        return arView
    }
    
    func updateUIView(_ uiView: ARView, context: Context) {
        // Updates handled by view model
    }
}

// MARK: - View Model

/// Orchestrates the iOS AR navigation experience.
class ARNavigationViewModel: ObservableObject {
    
    @Published var sessionStateLabel: String = "Idle"
    @Published var trackingState: String = ""
    @Published var currentInstruction: String = "Point camera at entrance marker"
    @Published var destinationLabel: String = ""
    @Published var arrowCount: Int = 0
    @Published var isAligned: Bool = false
    @Published var showDebugPanel: Bool = false
    @Published var stateColor: Color = .gray
    
    private let sessionManager = ARSessionManager()
    private let markerDetector = ARMarkerDetector()
    private let routeRenderer = ARRouteRenderer()
    private weak var arView: ARView?
    
    // TODO: Wire these from shared ArNavigationCoordinator via KMP interop
    private var mockArrows: [ArrowPlacementData] = []
    
    func setupARView(_ arView: ARView) {
        self.arView = arView
        
        // Wire marker detector to session delegate
        sessionManager.onTrackingStateChanged = { [weak self] isLimited, reason in
            DispatchQueue.main.async {
                self?.trackingState = isLimited ? reason : "Normal"
            }
        }
        
        // Configure marker detector
        markerDetector.configure(
            markerId: "marker-main",
            markerName: "entrance_marker_main",
            buildingX: 0.0, buildingY: 1.2, buildingZ: 0.0,
            buildingRotationYDeg: 0.0,
            nearestNodeId: "n01"
        )
        
        // Set up marker detection callback
        markerDetector.onMarkerDetected = { [weak self] event in
            DispatchQueue.main.async {
                self?.handleMarkerDetected(event)
            }
        }
        
        // Hook into session delegate for anchor detection
        // We'll check for image anchors in the session updates
        NotificationCenter.default.addObserver(
            forName: .init("ARSessionDidAddAnchors"),
            object: nil,
            queue: .main
        ) { [weak self] _ in }
    }
    
    func startSession() {
        guard let arView = arView else { return }
        
        sessionStateLabel = "Waiting for Marker"
        stateColor = .orange
        
        sessionManager.startSession(
            arView: arView,
            markerImageName: "entrance_marker_main",
            markerPhysicalWidth: 0.21
        )
        
        // Add session delegate for anchor detection
        arView.session.delegate = AnchorDetectionDelegate(
            markerDetector: markerDetector,
            sessionManager: sessionManager
        )
    }
    
    func stopSession() {
        sessionManager.stopSession()
        if let arView = arView {
            routeRenderer.clearRoute(from: arView)
        }
        sessionStateLabel = "Stopped"
        stateColor = .gray
    }
    
    func simulateAlignment() {
        // Simulate marker detection at building origin with identity transform
        let event = MarkerDetectionEvent(
            markerId: "marker-main",
            entranceNodeId: "n01",
            markerBuildingX: 0.0,
            markerBuildingY: 1.2,
            markerBuildingZ: 0.0,
            markerArX: 0.0,
            markerArY: 0.0,
            markerArZ: -1.0, // 1m in front of camera
            markerArRotationYDeg: 0.0,
            markerBuildingRotationYDeg: 0.0,
            confidence: 1.0
        )
        handleMarkerDetected(event)
    }
    
    private func handleMarkerDetected(_ event: MarkerDetectionEvent) {
        isAligned = true
        sessionStateLabel = "Aligned"
        stateColor = .green
        currentInstruction = "Route loaded — follow the arrows"
        
        // Compute alignment transform
        let rotDeg = event.markerArRotationYDeg - event.markerBuildingRotationYDeg
        let cosR = cos(rotDeg * .pi / 180.0)
        let sinR = sin(rotDeg * .pi / 180.0)
        let rotBldgX = event.markerBuildingX * cosR + event.markerBuildingZ * sinR
        let rotBldgZ = -event.markerBuildingX * sinR + event.markerBuildingZ * cosR
        
        routeRenderer.setAlignmentTransform(
            offsetX: event.markerArX - rotBldgX,
            offsetY: event.markerArY - event.markerBuildingY,
            offsetZ: event.markerArZ - rotBldgZ,
            rotationYDeg: rotDeg
        )
        
        // TODO: Get arrow data from shared ArNavigationCoordinator
        // For now, generate demo arrows for the demo-office route
        let demoArrows = generateDemoArrows()
        
        if let arView = arView {
            routeRenderer.renderRoute(in: arView, arrows: demoArrows)
            arrowCount = routeRenderer.renderedArrowCount
            sessionStateLabel = "Rendering Route"
            stateColor = .blue
        }
    }
    
    /// Generate demo arrows for testing (represents n01 → n02 → n03 → n04 → n07)
    private func generateDemoArrows() -> [ArrowPlacementData] {
        return [
            ArrowPlacementData(id: "a0", positionX: 0.0, positionY: 0.05, positionZ: 0.0,
                             forwardDx: 1.0, forwardDy: 0.0, forwardDz: 0.0, type: .follow, label: nil),
            ArrowPlacementData(id: "a1", positionX: 1.5, positionY: 0.05, positionZ: 0.0,
                             forwardDx: 1.0, forwardDy: 0.0, forwardDz: 0.0, type: .follow, label: nil),
            ArrowPlacementData(id: "a2", positionX: 3.0, positionY: 0.05, positionZ: 0.0,
                             forwardDx: 1.0, forwardDy: 0.0, forwardDz: 0.0, type: .follow, label: nil),
            ArrowPlacementData(id: "a3", positionX: 4.5, positionY: 0.05, positionZ: 0.0,
                             forwardDx: 1.0, forwardDy: 0.0, forwardDz: 0.0, type: .follow, label: nil),
            ArrowPlacementData(id: "a4", positionX: 6.0, positionY: 0.05, positionZ: 0.0,
                             forwardDx: 0.0, forwardDy: 0.0, forwardDz: 1.0, type: .turnRight, label: "Turn right"),
            ArrowPlacementData(id: "a5", positionX: 6.0, positionY: 0.05, positionZ: 1.5,
                             forwardDx: 0.0, forwardDy: 0.0, forwardDz: 1.0, type: .follow, label: nil),
            ArrowPlacementData(id: "a6", positionX: 6.0, positionY: 0.05, positionZ: 3.0,
                             forwardDx: 0.0, forwardDy: 0.0, forwardDz: 1.0, type: .follow, label: nil),
            ArrowPlacementData(id: "a7", positionX: 6.0, positionY: 0.05, positionZ: 4.0,
                             forwardDx: -1.0, forwardDy: 0.0, forwardDz: 0.0, type: .turnLeft, label: "Turn left"),
            ArrowPlacementData(id: "a8", positionX: 4.5, positionY: 0.05, positionZ: 4.0,
                             forwardDx: -1.0, forwardDy: 0.0, forwardDz: 0.0, type: .follow, label: nil),
            ArrowPlacementData(id: "a9", positionX: 3.0, positionY: 0.05, positionZ: 4.0,
                             forwardDx: -1.0, forwardDy: 0.0, forwardDz: 0.0, type: .destination, label: "Conference Room"),
        ]
    }
}

// MARK: - Session Delegate Bridge

/// Bridges ARSession delegate to both session manager and marker detector.
class AnchorDetectionDelegate: NSObject, ARSessionDelegate {
    let markerDetector: ARMarkerDetector
    let sessionManager: ARSessionManager
    
    init(markerDetector: ARMarkerDetector, sessionManager: ARSessionManager) {
        self.markerDetector = markerDetector
        self.sessionManager = sessionManager
    }
    
    func session(_ session: ARSession, didAdd anchors: [ARAnchor]) {
        for anchor in anchors {
            markerDetector.processAnchor(anchor)
        }
    }
    
    func session(_ session: ARSession, cameraDidChangeTrackingState camera: ARCamera) {
        sessionManager.session(session, cameraDidChangeTrackingState: camera)
    }
    
    func session(_ session: ARSession, didFailWithError error: Error) {
        sessionManager.session(session, didFailWithError: error)
    }
    
    func sessionWasInterrupted(_ session: ARSession) {
        sessionManager.sessionWasInterrupted(session)
    }
    
    func sessionInterruptionEnded(_ session: ARSession) {
        sessionManager.sessionInterruptionEnded(session)
    }
}
