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
                            originName: flow.confirmedEntrance.isEmpty ? "Main Entrance" : flow.confirmedEntrance,
                            destinationName: flow.selectedRoom?.displayName ?? "Destination",
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
                RouteMiniStrip(routePoints: flow.routePackage?.routePoints ?? [])

                HStack(alignment: .top, spacing: 12) {
                    routeEndpoint(
                        label: "From",
                        title: flow.confirmedEntrance.isEmpty ? "Main Entrance" : flow.confirmedEntrance,
                        icon: "location.circle.fill",
                        color: VecturTheme.green
                    )

                    Image(systemName: "chevron.right")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(VecturTheme.textDisabled)
                        .padding(.top, 22)

                    routeEndpoint(
                        label: "To",
                        title: flow.selectedRoom?.displayName ?? "Destination",
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

            Text("Point your camera at the entrance sign to begin")
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

private struct RouteMiniStrip: View {
    let routePoints: [(Double, Double)]
    @State private var markerProgress: CGFloat = 0

    var body: some View {
        Canvas { context, size in
            let start = CGPoint(x: 24, y: size.height / 2)
            let end = CGPoint(x: size.width - 24, y: size.height / 2)
            var path = Path()
            path.move(to: start)
            path.addCurve(
                to: end,
                control1: CGPoint(x: size.width * 0.32, y: 10),
                control2: CGPoint(x: size.width * 0.62, y: size.height - 10)
            )

            context.stroke(
                path,
                with: .color(VecturTheme.borderStrong),
                style: StrokeStyle(lineWidth: 3, lineCap: .round, dash: [10, 8])
            )
            context.stroke(
                path,
                with: .linearGradient(
                    Gradient(colors: [VecturTheme.blue, VecturTheme.cyan]),
                    startPoint: .zero,
                    endPoint: CGPoint(x: size.width, y: size.height)
                ),
                style: StrokeStyle(lineWidth: 4, lineCap: .round)
            )

            context.fill(Path(ellipseIn: CGRect(x: start.x - 6, y: start.y - 6, width: 12, height: 12)), with: .color(VecturTheme.green))
            context.fill(Path(ellipseIn: CGRect(x: end.x - 6, y: end.y - 6, width: 12, height: 12)), with: .color(VecturTheme.amber))

            let t = markerProgress
            let markerX = start.x + (end.x - start.x) * t
            let markerY = start.y - sin(t * CGFloat.pi) * 16
            context.fill(Path(ellipseIn: CGRect(x: markerX - 4, y: markerY - 4, width: 8, height: 8)), with: .color(.white))

            if routePoints.count > 1 {
                context.fill(
                    Path(ellipseIn: CGRect(x: size.width / 2 - 2, y: size.height / 2 - 2, width: 4, height: 4)),
                    with: .color(VecturTheme.cyan.opacity(0.45))
                )
            }
        }
        .frame(height: 66)
        .background(VecturTheme.elevated)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .onAppear {
            withAnimation(.easeInOut(duration: 1.8).repeatForever(autoreverses: false)) {
                markerProgress = 1
            }
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
                TimelineStep(number: 1, title: "Start from \(originName)", detail: "Entrance", first: true)
                TimelineStep(number: 2, title: "Follow the highlighted route", detail: "\(distanceText) total")
                TimelineStep(number: 3, title: "Arrive at \(destinationName)", detail: "Destination", last: true)
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
