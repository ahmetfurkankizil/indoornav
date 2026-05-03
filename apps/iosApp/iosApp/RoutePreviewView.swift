import SwiftUI

/// Route preview screen shown before AR navigation starts.
struct RoutePreviewView: View {
    @ObservedObject var flow: NavigationFlowModel

    private var distance: Double {
        flow.routePackage?.totalDistance ?? 0
    }

    private var stepCount: Int {
        max(1, flow.routePackage?.arrows.filter { $0.type != .follow }.count ?? 1)
    }

    private var originName: String {
        if let origin = flow.selectedOriginRoom { return origin.displayName }
        return flow.confirmedEntrance.isEmpty ? "Main Entrance" : flow.confirmedEntrance
    }

    private var destinationName: String {
        flow.selectedRoom?.displayName ?? "Destination"
    }

    var body: some View {
        ZStack {
            VecturBackground()

            VStack(spacing: 0) {
                topBar
                    .padding(.horizontal, 20)
                    .padding(.top, 8)

                RouteProgressStrip(activeStep: 2)
                    .padding(.horizontal, 24)
                    .padding(.top, 10)

                ScrollView(showsIndicators: false) {
                    VStack(spacing: 18) {
                        header

                        routeSummaryCard

                        RouteTimelineCard(
                            originName: originName,
                            destinationName: destinationName,
                            distanceText: formatMeters(distance)
                        )
                    }
                    .padding(.horizontal, 24)
                    .padding(.top, 26)
                    .padding(.bottom, 24)
                }

                bottomActions
                    .padding(.horizontal, 24)
                    .padding(.bottom, 18)
            }
            .safeAreaPadding(.top)
            .safeAreaPadding(.bottom)
        }
    }

    private var topBar: some View {
        HStack {
            VecturIconChip(systemName: "chevron.left", action: { flow.goBackToDestinationSelect() })
            Spacer()
            Text("Route Preview")
                .font(.headline)
                .foregroundStyle(VecturTheme.textPrimary)
            Spacer()
            Color.clear.frame(width: 48, height: 48)
        }
    }

    private var header: some View {
        VStack(spacing: 10) {
            VecturStatPill(text: "Walking", color: VecturTheme.green)

            HStack(alignment: .firstTextBaseline, spacing: 8) {
                Image(systemName: "figure.walk")
                    .font(.title2.weight(.semibold))
                    .foregroundStyle(VecturTheme.cyan)
                Text(formatWalkingTime(distance / 1.2))
                    .font(.system(size: 44, weight: .heavy, design: .rounded))
                    .foregroundStyle(VecturTheme.textPrimary)
            }

            Text("\(formatMeters(distance)) - \(stepCount) \(stepCount == 1 ? "step" : "steps") - Ground Floor")
                .font(.subheadline.weight(.medium))
                .foregroundStyle(VecturTheme.textMuted)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
    }

    private var routeSummaryCard: some View {
        VecturCard {
            VStack(spacing: 18) {
                RoutePlanView(
                    nodes: flow.reviewedConfig?.nodes ?? [],
                    edges: flow.reviewedConfig?.edges ?? [],
                    routeNodeIds: flow.routePackage?.routeNodeIds ?? [],
                    routePoints: flow.routePackage?.routePoints ?? []
                )

                HStack(alignment: .top, spacing: 12) {
                    routeEndpoint(
                        label: "From",
                        title: originName,
                        icon: "location.circle.fill",
                        color: VecturTheme.green
                    )

                    Image(systemName: "chevron.right")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(VecturTheme.textDisabled)
                        .padding(.top, 22)

                    routeEndpoint(
                        label: "To",
                        title: destinationName,
                        icon: "mappin.and.ellipse",
                        color: VecturTheme.amber,
                        alignTrailing: true
                    )
                }
            }
        }
    }

    private func routeEndpoint(
        label: String,
        title: String,
        icon: String,
        color: Color,
        alignTrailing: Bool = false
    ) -> some View {
        VStack(alignment: alignTrailing ? .trailing : .leading, spacing: 8) {
            Image(systemName: icon)
                .font(.system(size: 22, weight: .semibold))
                .foregroundStyle(color)
                .frame(width: 44, height: 44)
                .background(color.opacity(0.16))
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))

            VStack(alignment: alignTrailing ? .trailing : .leading, spacing: 3) {
                Text(label)
                    .font(.system(size: 11, weight: .bold))
                    .tracking(1.2)
                    .foregroundStyle(VecturTheme.textMuted)
                    .textCase(.uppercase)
                Text(title)
                    .font(.headline)
                    .foregroundStyle(VecturTheme.textPrimary)
                    .lineLimit(2)
            }
        }
        .frame(maxWidth: .infinity, alignment: alignTrailing ? .trailing : .leading)
    }

    private var bottomActions: some View {
        VStack(spacing: 12) {
            Button(action: { flow.startNavigation() }) {
                Label("Start AR Navigation", systemImage: "arkit")
                    .vecturPrimaryButton()
            }
            .buttonStyle(.plain)

            Text(flow.selectedOriginRoom == nil
                 ? "Point your camera at the entrance sign to begin"
                 : "Start from your selected point")
                .font(.footnote.weight(.medium))
                .foregroundStyle(VecturTheme.textDisabled)
                .multilineTextAlignment(.center)
        }
    }

    private func formatWalkingTime(_ seconds: Double) -> String {
        if seconds < 60 { return "< 1 min" }
        return "~\(Int(ceil(seconds / 60.0))) min"
    }

    private func formatMeters(_ meters: Double) -> String {
        "\(String(format: "%.0f", meters)) m"
    }
}

