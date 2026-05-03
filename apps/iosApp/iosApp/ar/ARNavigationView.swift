import SwiftUI
import ARKit
import RealityKit

/// Native AR navigation view with optional marker-gated rendering.
///
/// Phase 4: When an entrance marker includes a reference image, AR does not
/// render navigation arrows until initial alignment is established from that
/// marker. Web-published buildings may omit the reference image; in that case
/// navigation starts from the user's current AR origin without asking for a
/// second physical poster.
///
/// Flow:
///   1. AR session starts with image detection when a marker image is configured.
///   2. Pre-alignment overlay asks for the entrance poster only in that case.
///   3. On marker detection, or immediately for markerless packages, place arrows.
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

            if let assetError = viewModel.markerAssetError {
                configErrorOverlay(message: assetError)
            } else if viewModel.hasArrived {
                arrivalOverlay
            } else if !viewModel.isAligned {
                VStack(spacing: 0) {
                    topBar
                    alignmentOverlay
                }
            } else {
                activeNavigationOverlay
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
            if !viewModel.isAligned {
                VecturStatPill(text: viewModel.sessionStateLabel, color: viewModel.stateColor)
            }

            if viewModel.isSimulated {
                VecturStatPill(text: "Demo", color: VecturTheme.amber)
            }

            Spacer()

            Button(action: {
                viewModel.endNavigation()
                isPresented = false
            }) {
                Image(systemName: "xmark")
                    .font(.system(size: 17, weight: .bold))
                    .foregroundStyle(VecturTheme.textSecondary)
                    .frame(width: 48, height: 48)
                    .background(VecturTheme.elevated.opacity(0.72))
                    .overlay(Circle().stroke(VecturTheme.borderSubtle, lineWidth: 1))
                    .clipShape(Circle())
            }
        }
        .padding(.horizontal, 16)
        .padding(.top, 8)
        .safeAreaPadding(.top)
    }

    // MARK: - Pre-Alignment Overlay

    private var alignmentOverlay: some View {
        VStack(spacing: 0) {
            Spacer()

            VecturCard(glass: true) {
                VStack(spacing: 16) {
                HStack(spacing: 14) {
                    if viewModel.alignmentTimedOut {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .font(.system(size: 30, weight: .semibold))
                            .foregroundStyle(VecturTheme.amber)
                    } else {
                        RadarSweep()
                            .frame(width: 48, height: 48)
                    }

                    VStack(alignment: .leading, spacing: 3) {
                        Text(viewModel.alignmentTimedOut ? viewModel.timeoutReasonMessage : "Looking for entrance sign…")
                            .font(.headline)
                            .foregroundStyle(VecturTheme.textPrimary)
                            .lineLimit(2)
                        Text(viewModel.alignmentTimedOut ? viewModel.timeoutHintMessage : "Point your camera at the entrance poster")
                            .font(.subheadline)
                            .foregroundStyle(VecturTheme.textMuted)
                            .lineLimit(2)
                    }

                    Spacer(minLength: 0)
                }

                AlignmentMiniIllustration()

                Text("Frames analyzed: \(viewModel.detectionCandidateCount + viewModel.detectionRejectedCount) - Markers detected: \(viewModel.detectionCandidateCount)")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(VecturTheme.textMuted)

                if viewModel.alignmentTimedOut {
                    HStack(spacing: 12) {
                        Button(action: { viewModel.retryAlignment() }) {
                            Label("Retry", systemImage: "arrow.clockwise")
                                .vecturPrimaryButton()
                        }
                        .buttonStyle(.plain)

                        Button(action: {
                            viewModel.endNavigation()
                            isPresented = false
                        }) {
                            Text("Cancel")
                                .vecturSecondaryButton()
                        }
                        .buttonStyle(.plain)
                    }
                }

                #if targetEnvironment(simulator)
                Button(action: { viewModel.simulateAlignment() }) {
                    Label("Simulate Scan", systemImage: "qrcode.viewfinder")
                        .vecturPrimaryButton()
                }
                .buttonStyle(.plain)
                #endif
            }
            }
            .padding(.horizontal, 12)
            .padding(.bottom, 32)
        }
    }

    // MARK: - Active Navigation Overlay
    //
    // Layout mirrors Apple Maps turn-by-turn:
    //   top  → instruction banner (direction icon + text + distance)
    //   bottom → ETA strip + End Route button

    private var activeNavigationOverlay: some View {
        VStack(spacing: 0) {
            instructionBanner
                .padding(.horizontal, 12)
                .safeAreaPadding(.top)

            CompassStrip(progress: viewModel.progress)
                .padding(.horizontal, 18)
                .padding(.top, 8)

            Spacer()

            bottomHUD
        }
        .padding(.bottom, 32)
    }

    private var instructionBanner: some View {
        let urgent = (viewModel.nextActionDistance ?? .greatestFiniteMagnitude) < 5.0
        return HStack(spacing: 16) {
            ZStack {
                RoundedRectangle(cornerRadius: 12)
                    .fill((urgent ? VecturTheme.amber : VecturTheme.cyan).opacity(0.16))
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke((urgent ? VecturTheme.amber : VecturTheme.cyan).opacity(0.36), lineWidth: 1)
                    )
                    .frame(width: 56, height: 56)
                Image(systemName: viewModel.nextActionIcon)
                    .font(.system(size: 26, weight: .semibold))
                    .foregroundStyle(urgent ? VecturTheme.amber : VecturTheme.cyan)
            }

            VStack(alignment: .leading, spacing: 2) {
                Text(viewModel.nextActionText)
                    .font(.title3).fontWeight(.bold)
                    .foregroundStyle(VecturTheme.textPrimary)
                    .lineLimit(1)
                if let dist = viewModel.nextActionDistance, dist < 30 {
                    HStack(alignment: .firstTextBaseline, spacing: 4) {
                        Text("in")
                            .foregroundStyle(VecturTheme.textMuted)
                        Text("\(String(format: "%.0f", dist)) m")
                            .foregroundStyle(VecturTheme.primaryGradient)
                    }
                    .font(.subheadline.weight(.bold))
                }
            }

            Spacer()

            VStack(spacing: 5) {
                Image(systemName: viewModel.trackingStatusIcon)
                    .font(.system(size: 14, weight: .bold))
                    .foregroundStyle(trackingColor)
                Text(viewModel.trackingStatusLabel)
                    .font(.system(size: 10, weight: .bold))
                    .foregroundStyle(trackingColor)
                    .lineLimit(1)
            }
            .padding(.horizontal, 9)
            .padding(.vertical, 8)
            .background(trackingColor.opacity(0.14))
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(trackingColor.opacity(0.38), lineWidth: 1))
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .background(VecturTheme.card.opacity(0.64), in: RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(VecturTheme.borderStrong, lineWidth: 1)
        )
        .shadow(color: .black.opacity(0.18), radius: 14, y: 6)
    }

    private var bottomHUD: some View {
        HStack(alignment: .center, spacing: 0) {
            ProgressRing(
                progress: viewModel.totalDistance > 0 ? 1.0 - (viewModel.remainingDistance / viewModel.totalDistance) : viewModel.progress,
                etaText: viewModel.remainingDistance > 0 ? formatETA(viewModel.remainingDistance / 1.2) : "Done",
                distanceText: viewModel.remainingDistance > 0 ? "\(String(format: "%.0f", viewModel.remainingDistance)) m" : ""
            )
            .padding(.trailing, 12)

            VStack(alignment: .leading, spacing: 3) {
                Text(viewModel.destinationLabel)
                    .font(.headline)
                    .foregroundStyle(VecturTheme.textPrimary)
                    .lineLimit(1)
                Text("Follow the path")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(VecturTheme.textMuted)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            #if targetEnvironment(simulator)
            Button(action: { viewModel.advanceProgress() }) {
                Image(systemName: "forward.fill")
                    .font(.callout)
                    .foregroundStyle(.white)
                    .padding(10)
                    .background(Color.indigo)
                    .clipShape(Circle())
            }
            .padding(.trailing, 10)
            #endif

            Button(action: {
                viewModel.endNavigation()
                isPresented = false
            }) {
                Image(systemName: "xmark")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(.white)
                    .frame(width: 46, height: 46)
                    .background(VecturTheme.red)
                    .clipShape(Circle())
            }
        }
        .padding(14)
        .background(VecturTheme.card.opacity(0.64), in: RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(VecturTheme.borderStrong, lineWidth: 1)
        )
        .padding(.horizontal, 12)
    }

    private var trackingColor: Color {
        if !viewModel.isLowConfidence { return VecturTheme.green }
        if viewModel.trackingStatusLabel == "Hold steady" { return VecturTheme.amber }
        return VecturTheme.red
    }

    private func formatETA(_ seconds: Double) -> String {
        if seconds < 60 { return "< 1 min" }
        return "~\(Int(ceil(seconds / 60))) min"
    }

    // MARK: - Arrival Overlay

    @State private var arrivalScale: CGFloat = 0.5

    private var arrivalOverlay: some View {
        ZStack(alignment: .topTrailing) {
            VecturBackground()

            VStack(spacing: 20) {
                Spacer()

                Image(systemName: "checkmark.circle.fill")
                    .font(.system(size: 104, weight: .semibold))
                    .foregroundStyle(VecturTheme.green)
                    .scaleEffect(arrivalScale)
                    .onAppear {
                        withAnimation(.spring(response: 0.5, dampingFraction: 0.6)) {
                            arrivalScale = 1.0
                        }
                    }

                VStack(spacing: 8) {
                    Text("Arrival confirmed")
                        .font(.system(size: 11, weight: .bold))
                        .tracking(1.4)
                        .foregroundStyle(VecturTheme.green)
                        .textCase(.uppercase)
                    Text("You've arrived")
                        .font(.system(size: 34, weight: .heavy, design: .rounded))
                        .foregroundStyle(VecturTheme.textPrimary)
                    Text(viewModel.destinationLabel)
                        .font(.title3.weight(.semibold))
                        .foregroundStyle(VecturTheme.textSecondary)
                        .lineLimit(1)
                }

                HStack(spacing: 10) {
                    ArrivalStat(value: "\(String(format: "%.0f", viewModel.totalDistance)) m", label: "Distance")
                    ArrivalStat(value: formatETA(viewModel.totalDistance / 1.2), label: "Time")
                    ArrivalStat(value: "\(viewModel.routeStepCount)", label: viewModel.routeStepCount == 1 ? "Step" : "Steps")
                }

                VecturCard {
                    HStack(spacing: 12) {
                        Image(systemName: "flag.fill")
                            .font(.system(size: 20, weight: .semibold))
                            .foregroundStyle(VecturTheme.amber)
                            .frame(width: 44, height: 44)
                            .background(VecturTheme.amber.opacity(0.16))
                            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                        VStack(alignment: .leading, spacing: 3) {
                            Text(viewModel.destinationLabel)
                                .font(.headline)
                                .foregroundStyle(VecturTheme.textPrimary)
                                .lineLimit(1)
                            Text("Route complete")
                                .font(.subheadline)
                                .foregroundStyle(VecturTheme.textMuted)
                        }
                        Spacer()
                        VecturStatPill(text: "Arrived", color: VecturTheme.green)
                    }
                }

                Spacer()

                Button(action: {
                    viewModel.endNavigation()
                    isPresented = false
                }) {
                    Label("Done", systemImage: "checkmark")
                        .vecturPrimaryButton()
                }
                .buttonStyle(.plain)
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 18)
            .safeAreaPadding(.top)
            .safeAreaPadding(.bottom)

            Button(action: {
                viewModel.endNavigation()
                isPresented = false
            }) {
                Image(systemName: "xmark")
                    .font(.system(size: 17, weight: .bold))
                    .foregroundStyle(VecturTheme.textSecondary)
                    .frame(width: 48, height: 48)
                    .background(VecturTheme.elevated.opacity(0.94))
                    .overlay(Circle().stroke(VecturTheme.borderSubtle, lineWidth: 1))
                    .clipShape(Circle())
            }
            .padding(.top, 10)
            .padding(.trailing, 18)
            .safeAreaPadding(.top)
        }
    }

    // MARK: - Configuration Error Overlay

    private func configErrorOverlay(message: String) -> some View {
        ZStack {
            VecturBackground()

            VecturCard {
                VStack(spacing: 18) {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .font(.system(size: 52, weight: .semibold))
                        .foregroundStyle(VecturTheme.red)

                    Text("Setup needed")
                        .font(.title2.weight(.bold))
                        .foregroundStyle(VecturTheme.textPrimary)

                    Text(message)
                        .font(.subheadline)
                        .foregroundStyle(VecturTheme.textSecondary)
                        .multilineTextAlignment(.center)

                    Text("The entrance sign image needs to be added to this app.")
                        .font(.caption)
                        .foregroundStyle(VecturTheme.textMuted)
                        .multilineTextAlignment(.center)

                    Button(action: {
                        viewModel.endNavigation()
                        isPresented = false
                    }) {
                        Label("Go Back", systemImage: "arrow.left")
                            .vecturPrimaryButton()
                    }
                    .buttonStyle(.plain)
                }
                .frame(maxWidth: .infinity)
            }
            .padding(.horizontal, 24)
        }
    }

}

