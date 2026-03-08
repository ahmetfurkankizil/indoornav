import SwiftUI
import ARKit
import RealityKit

/// Native ARKit/RealityKit navigation view for iOS.
///
/// Full navigation experience:
/// - Marker detection → alignment → route rendering
/// - Active navigation overlay (instruction, progress, tracking)
/// - Arrival detection → session summary
/// - Debug controls (simulate scan, advance progress, simulate arrival)
struct ARNavigationView: View {
    @Binding var isPresented: Bool
    @StateObject private var viewModel = ARNavigationViewModel()
    
    var buildingId: String = "demo-office-01"
    var destinationName: String = ""
    
    var body: some View {
        ZStack {
            // Real AR camera view
            ARViewContainer(viewModel: viewModel)
                .ignoresSafeArea()
            
            // State-dependent overlay
            VStack(spacing: 0) {
                topBar
                Spacer()
                
                if viewModel.hasArrived {
                    arrivalOverlay
                } else {
                    activeNavigationOverlay
                }
            }
        }
        .onAppear {
            viewModel.startSession()
        }
    }
    
    // MARK: - Top Bar
    
    private var topBar: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 6) {
                    Circle()
                        .fill(viewModel.stateColor)
                        .frame(width: 8, height: 8)
                    Text(viewModel.sessionStateLabel)
                        .font(.caption)
                        .fontWeight(.semibold)
                        .foregroundStyle(.white)
                }
                
                if viewModel.isSimulated {
                    Text("DEMO MODE")
                        .font(.system(size: 10, weight: .bold))
                        .foregroundStyle(.orange)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(Color.orange.opacity(0.2))
                        .clipShape(RoundedRectangle(cornerRadius: 4))
                }
                
                if !viewModel.trackingState.isEmpty {
                    Text("Tracking: \(viewModel.trackingState)")
                        .font(.caption2)
                        .foregroundStyle(.white.opacity(0.6))
                }
            }
            
            Spacer()
            
            Button(action: {
                viewModel.endNavigation()
                isPresented = false
            }) {
                Image(systemName: "xmark.circle.fill")
                    .font(.title2)
                    .foregroundStyle(.white.opacity(0.8))
            }
        }
        .padding(.horizontal, 16)
        .padding(.top, 12)
    }
    
    // MARK: - Active Navigation Overlay
    
    private var activeNavigationOverlay: some View {
        VStack(spacing: 16) {
            // Progress bar
            if viewModel.isAligned {
                ProgressView(value: viewModel.progress, total: 1.0)
                    .tint(viewModel.progress > 0.8 ? .green : .blue)
                    .padding(.horizontal, 32)
            }
            
            // Current instruction
            if !viewModel.currentInstruction.isEmpty {
                Text(viewModel.currentInstruction)
                    .font(.title3)
                    .fontWeight(.semibold)
                    .foregroundStyle(.white)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 14)
                    .background(.ultraThinMaterial)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
            }
            
            // Destination + distance
            HStack {
                Image(systemName: "location.fill")
                    .foregroundStyle(.blue)
                Text(viewModel.destinationLabel)
                    .font(.callout)
                    .foregroundStyle(.white.opacity(0.9))
                
                if viewModel.isAligned {
                    Spacer()
                    Text("\(viewModel.arrowCount) arrows")
                        .font(.caption)
                        .foregroundStyle(.white.opacity(0.5))
                }
            }
            .padding(.horizontal, 24)
            
            // Action buttons
            HStack(spacing: 12) {
                // Debug toggle
                Button(action: { viewModel.showDebugPanel.toggle() }) {
                    Image(systemName: "ladybug")
                        .font(.callout)
                        .foregroundStyle(.white)
                        .padding(10)
                        .background(.ultraThinMaterial)
                        .clipShape(Circle())
                }
                
                if !viewModel.isAligned {
                    // Simulate scan
                    Button(action: { viewModel.simulateAlignment() }) {
                        Label("Simulate Scan", systemImage: "qrcode.viewfinder")
                            .font(.callout)
                            .fontWeight(.medium)
                            .foregroundStyle(.white)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 10)
                            .background(.blue.opacity(0.85))
                            .clipShape(Capsule())
                    }
                } else {
                    // Advance progress (demo)
                    Button(action: { viewModel.advanceProgress() }) {
                        Label("Advance", systemImage: "forward.fill")
                            .font(.callout)
                            .fontWeight(.medium)
                            .foregroundStyle(.white)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 10)
                            .background(.indigo.opacity(0.8))
                            .clipShape(Capsule())
                    }
                    
                    // End navigation
                    Button(action: {
                        viewModel.endNavigation()
                        isPresented = false
                    }) {
                        Label("End", systemImage: "stop.fill")
                            .font(.callout)
                            .fontWeight(.medium)
                            .foregroundStyle(.white)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 10)
                            .background(.red.opacity(0.7))
                            .clipShape(Capsule())
                    }
                }
            }
            
            // Debug panel
            if viewModel.showDebugPanel {
                debugPanel
            }
        }
        .padding(.bottom, 40)
    }
    
    // MARK: - Arrival Overlay
    
    private var arrivalOverlay: some View {
        VStack(spacing: 20) {
            Spacer()
            
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 64))
                .foregroundStyle(.green)
            
            Text("You've arrived!")
                .font(.title)
                .fontWeight(.bold)
                .foregroundStyle(.white)
            
            Text(viewModel.destinationLabel)
                .font(.title3)
                .foregroundStyle(.white.opacity(0.8))
            
            if viewModel.isSimulated {
                Text("Demo session")
                    .font(.caption)
                    .foregroundStyle(.orange)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 4)
                    .background(Color.orange.opacity(0.2))
                    .clipShape(Capsule())
            }
            
            VStack(spacing: 8) {
                Button(action: {
                    viewModel.endNavigation()
                    isPresented = false
                }) {
                    Text("Done")
                        .font(.headline)
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(.blue)
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                }
                
                Button(action: {
                    // TODO: Navigate to history
                    viewModel.endNavigation()
                    isPresented = false
                }) {
                    Text("View History")
                        .font(.subheadline)
                        .foregroundStyle(.blue)
                }
            }
            .padding(.horizontal, 32)
            .padding(.bottom, 50)
        }
        .background(
            LinearGradient(
                colors: [.clear, .black.opacity(0.85)],
                startPoint: .top,
                endPoint: .bottom
            )
        )
    }
    
    // MARK: - Debug Panel
    
    private var debugPanel: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("Debug Info")
                .font(.caption).fontWeight(.bold)
            Text("State: \(viewModel.sessionStateLabel)")
                .font(.caption2)
            Text("Marker: \(viewModel.isAligned ? "✓" : "✗")")
                .font(.caption2)
            Text("Arrows: \(viewModel.arrowCount)")
                .font(.caption2)
            Text("Progress: \(Int(viewModel.progress * 100))%")
                .font(.caption2)
            Text("Mode: \(viewModel.isSimulated ? "Simulated" : "Live")")
                .font(.caption2)
        }
        .foregroundStyle(.white)
        .padding(10)
        .background(.black.opacity(0.7))
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .padding(.horizontal, 16)
    }
}

