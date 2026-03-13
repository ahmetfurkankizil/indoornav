import SwiftUI
import ARKit
import RealityKit

/// Native AR navigation view with live progress, recenter, and arrival flow.
struct ARNavigationView: View {
    @Binding var isPresented: Bool
    @StateObject private var viewModel = ARNavigationViewModel()
    
    var buildingId: String = "demo-office-01"
    var destinationName: String = ""
    
    var body: some View {
        ZStack {
            ARViewContainer(viewModel: viewModel)
                .ignoresSafeArea()
            
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
            viewModel.destinationLabel = destinationName.isEmpty ? "Conference Room" : destinationName
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
                
                if viewModel.trackingQuality != "Normal" {
                    Text("⚠ \(viewModel.trackingQuality)")
                        .font(.caption2)
                        .foregroundStyle(.yellow)
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
        VStack(spacing: 12) {
            // Progress bar
            if viewModel.isAligned {
                VStack(spacing: 4) {
                    ProgressView(value: viewModel.progress, total: 1.0)
                        .tint(viewModel.progress > 0.8 ? .green : .blue)
                    
                    HStack {
                        Text("\(Int(viewModel.progress * 100))%")
                            .font(.caption2)
                        Spacer()
                        if viewModel.remainingDistance > 0 {
                            Text("\(String(format: "%.1f", viewModel.remainingDistance))m remaining")
                                .font(.caption2)
                        }
                    }
                    .foregroundStyle(.white.opacity(0.7))
                }
                .padding(.horizontal, 32)
            }
            
            // Instruction
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
            
            // Destination + low confidence warning
            HStack {
                Image(systemName: "location.fill")
                    .foregroundStyle(.blue)
                Text(viewModel.destinationLabel)
                    .font(.callout)
                    .foregroundStyle(.white.opacity(0.9))
                Spacer()
                if viewModel.isLowConfidence {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .foregroundStyle(.yellow)
                        .font(.caption)
                }
            }
            .padding(.horizontal, 24)
            
            // Action buttons
            HStack(spacing: 10) {
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
                    Button(action: { viewModel.simulateAlignment() }) {
                        Label("Simulate Scan", systemImage: "qrcode.viewfinder")
                            .font(.callout).fontWeight(.medium)
                            .foregroundStyle(.white)
                            .padding(.horizontal, 14).padding(.vertical, 10)
                            .background(.blue.opacity(0.85))
                            .clipShape(Capsule())
                    }
                } else {
                    // Recenter / Rescan
                    Button(action: { viewModel.rescanMarker() }) {
                        Label("Rescan", systemImage: "arrow.counterclockwise")
                            .font(.callout).fontWeight(.medium)
                            .foregroundStyle(.white)
                            .padding(.horizontal, 12).padding(.vertical, 10)
                            .background(.indigo.opacity(0.8))
                            .clipShape(Capsule())
                    }
                    
                    // Advance (demo only)
                    if viewModel.isSimulated {
                        Button(action: { viewModel.advanceProgress() }) {
                            Label("Advance", systemImage: "forward.fill")
                                .font(.callout).fontWeight(.medium)
                                .foregroundStyle(.white)
                                .padding(.horizontal, 12).padding(.vertical, 10)
                                .background(.indigo.opacity(0.7))
                                .clipShape(Capsule())
                        }
                    }
                    
                    Button(action: {
                        viewModel.endNavigation()
                        isPresented = false
                    }) {
                        Label("End", systemImage: "stop.fill")
                            .font(.callout).fontWeight(.medium)
                            .foregroundStyle(.white)
                            .padding(.horizontal, 12).padding(.vertical, 10)
                            .background(.red.opacity(0.7))
                            .clipShape(Capsule())
                    }
                }
            }
            
            // Debug panel (collapsible)
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
                .font(.title).fontWeight(.bold)
                .foregroundStyle(.white)
            
            Text(viewModel.destinationLabel)
                .font(.title3)
                .foregroundStyle(.white.opacity(0.8))
            
            if viewModel.isSimulated {
                Text("Demo session")
                    .font(.caption)
                    .foregroundStyle(.orange)
                    .padding(.horizontal, 12).padding(.vertical, 4)
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
        VStack(alignment: .leading, spacing: 3) {
            Text("Debug").font(.caption).fontWeight(.bold)
            Text("State: \(viewModel.sessionStateLabel)").font(.caption2)
            Text("Marker: \(viewModel.isAligned ? "✓" : "✗")").font(.caption2)
            Text("Arrows: \(viewModel.arrowCount)").font(.caption2)
            Text("Progress: \(Int(viewModel.progress * 100))%").font(.caption2)
            Text("Remaining: \(String(format: "%.1f", viewModel.remainingDistance))m").font(.caption2)
            Text("Tracking: \(viewModel.trackingQuality)").font(.caption2)
            Text("Confidence: \(viewModel.isLowConfidence ? "Low" : "OK")").font(.caption2)
            Text("Mode: \(viewModel.isSimulated ? "Simulated" : "Live")").font(.caption2)
            Text("Segment: \(viewModel.nearestSegment)").font(.caption2)
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
    @Published var trackingQuality: String = "Normal"
    @Published var currentInstruction: String = "Point camera at entrance marker"
    @Published var destinationLabel: String = ""
    @Published var arrowCount: Int = 0
    @Published var isAligned: Bool = false
    @Published var hasArrived: Bool = false
    @Published var isSimulated: Bool = false
    @Published var progress: Double = 0.0
    @Published var remainingDistance: Double = 0.0
    @Published var isLowConfidence: Bool = false
    @Published var nearestSegment: Int = 0
    @Published var showDebugPanel: Bool = false
    @Published var stateColor: Color = .orange
    
    private let sessionManager = ARSessionManager()
    private let markerDetector = ARMarkerDetector()
    private let routeRenderer = ARRouteRenderer()
    private weak var arView: ARView?
    
    /// Loaded building package (nil = fall back to hardcoded demo)
    private var loadedPackage: BuildingPackageLoader.LoadedPackage?
    
    // Alignment state
    private var alignmentOffsetX: Double = 0
    private var alignmentOffsetY: Double = 0
    private var alignmentOffsetZ: Double = 0
    private var alignmentRotYDeg: Double = 0
    
    private var poseTimer: Timer?
    
    func setupARView(_ arView: ARView) {
        self.arView = arView
        
        // Try loading house package from bundle
        loadedPackage = BuildingPackageLoader.loadFromBundle()
        if let pkg = loadedPackage {
            print("[ViewModel] Using house package: \(pkg.config.buildingName)")
            destinationLabel = pkg.destinationName
        }
        
        sessionManager.onTrackingStateChanged = { [weak self] isLimited, reason in
            DispatchQueue.main.async {
                self?.trackingQuality = isLimited ? reason : "Normal"
                if isLimited {
                    self?.isLowConfidence = true
                }
            }
        }
        
        // Configure marker detector from package or defaults
        let marker = loadedPackage?.entranceMarker
        markerDetector.configure(
            markerId: marker?.id ?? "marker-main",
            markerName: "entrance_marker_main",
            buildingX: marker?.position.x ?? 0.0,
            buildingY: marker?.position.y ?? 1.2,
            buildingZ: marker?.position.z ?? 0.0,
            buildingRotationYDeg: 0.0,
            nearestNodeId: marker?.startNodeId ?? "n01"
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
            markerId: "marker-main", entranceNodeId: "n01",
            markerBuildingX: 0.0, markerBuildingY: 1.2, markerBuildingZ: 0.0,
            markerArX: 0.0, markerArY: 0.0, markerArZ: -1.0,
            markerArRotationYDeg: 0.0, markerBuildingRotationYDeg: 0.0,
            confidence: 1.0, role: .entrance
        )
        handleMarkerDetected(event)
    }
    
    func rescanMarker() {
        sessionStateLabel = "Rescanning..."
        stateColor = .orange
        currentInstruction = "Point camera at entrance marker to recenter"
        // Marker detector is already active; next detection will re-align
    }
    
    func advanceProgress() {
        progress = min(progress + 0.15, 1.0)
        remainingDistance = max(0, remainingDistance - 2.0)
        checkArrival()
    }
    
    func endNavigation() {
        poseTimer?.invalidate()
        poseTimer = nil
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
        isLowConfidence = false
        
        if !isSimulated { progress = 0.0 }
        
        let rotDeg = event.markerArRotationYDeg - event.markerBuildingRotationYDeg
        let cosR = cos(rotDeg * .pi / 180.0)
        let sinR = sin(rotDeg * .pi / 180.0)
        let rotBldgX = event.markerBuildingX * cosR + event.markerBuildingZ * sinR
        let rotBldgZ = -event.markerBuildingX * sinR + event.markerBuildingZ * cosR
        
        alignmentOffsetX = event.markerArX - rotBldgX
        alignmentOffsetY = event.markerArY - event.markerBuildingY
        alignmentOffsetZ = event.markerArZ - rotBldgZ
        alignmentRotYDeg = rotDeg
        
        routeRenderer.setAlignmentTransform(
            offsetX: alignmentOffsetX, offsetY: alignmentOffsetY,
            offsetZ: alignmentOffsetZ, rotationYDeg: alignmentRotYDeg
        )
        
        let arrowsToRender: [ArrowPlacementData]
        if let pkg = loadedPackage {
            arrowsToRender = pkg.arrows
            remainingDistance = pkg.totalDistance
        } else {
            arrowsToRender = generateDemoArrows()
            remainingDistance = 13.0
        }
        
        if let arView = arView {
            routeRenderer.renderRoute(in: arView, arrows: arrowsToRender)
            arrowCount = routeRenderer.renderedArrowCount
        }
        
        // Start live pose updates for non-simulated mode
        if !isSimulated {
            startPoseUpdates()
        }
    }
    
    private func startPoseUpdates() {
        poseTimer?.invalidate()
        poseTimer = Timer.scheduledTimer(withTimeInterval: 0.5, repeats: true) { [weak self] _ in
            self?.sampleCameraPose()
        }
    }
    
    private func sampleCameraPose() {
        guard let arView = arView, isAligned, !isSimulated else { return }
        let transform = arView.cameraTransform
        let pos = transform.translation
        
        // Convert AR-world → building-local via inverse alignment
        let radians = -alignmentRotYDeg * .pi / 180.0
        let cosR = cos(radians)
        let sinR = sin(radians)
        let tx = Double(pos.x) - alignmentOffsetX
        let tz = Double(pos.z) - alignmentOffsetZ
        let bx = tx * cosR + tz * sinR
        let bz = -tx * sinR + tz * cosR
        
        // Route points for progress tracking — from package or demo
        let routePoints: [(Double, Double)]
        let totalDist: Double
        if let pkg = loadedPackage {
            routePoints = pkg.routePoints
            totalDist = pkg.totalDistance
        } else {
            routePoints = [(0,0), (3,0), (6,0), (6,4), (3,4)]
            totalDist = 13.0
        }
        
        // Compute segment lengths
        var segLengths: [Double] = []
        for i in 0..<(routePoints.count - 1) {
            let dx = routePoints[i+1].0 - routePoints[i].0
            let dz = routePoints[i+1].1 - routePoints[i].1
            segLengths.append(sqrt(dx*dx + dz*dz))
        }
        
        var bestDist = Double.greatestFiniteMagnitude
        var bestCum = 0.0
        var cumDist = 0.0
        
        for i in 0..<(routePoints.count - 1) {
            let (ax, az) = routePoints[i]
            let (bpx, bpz) = routePoints[i+1]
            let segLen = segLengths[i]
            let dx = bpx - ax; let dz = bpz - az
            var t = ((bx - ax) * dx + (bz - az) * dz) / (dx*dx + dz*dz)
            t = max(0, min(1, t))
            let px = ax + t * dx; let pz = az + t * dz
            let d = sqrt((bx-px)*(bx-px) + (bz-pz)*(bz-pz))
            if d < bestDist {
                bestDist = d
                bestCum = cumDist + t * segLen
            }
            cumDist += segLen
        }
        
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            let newProgress = bestCum / totalDist
            if newProgress > self.progress {
                self.progress = min(newProgress, 1.0)
            }
            self.remainingDistance = max(0, totalDist - bestCum)
            self.isLowConfidence = bestDist > 3.0
            self.checkArrival()
        }
    }
    
    private func checkArrival() {
        if progress >= 0.95 || remainingDistance < 1.5 {
            arriveAtDestination()
        } else if progress >= 0.8 {
            currentInstruction = "Approaching destination..."
            stateColor = .green
        }
    }
    
    private func arriveAtDestination() {
        poseTimer?.invalidate()
        hasArrived = true
        sessionStateLabel = "Arrived"
        stateColor = .green
        currentInstruction = "You've arrived!"
    }
    
    private func generateDemoArrows() -> [ArrowPlacementData] {
        return [
            ArrowPlacementData(id: "a0", positionX: 0.0, positionY: 0.05, positionZ: 0.0, forwardDx: 1.0, forwardDy: 0.0, forwardDz: 0.0, type: .follow, label: nil),
            ArrowPlacementData(id: "a1", positionX: 1.5, positionY: 0.05, positionZ: 0.0, forwardDx: 1.0, forwardDy: 0.0, forwardDz: 0.0, type: .follow, label: nil),
            ArrowPlacementData(id: "a2", positionX: 3.0, positionY: 0.05, positionZ: 0.0, forwardDx: 1.0, forwardDy: 0.0, forwardDz: 0.0, type: .follow, label: nil),
            ArrowPlacementData(id: "a3", positionX: 4.5, positionY: 0.05, positionZ: 0.0, forwardDx: 1.0, forwardDy: 0.0, forwardDz: 0.0, type: .follow, label: nil),
            ArrowPlacementData(id: "a4", positionX: 6.0, positionY: 0.05, positionZ: 0.0, forwardDx: 0.0, forwardDy: 0.0, forwardDz: 1.0, type: .turnRight, label: "Turn right"),
            ArrowPlacementData(id: "a5", positionX: 6.0, positionY: 0.05, positionZ: 1.5, forwardDx: 0.0, forwardDy: 0.0, forwardDz: 1.0, type: .follow, label: nil),
            ArrowPlacementData(id: "a6", positionX: 6.0, positionY: 0.05, positionZ: 3.0, forwardDx: 0.0, forwardDy: 0.0, forwardDz: 1.0, type: .follow, label: nil),
            ArrowPlacementData(id: "a7", positionX: 6.0, positionY: 0.05, positionZ: 4.0, forwardDx: -1.0, forwardDy: 0.0, forwardDz: 0.0, type: .turnLeft, label: "Turn left"),
            ArrowPlacementData(id: "a8", positionX: 4.5, positionY: 0.05, positionZ: 4.0, forwardDx: -1.0, forwardDy: 0.0, forwardDz: 0.0, type: .follow, label: nil),
            ArrowPlacementData(id: "a9", positionX: 3.0, positionY: 0.05, positionZ: 4.0, forwardDx: -1.0, forwardDy: 0.0, forwardDz: 0.0, type: .destination, label: "Conference Room"),
        ]
    }
}

// MARK: - Session Delegate

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