/// 2D floor-plan minimap shown in the route preview screen. Draws all edges in the loaded
/// nav graph in gray, then the active route in cyan. The origin is marked green, the
/// destination amber. Coordinates are projected from building-local (x, z) into the canvas
/// while preserving aspect ratio.
private struct RoutePlanView: View {
    let nodes: [BuildingPackageLoader.PackageNode]
    let edges: [BuildingPackageLoader.PackageEdge]
    let routeNodeIds: [String]
    let routePoints: [(Double, Double)]
    @State private var pulse: CGFloat = 0

    var body: some View {
        Canvas { context, size in
            guard nodes.count >= 2 else {
                drawEmpty(context: context, size: size)
                return
            }

            let bounds = computeBounds()
            let project = makeProjector(bounds: bounds, size: size, padding: 18)

            // 1. Floor edges (gray)
            for edge in edges {
                guard let from = nodes.first(where: { $0.id == edge.from }),
                      let to = nodes.first(where: { $0.id == edge.to }) else { continue }
                var path = Path()
                path.move(to: project(from.x, from.z))
                path.addLine(to: project(to.x, to.z))
                context.stroke(path,
                               with: .color(VecturTheme.borderStrong),
                               style: StrokeStyle(lineWidth: 1.5, lineCap: .round))
            }

            // 2. Visible destination/facility nodes. Routing helpers stay invisible.
            for node in nodes {
                if BuildingPackageLoader.isNavigationHelperNode(node) { continue }
                let p = project(node.x, node.z)
                let r: CGFloat = 1.6
                context.fill(
                    Path(ellipseIn: CGRect(x: p.x - r, y: p.y - r, width: r * 2, height: r * 2)),
                    with: .color(VecturTheme.borderSubtle.opacity(0.9))
                )
            }

            // 3. Active route polyline (cyan)
            if routeNodeIds.count >= 2 {
                let routeNodes = routeNodeIds.compactMap { id in nodes.first { $0.id == id } }
                guard routeNodes.count >= 2 else {
                    drawEmpty(context: context, size: size)
                    return
                }

                var path = Path()
                path.move(to: project(routeNodes[0].x, routeNodes[0].z))
                for n in routeNodes.dropFirst() {
                    path.addLine(to: project(n.x, n.z))
                }
                context.stroke(path,
                               with: .color(VecturTheme.cyan.opacity(0.95)),
                               style: StrokeStyle(lineWidth: 4, lineCap: .round, lineJoin: .round))

                // 4. Origin (green) and destination (amber) markers.
                let originPt = project(routeNodes.first!.x, routeNodes.first!.z)
                let destPt = project(routeNodes.last!.x, routeNodes.last!.z)

                let originRadius: CGFloat = 7
                context.fill(
                    Path(ellipseIn: CGRect(
                        x: originPt.x - originRadius, y: originPt.y - originRadius,
                        width: originRadius * 2, height: originRadius * 2
                    )),
                    with: .color(VecturTheme.green)
                )

                // Pulsing ring around the destination marker.
                let pulseRadius: CGFloat = 8 + pulse * 6
                context.stroke(
                    Path(ellipseIn: CGRect(
                        x: destPt.x - pulseRadius, y: destPt.y - pulseRadius,
                        width: pulseRadius * 2, height: pulseRadius * 2
                    )),
                    with: .color(VecturTheme.amber.opacity(1.0 - Double(pulse))),
                    lineWidth: 2
                )
                let destRadius: CGFloat = 7
                context.fill(
                    Path(ellipseIn: CGRect(
                        x: destPt.x - destRadius, y: destPt.y - destRadius,
                        width: destRadius * 2, height: destRadius * 2
                    )),
                    with: .color(VecturTheme.amber)
                )
            }
        }
        .frame(height: 180)
        .background(VecturTheme.elevated)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .onAppear {
            withAnimation(.easeInOut(duration: 1.6).repeatForever(autoreverses: true)) {
                pulse = 1
            }
        }
    }