// MARK: - ARView Container

struct ARViewContainer: UIViewRepresentable {
    @ObservedObject var viewModel: ARNavigationViewModel
    
    func makeUIView(context: Context) -> ARView {
        let arView = ARView(frame: .zero)
        arView.automaticallyConfigureSession = false
        viewModel.setupARView(arView)
        return arView
    }
    
    func updateUIView(_ uiView: ARView, context: Context) {}
}

// MARK: - View Model

class ARNavigationViewModel: ObservableObject {
    
    @Published var sessionStateLabel: String = "Initializing"
    @Published var trackingState: String = ""
    @Published var currentInstruction: String = "Point camera at entrance marker"
    @Published var destinationLabel: String = ""
    @Published var arrowCount: Int = 0
    @Published var isAligned: Bool = false
    @Published var hasArrived: Bool = false
    @Published var isSimulated: Bool = false
    @Published var progress: Double = 0.0
    @Published var showDebugPanel: Bool = false
    @Published var stateColor: Color = .orange
    
    private let sessionManager = ARSessionManager()
    private let markerDetector = ARMarkerDetector()
    private let routeRenderer = ARRouteRenderer()
    private weak var arView: ARView?
    
    func setupARView(_ arView: ARView) {
        self.arView = arView
        
        sessionManager.onTrackingStateChanged = { [weak self] isLimited, reason in
            DispatchQueue.main.async {
                self?.trackingState = isLimited ? reason : "Normal"
            }
        }
        
        markerDetector.configure(
            markerId: "marker-main",
            markerName: "entrance_marker_main",
            buildingX: 0.0, buildingY: 1.2, buildingZ: 0.0,
            buildingRotationYDeg: 0.0,
            nearestNodeId: "n01"
        )
        
        markerDetector.onMarkerDetected = { [weak self] event in
            DispatchQueue.main.async { self?.handleMarkerDetected(event) }
        }
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
        
        arView.session.delegate = AnchorDetectionDelegate(
            markerDetector: markerDetector,
            sessionManager: sessionManager
        )
    }
    
    func simulateAlignment() {
        isSimulated = true
        let event = MarkerDetectionEvent(
            markerId: "marker-main",
            entranceNodeId: "n01",
            markerBuildingX: 0.0, markerBuildingY: 1.2, markerBuildingZ: 0.0,
            markerArX: 0.0, markerArY: 0.0, markerArZ: -1.0,
            markerArRotationYDeg: 0.0, markerBuildingRotationYDeg: 0.0,
            confidence: 1.0
        )
        handleMarkerDetected(event)
    }
    
    func advanceProgress() {
        progress = min(progress + 0.15, 1.0)
        
        if progress >= 0.95 {
            arriveAtDestination()
        } else if progress >= 0.8 {
            currentInstruction = "Approaching destination..."
            stateColor = .green
        }
    }
    
    func endNavigation() {
        sessionManager.stopSession()
        if let arView = arView {
            routeRenderer.clearRoute(from: arView)
        }
        sessionStateLabel = "Ended"
        stateColor = .gray
    }
    
    private func handleMarkerDetected(_ event: MarkerDetectionEvent) {
        isAligned = true
        sessionStateLabel = "Navigating"
        stateColor = .blue
        currentInstruction = "Follow the arrows"
        progress = 0.0
        
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
        
        let demoArrows = generateDemoArrows()
        if let arView = arView {
            routeRenderer.renderRoute(in: arView, arrows: demoArrows)
            arrowCount = routeRenderer.renderedArrowCount
        }
    }
    
    private func arriveAtDestination() {
        hasArrived = true
        sessionStateLabel = "Arrived"
        stateColor = .green
        currentInstruction = "You've arrived!"
    }
    
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

class AnchorDetectionDelegate: NSObject, ARSessionDelegate {
    let markerDetector: ARMarkerDetector
    let sessionManager: ARSessionManager
    
    init(markerDetector: ARMarkerDetector, sessionManager: ARSessionManager) {
        self.markerDetector = markerDetector
        self.sessionManager = sessionManager
    }
    
    func session(_ session: ARSession, didAdd anchors: [ARAnchor]) {
        for anchor in anchors { markerDetector.processAnchor(anchor) }
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