private struct ProgressRing: View {
    let progress: Double
    let etaText: String
    let distanceText: String

    var body: some View {
        ZStack {
            Circle()
                .stroke(VecturTheme.borderStrong, lineWidth: 4)
            Circle()
                .trim(from: 0, to: CGFloat(progress).clamped(to: 0...1))
                .stroke(VecturTheme.cyan, style: StrokeStyle(lineWidth: 4, lineCap: .round))
                .rotationEffect(.degrees(-90))
            VStack(spacing: 1) {
                Text(etaText)
                    .font(.caption.weight(.bold))
                    .foregroundStyle(VecturTheme.textPrimary)
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
                Text(distanceText)
                    .font(.system(size: 10, weight: .semibold))
                    .foregroundStyle(VecturTheme.textMuted)
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
            }
        }
        .frame(width: 78, height: 78)
    }
}

private struct CompassStrip: View {
    let progress: Double

    var body: some View {
        Canvas { context, size in
            let centerY = size.height / 2
            let offset = CGFloat(progress) * 42

            for index in -6...6 {
                let x = size.width / 2 + CGFloat(index) * 28 - offset
                let active = index == 0
                var path = Path()
                path.move(to: CGPoint(x: x, y: centerY - 5))
                path.addLine(to: CGPoint(x: x, y: centerY + 5))
                context.stroke(
                    path,
                    with: .color(active ? VecturTheme.cyan : VecturTheme.textDisabled),
                    style: StrokeStyle(lineWidth: active ? 2 : 1, lineCap: .round)
                )
            }

            context.fill(
                Path(ellipseIn: CGRect(x: size.width / 2 - 3, y: centerY - 3, width: 6, height: 6)),
                with: .color(VecturTheme.cyan)
            )
        }
        .frame(height: 24)
        .background(VecturTheme.card.opacity(0.58))
        .overlay(Capsule().stroke(VecturTheme.borderSubtle.opacity(0.7), lineWidth: 1))
        .clipShape(Capsule())
    }
}