    private func drawEmpty(context: GraphicsContext, size: CGSize) {
        let center = CGPoint(x: size.width / 2, y: size.height / 2)
        let r: CGFloat = 4
        context.fill(
            Path(ellipseIn: CGRect(x: center.x - r, y: center.y - r, width: r * 2, height: r * 2)),
            with: .color(VecturTheme.textDisabled)
        )
    }

    private func computeBounds() -> (minX: Double, maxX: Double, minZ: Double, maxZ: Double) {
        let minX = nodes.map(\.x).min() ?? 0
        let maxX = nodes.map(\.x).max() ?? 1
        let minZ = nodes.map(\.z).min() ?? 0
        let maxZ = nodes.map(\.z).max() ?? 1
        return (minX, maxX, minZ, maxZ)
    }

    /// Returns a closure that maps building-local (x, z) into canvas (x, y) space,
    /// preserving aspect ratio and centering the floor plan.
    private func makeProjector(
        bounds: (minX: Double, maxX: Double, minZ: Double, maxZ: Double),
        size: CGSize,
        padding: CGFloat
    ) -> (Double, Double) -> CGPoint {
        let widthRange = max(0.001, bounds.maxX - bounds.minX)
        let heightRange = max(0.001, bounds.maxZ - bounds.minZ)
        let usableW = max(1, size.width - padding * 2)
        let usableH = max(1, size.height - padding * 2)
        let scale = min(usableW / widthRange, usableH / heightRange)
        let centerOffsetX = (usableW - widthRange * scale) / 2
        let centerOffsetY = (usableH - heightRange * scale) / 2
        return { x, z in
            let px = padding + centerOffsetX + (x - bounds.minX) * scale
            // Flip Z so that +z forward (away from entrance) renders downward, matching
            // the Android orientation where the entrance sits near the top of the strip.
            let py = padding + centerOffsetY + (z - bounds.minZ) * scale
            return CGPoint(x: px, y: py)
        }
    }
}

private struct RouteTimelineCard: View {
    let originName: String
    let destinationName: String
    let distanceText: String

    var body: some View {
        VecturCard {
            VStack(alignment: .leading, spacing: 12) {
                VecturSectionHeader(title: "Steps")
                TimelineStep(
                    number: 1,
                    title: "Proceed from \(originName)",
                    detail: "Starting point",
                    first: true
                )
                TimelineStep(
                    number: 2,
                    title: "Follow the highlighted path",
                    detail: "\(distanceText) total"
                )
                TimelineStep(
                    number: 3,
                    title: "Arrive at \(destinationName)",
                    detail: "Destination",
                    last: true
                )
            }
        }
    }
}

private struct TimelineStep: View {
    let number: Int
    let title: String
    let detail: String
    var first = false
    var last = false

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            VStack(spacing: 0) {
                if !first {
                    Rectangle()
                        .fill(VecturTheme.borderStrong)
                        .frame(width: 2, height: 12)
                }
                Text("\(number)")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(VecturTheme.cyan)
                    .frame(width: 28, height: 28)
                    .background(VecturTheme.cyan.opacity(0.15))
                    .overlay(Circle().stroke(VecturTheme.cyan.opacity(0.42), lineWidth: 1))
                    .clipShape(Circle())
                if !last {
                    Rectangle()
                        .fill(VecturTheme.borderStrong)
                        .frame(width: 2, height: 28)
                }
            }

            VStack(alignment: .leading, spacing: 3) {
                Text(title)
                    .font(.headline)
                    .foregroundStyle(VecturTheme.textPrimary)
                    .lineLimit(2)
                Text(detail)
                    .font(.subheadline)
                    .foregroundStyle(VecturTheme.textMuted)
            }
            .padding(.top, first ? 2 : 8)
        }
    }
}

private struct RouteProgressStrip: View {
    let activeStep: Int
    private let steps = ["Entrance", "Destination", "Navigate"]

    var body: some View {
        HStack(spacing: 0) {
            ForEach(steps.indices, id: \.self) { index in
                let active = index <= activeStep
                Text(index < activeStep ? "\(steps[index]) done" : steps[index])
                    .font(.system(size: 10, weight: .bold))
                    .foregroundStyle(active ? VecturTheme.textPrimary : VecturTheme.textMuted)
                    .lineLimit(1)
                    .minimumScaleFactor(0.72)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 5)
                    .background(active ? VecturTheme.blue.opacity(0.22) : VecturTheme.elevated.opacity(0.72))
                    .overlay(Capsule().stroke(active ? VecturTheme.blue.opacity(0.5) : VecturTheme.borderSubtle, lineWidth: 1))
                    .clipShape(Capsule())

                if index != steps.count - 1 {
                    Rectangle()
                        .fill(index < activeStep ? VecturTheme.blue : VecturTheme.borderSubtle)
                        .frame(height: 1)
                }
            }
        }
    }
}
