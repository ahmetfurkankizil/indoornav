import SwiftUI
import ARKit
import RealityKit

/// Native AR navigation view with alignment-gated rendering.
///
/// Phase 4: AR does not render navigation arrows until initial alignment is
/// established from the entrance marker. The entrance marker metadata from
/// the reviewed package drives the alignment transform.
///
/// Flow:
///   1. AR session starts with image detection for the entrance marker.
///   2. Pre-alignment overlay: "Point camera at the entrance QR"
///   3. On marker detection → compute alignment transform → place arrows.
///   4. Phase 3 rolling lookahead + fade-behind + distance arrival continues.
///
/// This view REQUIRES an explicit destination and precomputed route package.
struct ARNavigationView: View {
    @Binding var isPresented: Bool
    @StateObject private var viewModel = ARNavigationViewModel()

    /// Required: the user-selected destination name.
    let destinationName: String
    /// Required: precomputed route package from BuildingPackageLoader.
    let routePackage: BuildingPackageLoader.LoadedPackage
    /// Validated entrance marker from the reviewed package (may be nil for legacy flows).
    let entranceMarker: BuildingPackageLoader.PackageMarker?

    var body: some View {
        ZStack {
            ARViewContainer(
                viewModel: viewModel,
                destinationName: destinationName,
                routePackage: routePackage,
                entranceMarker: entranceMarker
            )
            .ignoresSafeArea()

            VStack(spacing: 0) {
                topBar
                Spacer()

                if let assetError = viewModel.markerAssetError {
                    configErrorOverlay(message: assetError)
                } else if viewModel.hasArrived {
                    arrivalOverlay
                } else if !viewModel.isAligned {
                    alignmentOverlay
                } else {
                    activeNavigationOverlay
                }
            }
        }
        .onAppear {
            // configure + startSession are called from ARViewContainer.makeUIView
            // to guarantee arView is non-nil. onAppear can fire before makeUIView
            // in some SwiftUI layout scenarios, causing a black screen.
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

                if viewModel.isAligned && viewModel.trackingQuality != "Normal" {
                    Text("\(viewModel.trackingQuality)")
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

    // MARK: - Pre-Alignment Overlay

    private var alignmentOverlay: some View {
        VStack(spacing: 20) {
            Spacer()

            // Scanning animation
            Image(systemName: "viewfinder")
                .font(.system(size: 56))
                .foregroundStyle(.white.opacity(0.8))

            Text("Point the camera at the entrance poster")
                .font(.title3)
                .fontWeight(.semibold)
                .foregroundStyle(.white)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)

            Text("Hold steady at the same poster you scanned")
                .font(.subheadline)
                .foregroundStyle(.white.opacity(0.6))
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)

            if viewModel.alignmentTimedOut {
                VStack(spacing: 12) {
                    Text(viewModel.timeoutReasonMessage)
                        .font(.subheadline)
                        .foregroundStyle(.orange)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 24)

                    Text(viewModel.timeoutHintMessage)
                        .font(.caption)
                        .foregroundStyle(.white.opacity(0.6))
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 24)

                    HStack(spacing: 12) {
                        Button(action: { viewModel.retryAlignment() }) {
                            Label("Retry", systemImage: "arrow.counterclockwise")
                                .font(.callout).fontWeight(.medium)
                                .foregroundStyle(.white)
                                .padding(.horizontal, 16).padding(.vertical, 10)
                                .background(.blue.opacity(0.85))
                                .clipShape(Capsule())
                        }

                        Button(action: {
                            viewModel.endNavigation()
                            isPresented = false
                        }) {
                            Label("Cancel", systemImage: "xmark")
                                .font(.callout).fontWeight(.medium)
                                .foregroundStyle(.white)
                                .padding(.horizontal, 16).padding(.vertical, 10)
                                .background(.gray.opacity(0.6))
                                .clipShape(Capsule())
                        }
                    }
                }
            } else {
                ProgressView()
                    .progressViewStyle(.circular)
                    .tint(.white)
                    .scaleEffect(1.2)

                Text("Detecting entrance poster...")
                    .font(.caption)
                    .foregroundStyle(.white.opacity(0.5))
            }

            // Simulate alignment button (for demo / testing)
            #if targetEnvironment(simulator)
            Button(action: { viewModel.simulateAlignment() }) {
                Label("Simulate Marker Scan", systemImage: "qrcode.viewfinder")
                    .font(.callout).fontWeight(.medium)
                    .foregroundStyle(.white)
                    .padding(.horizontal, 14).padding(.vertical, 10)
                    .background(.blue.opacity(0.85))
                    .clipShape(Capsule())
            }
            .padding(.top, 8)
            #endif

            Spacer()
        }
        .background(
            LinearGradient(
                colors: [.clear, .black.opacity(0.6)],
                startPoint: .top,
                endPoint: .bottom
            )
        )
    }

    // MARK: - Active Navigation Overlay

    private var activeNavigationOverlay: some View {
        VStack(spacing: 12) {
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

            HStack(spacing: 10) {
                Button(action: { viewModel.showDebugPanel.toggle() }) {
                    Image(systemName: "ladybug")
                        .font(.callout)
                        .foregroundStyle(.white)
                        .padding(10)
                        .background(.ultraThinMaterial)
                        .clipShape(Circle())
                }

                #if targetEnvironment(simulator)
                Button(action: { viewModel.advanceProgress() }) {
                    Label("Advance", systemImage: "forward.fill")
                        .font(.callout).fontWeight(.medium)
                        .foregroundStyle(.white)
                        .padding(.horizontal, 12).padding(.vertical, 10)
                        .background(.indigo.opacity(0.7))
                        .clipShape(Capsule())
                }
                #endif

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

            Text("You've arrived at \(viewModel.destinationLabel)")
                .font(.title).fontWeight(.bold)
                .foregroundStyle(.white)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 24)

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

    // MARK: - Configuration Error Overlay

    private func configErrorOverlay(message: String) -> some View {
        VStack(spacing: 20) {
            Spacer()

            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 56))
                .foregroundStyle(.red)

            Text("Poster Asset Missing")
                .font(.title3)
                .fontWeight(.bold)
                .foregroundStyle(.white)

            Text(message)
                .font(.subheadline)
                .foregroundStyle(.white.opacity(0.8))
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)

            Text("Run generate-entrance-poster.sh and rebuild.")
                .font(.caption)
                .foregroundStyle(.white.opacity(0.5))
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)

            Button(action: {
                viewModel.endNavigation()
                isPresented = false
            }) {
                Label("Go Back", systemImage: "arrow.left")
                    .font(.callout).fontWeight(.medium)
                    .foregroundStyle(.white)
                    .padding(.horizontal, 20).padding(.vertical, 12)
                    .background(.blue.opacity(0.85))
                    .clipShape(Capsule())
            }

            Spacer()
        }
        .background(Color.black.opacity(0.8))
    }

    // MARK: - Debug Panel

    private var debugPanel: some View {
        VStack(alignment: .leading, spacing: 3) {
            Text("Debug").font(.caption).fontWeight(.bold)
            Text("State: \(viewModel.sessionStateLabel)").font(.caption2)
            Text("Aligned: \(viewModel.isAligned ? "Y" : "N")").font(.caption2)
            Text("Arrows visible: \(viewModel.arrowCount)").font(.caption2)
            Text("Progress: \(Int(viewModel.progress * 100))%").font(.caption2)
            Text("Remaining: \(String(format: "%.1f", viewModel.remainingDistance))m").font(.caption2)
            Text("To dest: \(String(format: "%.1f", viewModel.distanceToDestination))m").font(.caption2)
            Text("Tracking: \(viewModel.trackingQuality)").font(.caption2)
            Text("Confidence: \(viewModel.isLowConfidence ? "Low" : "OK")").font(.caption2)
            Text("Mode: \(viewModel.isSimulated ? "Simulated" : "Live")").font(.caption2)
            Text("Img candidates: \(viewModel.detectionCandidateCount)").font(.caption2)
            Text("Rejected: \(viewModel.detectionRejectedCount)").font(.caption2)
            Text("Expected marker: \(viewModel.expectedMarkerNameForDebug)").font(.caption2)
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
    let destinationName: String
    let routePackage: BuildingPackageLoader.LoadedPackage
    let entranceMarker: BuildingPackageLoader.PackageMarker?

    func makeUIView(context: Context) -> ARView {
        let arView = ARView(frame: .zero)
        arView.automaticallyConfigureSession = false

        // Defer to after the current SwiftUI render cycle to avoid
        // "Publishing changes from within view updates" warnings.
        // arView is a reference type — safe to capture.
        let vm = viewModel
        let dest = destinationName
        let pkg = routePackage
        let marker = entranceMarker
        DispatchQueue.main.async {
            vm.destinationLabel = dest
            vm.configure(with: pkg, entranceMarker: marker)
            vm.setupARView(arView)
            vm.startSession()
        }
        return arView
    }

    func updateUIView(_ uiView: ARView, context: Context) {}
}

// MARK: - View Model

class ARNavigationViewModel: ObservableObject {

    @Published var sessionStateLabel: String = "Initializing"
    @Published var trackingQuality: String = "Normal"
    @Published var currentInstruction: String = ""
    @Published var destinationLabel: String = ""
    @Published var arrowCount: Int = 0
    @Published var isAligned: Bool = false
    @Published var hasArrived: Bool = false
    @Published var isSimulated: Bool = false
    @Published var progress: Double = 0.0
    @Published var remainingDistance: Double = 0.0
    @Published var distanceToDestination: Double = 0.0
    @Published var isLowConfidence: Bool = false
    @Published var showDebugPanel: Bool = false
    @Published var stateColor: Color = .orange
    @Published var alignmentTimedOut: Bool = false
    @Published var markerAssetError: String?

    /// Categorized timeout reason for UI display.
    @Published var timeoutReasonMessage: String = "No matching entrance poster detected"
    @Published var timeoutHintMessage: String = ""

    private let sessionManager = ARSessionManager()
    private let markerDetector = ARMarkerDetector()
    private let routeRenderer = ARRouteRenderer()
    private weak var arView: ARView?
    /// Retained strongly so ARSession's weak delegate reference stays alive.
    private var anchorDelegate: AnchorDetectionDelegate?

    /// Route package provided by the flow.
    private var routePackage: BuildingPackageLoader.LoadedPackage?

    /// Entrance marker from the reviewed package.
    private var entranceMarker: BuildingPackageLoader.PackageMarker?

    /// User's cumulative distance along the route (building-local).
    private var userCumulativeDistance: Double = 0.0

    /// Destination threshold from route_rendering.json.
    private var destinationThreshold: Double = 1.5

    // Alignment state
    private var alignmentOffsetX: Double = 0
    private var alignmentOffsetY: Double = 0
    private var alignmentOffsetZ: Double = 0
    private var alignmentRotYDeg: Double = 0

    private var poseTimer: Timer?
    private var alignmentTimeoutTimer: Timer?

    /// Alignment timeout in seconds.
    private let alignmentTimeoutSeconds: TimeInterval = 30.0

    // MARK: - Debug Accessors

    var detectionCandidateCount: Int { markerDetector.totalCandidatesSeen }
    var detectionRejectedCount: Int { markerDetector.rejectedCandidates }
    var expectedMarkerNameForDebug: String { markerDetector.expectedMarkerName ?? "none" }

    /// Configure with a precomputed route package and entrance marker.
    func configure(with package: BuildingPackageLoader.LoadedPackage, entranceMarker: BuildingPackageLoader.PackageMarker?) {
        self.routePackage = package
        self.entranceMarker = entranceMarker ?? package.entranceMarker

        let renderConfig = package.config.routeRendering
        self.destinationThreshold = renderConfig.destinationThresholdMeters
        routeRenderer.configureRendering(
            lookaheadDistanceMeters: renderConfig.lookaheadDistanceMeters
        )
    }

    func setupARView(_ arView: ARView) {
        self.arView = arView

        sessionManager.onTrackingStateChanged = { [weak self] isLimited, reason in
            DispatchQueue.main.async {
                self?.trackingQuality = isLimited ? reason : "Normal"
                // Only flag low confidence if already aligned; before alignment
                // tracking is expected to be limited.
                if isLimited && self?.isAligned == true {
                    self?.isLowConfidence = true
                }
            }
        }

        sessionManager.onMarkerAssetMissing = { [weak self] imageName in
            DispatchQueue.main.async {
                self?.markerAssetError = "Marker image '\(imageName)' not found in app bundle. Rebuild with the correct AR Resources asset."
                self?.sessionStateLabel = "Configuration Error"
                self?.stateColor = .red
                print("[ARNav] Marker asset missing: \(imageName)")
            }
        }

        // Configure marker detector with entrance marker metadata
        let marker = self.entranceMarker
        let rotY = marker?.rotationYDegrees ?? 0.0

        markerDetector.configure(
            markerId: marker?.id ?? "marker-entrance-a",
            markerName: marker?.referenceImageName ?? "entrance_marker_main",
            buildingX: marker?.position.x ?? 0.0,
            buildingY: marker?.position.y ?? 1.2,
            buildingZ: marker?.position.z ?? 0.0,
            buildingRotationYDeg: rotY,
            nearestNodeId: marker?.startNodeId ?? "entrance_a"
        )

        markerDetector.onMarkerDetected = { [weak self] event in
            DispatchQueue.main.async { self?.handleMarkerDetected(event) }
        }
    }

    func startSession() {
        guard let arView = arView else { return }
        sessionStateLabel = "Waiting for Poster"
        stateColor = .orange
        alignmentTimedOut = false

        let marker = self.entranceMarker
        let markerWidth = marker?.physicalWidthMeters ?? 0.21
        let markerImageName = marker?.referenceImageName ?? "entrance_marker_main"

        // Pre-start validation: verify asset chain
        print("[ARNav] --- Pre-Start Validation ---")
        print("[ARNav] Entrance marker from package: id=\(marker?.id ?? "nil"), refImage=\(marker?.referenceImageName ?? "nil"), width=\(marker?.physicalWidthMeters ?? -1)m")

        #if !targetEnvironment(simulator)
        // Check that the reference image exists before starting the session
        if !ARSessionManager.hasReferenceImage(named: markerImageName) {
            let msg = "Reference image '\(markerImageName)' not found in AR Resources asset catalog. " +
                "The entrance poster image must be added to Assets.xcassets > AR Resources with this exact name."
            print("[ARNav] VALIDATION FAILED: \(msg)")
            markerAssetError = msg
            sessionStateLabel = "Configuration Error"
            stateColor = .red
            return
        }
        print("[ARNav] Asset validation passed: '\(markerImageName)' found in AR Resources")
        #endif

        sessionManager.startSession(
            arView: arView,
            markerImageName: markerImageName,
            markerPhysicalWidth: markerWidth
        )

        let delegate = AnchorDetectionDelegate(
            markerDetector: markerDetector,
            sessionManager: sessionManager
        )
        anchorDelegate = delegate
        arView.session.delegate = delegate

        // Start alignment timeout
        startAlignmentTimeout()
    }

    // MARK: - Alignment Timeout

    private func startAlignmentTimeout() {
        alignmentTimeoutTimer?.invalidate()
        alignmentTimeoutTimer = Timer.scheduledTimer(
            withTimeInterval: alignmentTimeoutSeconds,
            repeats: false
        ) { [weak self] _ in
            DispatchQueue.main.async {
                guard let self = self, !self.isAligned else { return }
                self.alignmentTimedOut = true
                self.stateColor = .orange

                // Categorize the failure reason
                let reason = self.markerDetector.detectionFailureReason
                switch reason {
                case .noCandidatesSeen:
                    self.sessionStateLabel = "No poster detected"
                    self.timeoutReasonMessage = "No entrance poster detected in camera"
                    self.timeoutHintMessage = "Make sure the printed entrance poster is visible, well-lit, and within 1–2 meters."
                    print("[ARNav] Timeout: zero image anchors received. ARKit never saw any reference image candidate.")
                    print("[ARNav] Likely cause: printed poster does not match bundled AR reference image asset, or poster not in view.")

                case .candidatesRejected(let seen, let names):
                    self.sessionStateLabel = "Poster mismatch"
                    self.timeoutReasonMessage = "Detected \(seen) image(s) but none matched the expected entrance poster"
                    self.timeoutHintMessage = "The printed poster must exactly match the bundled reference image. Detected: \(names.joined(separator: ", "))"
                    print("[ARNav] Timeout: \(seen) candidates seen, all rejected. Names: \(names). Expected: '\(self.markerDetector.expectedMarkerName ?? "?")'")

                case .assetMissing:
                    self.sessionStateLabel = "Asset missing"
                    self.timeoutReasonMessage = "Entrance poster asset not found in app bundle"
                    self.timeoutHintMessage = "Rebuild the app with the correct AR reference image in Assets.xcassets."
                    print("[ARNav] Timeout: marker asset was missing from bundle.")
                }

                print("[ARNav] Session running: \(self.sessionManager.isSessionRunning), ref images loaded: \(self.sessionManager.hasLoadedReferenceImages)")
                print("[ARNav] Loaded image names: \(self.sessionManager.loadedImageNames)")
            }
        }
    }

    func retryAlignment() {
        alignmentTimedOut = false
        sessionStateLabel = "Waiting for Marker"
        stateColor = .orange
        markerDetector.reset()

        // Restart the session to reset tracking
        guard let arView = arView else { return }
        let marker = self.entranceMarker
        let markerWidth = marker?.physicalWidthMeters ?? 0.21
        let markerImageName = marker?.referenceImageName ?? "entrance_marker_main"

        sessionManager.startSession(
            arView: arView,
            markerImageName: markerImageName,
            markerPhysicalWidth: markerWidth
        )

        let delegate = AnchorDetectionDelegate(
            markerDetector: markerDetector,
            sessionManager: sessionManager
        )
        anchorDelegate = delegate
        arView.session.delegate = delegate

        startAlignmentTimeout()
    }

    func simulateAlignment() {
        isSimulated = true
        alignmentTimeoutTimer?.invalidate()

        let marker = self.entranceMarker
        let event = MarkerDetectionEvent(
            markerId: marker?.id ?? "marker-entrance-a",
            entranceNodeId: marker?.startNodeId ?? "entrance_a",
            markerBuildingX: marker?.position.x ?? 0.0,
            markerBuildingY: marker?.position.y ?? 1.2,
            markerBuildingZ: marker?.position.z ?? 0.0,
            markerArX: 0.0,
            markerArY: 0.0,
            markerArZ: -1.0,
            markerArRotationYDeg: 0.0,
            markerBuildingRotationYDeg: marker?.rotationYDegrees ?? 0.0,
            confidence: 1.0,
            role: .entrance
        )
        handleMarkerDetected(event)
    }

    func advanceProgress() {
        guard let pkg = routePackage else { return }
        userCumulativeDistance = min(userCumulativeDistance + 2.0, pkg.totalDistance)
        progress = min(userCumulativeDistance / pkg.totalDistance, 1.0)
        remainingDistance = max(0, pkg.totalDistance - userCumulativeDistance)

        // Approximate: use remaining route distance as proxy in simulation
        distanceToDestination = remainingDistance

        routeRenderer.updateVisibility(userCumulativeDistance: userCumulativeDistance)
        arrowCount = routeRenderer.renderedArrowCount

        checkArrival(distToDest: distanceToDestination)
    }

    func endNavigation() {
        poseTimer?.invalidate()
        poseTimer = nil
        alignmentTimeoutTimer?.invalidate()
        alignmentTimeoutTimer = nil
        sessionManager.stopSession()
        if let arView = arView {
            routeRenderer.clearRoute(from: arView)
        }
        sessionStateLabel = "Ended"
        stateColor = .gray
    }

    // MARK: - Marker Detection → Alignment Lock

    private func handleMarkerDetected(_ event: MarkerDetectionEvent) {
        alignmentTimeoutTimer?.invalidate()
        alignmentTimedOut = false
        isAligned = true
        sessionStateLabel = "Navigating"
        stateColor = .blue
        currentInstruction = "Follow the arrows"
        isLowConfidence = false

        if !isSimulated {
            progress = 0.0
            userCumulativeDistance = 0.0
        }

        // Compute alignment transform from marker detection event
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

        guard let pkg = routePackage, let arView = arView else { return }
        remainingDistance = pkg.totalDistance

        // Place all arrows (initially hidden), then show forward slice
        routeRenderer.placeAllArrows(in: arView, arrows: pkg.arrows)
        routeRenderer.updateVisibility(userCumulativeDistance: userCumulativeDistance)
        arrowCount = routeRenderer.renderedArrowCount

        print("[ARNav] Alignment locked — offset: (\(alignmentOffsetX), \(alignmentOffsetY), \(alignmentOffsetZ)), rotation: \(alignmentRotYDeg)°")

        if !isSimulated {
            startPoseUpdates()
        }
    }

    // MARK: - Pose Tracking

    private func startPoseUpdates() {
        poseTimer?.invalidate()
        poseTimer = Timer.scheduledTimer(withTimeInterval: 0.5, repeats: true) { [weak self] _ in
            self?.sampleCameraPose()
        }
    }

    private func sampleCameraPose() {
        guard let arView = arView, isAligned, !isSimulated, !hasArrived else { return }
        let transform = arView.cameraTransform
        let pos = transform.translation

        // Convert AR world → building-local
        let radians = -alignmentRotYDeg * .pi / 180.0
        let cosR = cos(radians)
        let sinR = sin(radians)
        let tx = Double(pos.x) - alignmentOffsetX
        let tz = Double(pos.z) - alignmentOffsetZ
        let bx = tx * cosR + tz * sinR
        let bz = -tx * sinR + tz * cosR

        guard let pkg = routePackage else { return }
        let routePoints = pkg.routePoints
        let totalDist = pkg.totalDistance

        // Compute cumulative distance to nearest point on route
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
            let denom = dx*dx + dz*dz
            guard denom > 0.0001 else { cumDist += segLen; continue }
            var t = ((bx - ax) * dx + (bz - az) * dz) / denom
            t = max(0, min(1, t))
            let px = ax + t * dx; let pz = az + t * dz
            let d = sqrt((bx-px)*(bx-px) + (bz-pz)*(bz-pz))
            if d < bestDist {
                bestDist = d
                bestCum = cumDist + t * segLen
            }
            cumDist += segLen
        }

        // Distance to destination door node
        let (destX, destZ) = pkg.destinationPosition
        let dxDest = bx - destX
        let dzDest = bz - destZ
        let destDist = sqrt(dxDest * dxDest + dzDest * dzDest)

        DispatchQueue.main.async { [weak self] in
            guard let self = self, !self.hasArrived else { return }

            // Only allow forward progress (no backward jumps)
            if bestCum > self.userCumulativeDistance {
                self.userCumulativeDistance = bestCum
            }
            self.progress = min(self.userCumulativeDistance / totalDist, 1.0)
            self.remainingDistance = max(0, totalDist - self.userCumulativeDistance)
            self.distanceToDestination = destDist
            self.isLowConfidence = bestDist > 3.0

            // Update arrow visibility based on progress
            self.routeRenderer.updateVisibility(userCumulativeDistance: self.userCumulativeDistance)
            self.arrowCount = self.routeRenderer.renderedArrowCount

            // Update instruction based on proximity
            if destDist < self.destinationThreshold * 2 {
                self.currentInstruction = "Approaching \(self.destinationLabel)..."
                self.stateColor = .green
            }

            self.checkArrival(distToDest: destDist)
        }
    }

    private func checkArrival(distToDest: Double) {
        guard !hasArrived else { return }
        if distToDest <= destinationThreshold {
            arriveAtDestination()
        }
    }

    private func arriveAtDestination() {
        guard !hasArrived else { return }
        poseTimer?.invalidate()
        hasArrived = true
        sessionStateLabel = "Arrived"
        stateColor = .green
        currentInstruction = "You've arrived at \(destinationLabel)"

        // Hide all guidance arrows on arrival
        routeRenderer.hideAllArrows()
        arrowCount = 0
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
        for anchor in anchors {
            if let imageAnchor = anchor as? ARImageAnchor {
                print("[AnchorDelegate] didAdd ARImageAnchor: '\(imageAnchor.referenceImage.name ?? "<unnamed>")' tracked=\(imageAnchor.isTracked)")
            }
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