private struct RadarSweep: View {
    @State private var rotation: Double = 0

    var body: some View {
        Canvas { context, size in
            let rect = CGRect(origin: .zero, size: size)
            context.fill(Path(ellipseIn: rect), with: .color(VecturTheme.cyan.opacity(0.12)))
            context.stroke(Path(ellipseIn: rect.insetBy(dx: 4, dy: 4)), with: .color(VecturTheme.cyan.opacity(0.42)), lineWidth: 2)

            var arc = Path()
            arc.addArc(
                center: CGPoint(x: size.width / 2, y: size.height / 2),
                radius: min(size.width, size.height) / 2 - 4,
                startAngle: .degrees(rotation),
                endAngle: .degrees(rotation + 72),
                clockwise: false
            )
            context.stroke(arc, with: .color(VecturTheme.cyan), style: StrokeStyle(lineWidth: 4, lineCap: .round))
        }
        .onAppear {
            withAnimation(.linear(duration: 2.0).repeatForever(autoreverses: false)) {
                rotation = 360
            }
        }
    }
}

private struct AlignmentMiniIllustration: View {
    @State private var phase: CGFloat = 0

    var body: some View {
        Canvas { context, size in
            let phoneX = size.width * 0.2 + sin(phase * CGFloat.pi * 2) * 14
            let posterX = size.width * 0.7

            context.fill(
                Path(roundedRect: CGRect(x: posterX, y: 16, width: 54, height: 44), cornerRadius: 8),
                with: .color(VecturTheme.borderStrong)
            )
            context.fill(
                Path(roundedRect: CGRect(x: posterX + 8, y: 24, width: 38, height: 28), cornerRadius: 6),
                with: .linearGradient(
                    Gradient(colors: [VecturTheme.blue, VecturTheme.cyan]),
                    startPoint: CGPoint(x: posterX + 8, y: 24),
                    endPoint: CGPoint(x: posterX + 46, y: 52)
                )
            )
            context.fill(
                Path(roundedRect: CGRect(x: phoneX, y: 22, width: 34, height: 46), cornerRadius: 10),
                with: .color(VecturTheme.overlay)
            )
            context.stroke(
                Path(ellipseIn: CGRect(x: phoneX - 11, y: 17, width: 56, height: 56)),
                with: .color(VecturTheme.cyan.opacity(0.28)),
                lineWidth: 2
            )
        }
        .frame(height: 76)
        .background(VecturTheme.elevated.opacity(0.72))
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .onAppear {
            withAnimation(.easeInOut(duration: 2.4).repeatForever(autoreverses: false)) {
                phase = 1
            }
        }
    }
}

private struct ArrivalStat: View {
    let value: String
    let label: String

    var body: some View {
        VStack(spacing: 4) {
            Text(value)
                .font(.headline.weight(.bold))
                .foregroundStyle(VecturTheme.textPrimary)
                .lineLimit(1)
                .minimumScaleFactor(0.65)
            Text(label)
                .font(.caption.weight(.semibold))
                .foregroundStyle(VecturTheme.textMuted)
                .lineLimit(1)
        }
        .frame(maxWidth: .infinity, minHeight: 72)
        .background(VecturTheme.card)
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .stroke(VecturTheme.borderSubtle, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}

private extension CGFloat {
    func clamped(to range: ClosedRange<CGFloat>) -> CGFloat {
        Swift.min(Swift.max(self, range.lowerBound), range.upperBound)
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
    @Published var totalDistance: Double = 0.0
    @Published var routeStepCount: Int = 1
    @Published var isLowConfidence: Bool = false
    @Published var stateColor: Color = .orange
    @Published var alignmentTimedOut: Bool = false
    @Published var markerAssetError: String?

    // Phase 11: Next-action guidance
    @Published var nextActionIcon: String = "arrow.up"
    @Published var nextActionText: String = "Follow the path"
    @Published var nextActionDistance: Double? = nil

    // Phase 11: Tracking status badge
    @Published var trackingStatusLabel: String = "Tracking"
    @Published var trackingStatusIcon: String = "location.fill"

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
    /// Tracks the last arrow that triggered a turn haptic, to fire only once per turn.
    private var lastHapticArrowId: String?

    /// Alignment timeout in seconds.
    private let alignmentTimeoutSeconds: TimeInterval = 30.0

    // MARK: - Debug Accessors

    var detectionCandidateCount: Int { markerDetector.totalCandidatesSeen }
    var detectionRejectedCount: Int { markerDetector.rejectedCandidates }
    var expectedMarkerNameForDebug: String { markerDetector.expectedMarkerName ?? "none" }
    private var configuredReferenceImageName: String? {
        guard let name = entranceMarker?.referenceImageName?.trimmingCharacters(in: .whitespacesAndNewlines),
              !name.isEmpty else {
            return nil
        }
        return name
    }

    /// Configure with a precomputed route package and entrance marker.
    func configure(with package: BuildingPackageLoader.LoadedPackage, entranceMarker: BuildingPackageLoader.PackageMarker?) {
        self.routePackage = package
        self.entranceMarker = entranceMarker ?? package.entranceMarker

        let renderConfig = package.config.routeRendering
        totalDistance = package.totalDistance
        remainingDistance = package.totalDistance
        distanceToDestination = package.totalDistance
        routeStepCount = max(1, package.arrows.filter { $0.type != .follow }.count)
        self.destinationThreshold = renderConfig.destinationThresholdMeters
        routeRenderer.configureRendering(
            lookaheadDistanceMeters: renderConfig.lookaheadDistanceMeters,
            arrowHeightOffsetMeters: renderConfig.arrowHeightOffsetMeters
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

                // Phase 11: Update tracking status badge
                if !isLimited {
                    self?.trackingStatusLabel = "Tracking"
                    self?.trackingStatusIcon = "location.fill"
                } else {
                    switch reason {
                    case "Move slower":
                        self?.trackingStatusLabel = "Hold steady"
                        self?.trackingStatusIcon = "hand.raised.fill"
                    case "More visual detail needed":
                        self?.trackingStatusLabel = "Look around slowly"
                        self?.trackingStatusIcon = "eye.slash"
                    case "Relocalizing":
                        self?.trackingStatusLabel = "Re-centering..."
                        self?.trackingStatusIcon = "arrow.triangle.2.circlepath"
                        if self?.isAligned == true {
                            HapticManager.shared.recentering()
                        }
                    default:
                        self?.trackingStatusLabel = reason
                        self?.trackingStatusIcon = "location.slash"
                    }
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
            markerName: configuredReferenceImageName,
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
        alignmentTimedOut = false
        markerAssetError = nil

        let marker = self.entranceMarker

        // Pre-start validation: verify asset chain
        print("[ARNav] --- Pre-Start Validation ---")
        print("[ARNav] Entrance marker from package: id=\(marker?.id ?? "nil"), refImage=\(marker?.referenceImageName ?? "nil"), width=\(marker?.physicalWidthMeters ?? -1)m")

        guard let markerImageName = configuredReferenceImageName else {
            print("[ARNav] No reference image configured; starting markerless world tracking")
            sessionManager.startWorldTrackingSession(arView: arView)
            alignToCurrentPosition()
            return
        }

        sessionStateLabel = "Waiting for Poster"
        stateColor = .orange
        let markerWidth = marker?.physicalWidthMeters ?? 0.21

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
        markerAssetError = nil
        markerDetector.reset()

        // Restart the session to reset tracking
        guard let arView = arView else { return }
        let marker = self.entranceMarker

        guard let markerImageName = configuredReferenceImageName else {
            sessionManager.startWorldTrackingSession(arView: arView)
            alignToCurrentPosition()
            return
        }

        sessionStateLabel = "Waiting for Poster"
        stateColor = .orange
        let markerWidth = marker?.physicalWidthMeters ?? 0.21

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

    private func alignToCurrentPosition() {
        alignmentTimeoutTimer?.invalidate()
        alignmentTimedOut = false
        isSimulated = false
        isAligned = true
        sessionStateLabel = "Navigating"
        stateColor = .blue
        currentInstruction = "Follow the path"
        isLowConfidence = false
        HapticManager.shared.routeStarted()
        lastHapticArrowId = nil

        progress = 0.0
        userCumulativeDistance = 0.0

        guard let pkg = routePackage, let arView = arView else { return }

        let startPoint = pkg.routePoints.first ?? (0.0, 0.0)
        let routeForward = initialRouteForward(from: pkg.routePoints)
        alignmentRotYDeg = markerlessRotationToFaceForward(routeForward: routeForward)

        let rotRad = alignmentRotYDeg * .pi / 180.0
        let cosR = cos(rotRad)
        let sinR = sin(rotRad)
        let rotatedStartX = startPoint.0 * cosR + startPoint.1 * sinR
        let rotatedStartZ = -startPoint.0 * sinR + startPoint.1 * cosR

        let startDistanceInFrontOfCamera = 0.8
        let markerlessGuidanceHeight = 1.15
        alignmentOffsetX = -rotatedStartX
        alignmentOffsetY = markerlessGuidanceHeight
        alignmentOffsetZ = -startDistanceInFrontOfCamera - rotatedStartZ

        routeRenderer.setAlignmentTransform(
            offsetX: alignmentOffsetX,
            offsetY: alignmentOffsetY,
            offsetZ: alignmentOffsetZ,
            rotationYDeg: alignmentRotYDeg
        )

        remainingDistance = pkg.totalDistance
        distanceToDestination = pkg.totalDistance
        routeRenderer.placeAllArrows(in: arView, arrows: pkg.arrows)
        routeRenderer.updateVisibility(userCumulativeDistance: userCumulativeDistance)
        arrowCount = routeRenderer.renderedArrowCount
        updateNextAction()

        print("[ARNav] Markerless alignment locked — route starts \(startDistanceInFrontOfCamera)m ahead, guidance height: \(markerlessGuidanceHeight)m, first segment rotation: \(alignmentRotYDeg)°")
        startPoseUpdates()
    }

    private func initialRouteForward(from routePoints: [(Double, Double)]) -> (x: Double, z: Double) {
        guard routePoints.count >= 2 else { return (0.0, -1.0) }

        for index in 0..<(routePoints.count - 1) {
            let dx = routePoints[index + 1].0 - routePoints[index].0
            let dz = routePoints[index + 1].1 - routePoints[index].1
            let length = sqrt(dx * dx + dz * dz)
            if length > 0.01 {
                return (dx / length, dz / length)
            }
        }

        return (0.0, -1.0)
    }

    private func markerlessRotationToFaceForward(routeForward: (x: Double, z: Double)) -> Double {
        let routeAngle = atan2(routeForward.x, routeForward.z)
        let arForwardAngle = Double.pi
        return (arForwardAngle - routeAngle) * 180.0 / .pi
    }

    func advanceProgress() {
        guard let pkg = routePackage else { return }
        userCumulativeDistance = min(userCumulativeDistance + 2.0, pkg.totalDistance)
        progress = pkg.totalDistance > 0 ? min(userCumulativeDistance / pkg.totalDistance, 1.0) : 1.0
        remainingDistance = max(0, pkg.totalDistance - userCumulativeDistance)

        // Approximate: use remaining route distance as proxy in simulation
        distanceToDestination = remainingDistance

        routeRenderer.updateVisibility(userCumulativeDistance: userCumulativeDistance)
        arrowCount = routeRenderer.renderedArrowCount

        updateNextAction()
        checkArrival(distToDest: distanceToDestination)
    }

    // MARK: - Next-Action Guidance (Phase 11)

    /// Scans the arrow array ahead of the user to find the next turn or destination,
    /// then updates the guidance card text, icon, and distance.
    private func updateNextAction() {
        guard let pkg = routePackage else { return }

        // Find the next non-follow arrow ahead of the user
        let upcoming = pkg.arrows.filter { $0.cumulativeDistance > userCumulativeDistance && $0.type != .follow }

        if let next = upcoming.first {
            let dist = next.cumulativeDistance - userCumulativeDistance
            nextActionDistance = dist

            switch next.type {
            case .turnLeft:
                nextActionIcon = "arrow.turn.up.left"
                nextActionText = dist < 3.0 ? "Turn left now" : "Turn left ahead"
            case .turnRight:
                nextActionIcon = "arrow.turn.up.right"
                nextActionText = dist < 3.0 ? "Turn right now" : "Turn right ahead"
            case .uTurn:
                nextActionIcon = "arrow.uturn.left"
                nextActionText = dist < 3.0 ? "U-turn now" : "U-turn ahead"
            case .destination:
                nextActionIcon = "flag.fill"
                nextActionText = dist < 3.0 ? "You're almost there" : "Continue to destination"
            case .follow:
                break
            }

            // Haptic: fire once when within 2 m of a turn
            if dist < 2.0 && next.id != lastHapticArrowId {
                lastHapticArrowId = next.id
                if next.type == .turnLeft || next.type == .turnRight || next.type == .uTurn {
                    HapticManager.shared.turnImminent()
                }
            }
        } else {
            // No turns or destination ahead — continue straight
            nextActionIcon = "arrow.up"
            nextActionText = "Continue straight"
            nextActionDistance = nil
        }

        // Update instruction for approaching destination
        if distanceToDestination < destinationThreshold * 2 {
            nextActionIcon = "flag.fill"
            nextActionText = "You're almost there"
            stateColor = .green
        }
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
        currentInstruction = "Follow the path"
        isLowConfidence = false
        HapticManager.shared.routeStarted()
        lastHapticArrowId = nil

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

            // Update arrow visibility based on progress, hiding guidance that is behind the camera.
            let forward = self.cameraForward(from: transform)
            self.routeRenderer.updateVisibility(
                userCumulativeDistance: self.userCumulativeDistance,
                cameraWorldX: pos.x,
                cameraWorldZ: pos.z,
                cameraForwardX: forward.x,
                cameraForwardZ: forward.z
            )
            self.arrowCount = self.routeRenderer.renderedArrowCount

            // Phase 11: Update next-action guidance
            self.updateNextAction()

            self.checkArrival(distToDest: destDist)
        }
    }

    private func cameraForward(from transform: Transform) -> SIMD3<Float> {
        let matrix = transform.matrix
        let forward = SIMD3<Float>(
            -matrix.columns.2.x,
            -matrix.columns.2.y,
            -matrix.columns.2.z
        )
        let length = simd_length(forward)
        guard length > 0.001 else { return SIMD3<Float>(0, 0, -1) }
        return forward / length
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
        currentInstruction = "You've reached \(destinationLabel)"
        HapticManager.shared.arrived()

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
