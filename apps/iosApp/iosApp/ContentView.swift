import SwiftUI

/// Root content view for the VecturAI iOS app.
///
/// Drives the full user flow via NavigationFlowModel:
///   home -> qrScan -> entranceConfirmed -> destinationSelect -> routePreview -> arNavigation
///
/// AR never launches without an explicit destination selection.
struct ContentView: View {
    @StateObject private var flow = NavigationFlowModel()

    var body: some View {
        ZStack {
            VecturBackground()

            switch flow.state {
            case .home:
                HomeView(flow: flow)

            case .qrScan:
                QRScanView(flow: flow)

            case .entranceConfirmed(let entrance):
                EntranceConfirmedView(
                    entranceName: entrance,
                    onContinue: { flow.proceedToDestinationSelect() }
                )

            case .destinationSelect:
                DestinationSelectView(flow: flow)

            case .routePreview:
                RoutePreviewView(flow: flow)

            case .arNavigation:
                if let pkg = flow.routePackage, let room = flow.selectedRoom {
                    ARNavigationView(
                        isPresented: Binding(
                            get: { flow.state == .arNavigation },
                            set: { if !$0 { flow.endNavigation() } }
                        ),
                        destinationName: room.displayName,
                        routePackage: pkg,
                        entranceMarker: flow.validatedEntranceMarker,
                        usesEntranceAlignment: flow.selectedOriginRoom == nil
                    )
                }

            case .packageError(let message):
                PackageErrorView(message: message, onRetry: { flow.retryPackageLoad() })
            }

        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(VecturTheme.canvas)
        .animation(.easeInOut(duration: 0.2), value: flowStateKey)
    }

    private var flowStateKey: String {
        switch flow.state {
        case .home: return "home"
        case .qrScan: return "qrScan"
        case .entranceConfirmed: return "entranceConfirmed"
        case .destinationSelect: return "destinationSelect"
        case .routePreview: return "routePreview"
        case .arNavigation: return "arNavigation"
        case .packageError: return "packageError"
        }
    }
}

// MARK: - Package Error View

private struct PackageErrorView: View {
    let message: String
    let onRetry: () -> Void

    var body: some View {
        ZStack {
            VecturBackground()

            VecturCard {
                VStack(spacing: 18) {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .font(.system(size: 48, weight: .semibold))
                        .foregroundStyle(VecturTheme.amber)

                    Text("Unable to load navigation data")
                        .font(.title2.weight(.bold))
                        .foregroundStyle(VecturTheme.textPrimary)
                        .multilineTextAlignment(.center)

                    Text(message)
                        .font(.subheadline)
                        .foregroundStyle(VecturTheme.textMuted)
                        .multilineTextAlignment(.center)

                    Button(action: onRetry) {
                        Label("Try Again", systemImage: "arrow.clockwise")
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

// MARK: - Home View

private struct HomeView: View {
    @ObservedObject var flow: NavigationFlowModel
    @State private var activePill = 0

    private let pills = ["Live AR", "Smart Routes", "Indoor Maps"]

    var body: some View {
        ZStack {
            VecturBackground()

            VStack(spacing: 0) {
                Spacer(minLength: 48)

                VecturBrandMark(size: 88, pulsing: true)
                    .padding(.bottom, 20)

                Text("VecturAI")
                    .font(.system(size: 42, weight: .heavy, design: .rounded))
                    .foregroundStyle(VecturTheme.primaryGradient)

                Text("Find your way indoors")
                    .font(.title3.weight(.medium))
                    .foregroundStyle(VecturTheme.textMuted)
                    .padding(.top, 4)

                HStack(spacing: 8) {
                    ForEach(pills.indices, id: \.self) { index in
                        FeaturePill(text: pills[index], active: activePill == index)
                    }
                }
                .padding(.top, 40)

                Spacer(minLength: 40)

                Button(action: { flow.startQRScan() }) {
                    Label("Scan Entrance Code", systemImage: "qrcode.viewfinder")
                        .vecturPrimaryButton()
                }
                .buttonStyle(.plain)

                Text("Scan the entrance poster to begin")
                    .font(.footnote.weight(.medium))
                    .foregroundStyle(VecturTheme.textDisabled)
                    .multilineTextAlignment(.center)
                    .padding(.top, 14)

                DemoBuildingStatus()
                    .padding(.top, 12)

                Spacer(minLength: 34)
            }
            .padding(.horizontal, 24)
            .safeAreaPadding(.top)
            .safeAreaPadding(.bottom)
        }
        .onAppear {
            activePill = 0
        }
        .task {
            while !Task.isCancelled {
                try? await Task.sleep(nanoseconds: 2_000_000_000)
                withAnimation(.easeInOut(duration: 0.22)) {
                    activePill = (activePill + 1) % pills.count
                }
            }
        }
    }
}

private struct FeaturePill: View {
    let text: String
    let active: Bool

    var body: some View {
        Text(text)
            .font(.caption.weight(.bold))
            .foregroundStyle(active ? VecturTheme.cyan : VecturTheme.textMuted)
            .lineLimit(1)
            .minimumScaleFactor(0.8)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 8)
            .background(active ? VecturTheme.cyan.opacity(0.16) : VecturTheme.elevated.opacity(0.72))
            .overlay(
                Capsule().stroke(active ? VecturTheme.cyan.opacity(0.58) : VecturTheme.borderSubtle, lineWidth: 1)
            )
            .clipShape(Capsule())
    }
}

private struct DemoBuildingStatus: View {
    var body: some View {
        HStack(spacing: 8) {
            Circle()
                .fill(VecturTheme.green)
                .frame(width: 7, height: 7)
            Text("Demo building ready")
                .font(.caption.weight(.semibold))
                .foregroundStyle(VecturTheme.textMuted)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(VecturTheme.elevated.opacity(0.75))
        .overlay(Capsule().stroke(VecturTheme.borderSubtle, lineWidth: 1))
        .clipShape(Capsule())
    }
}

// MARK: - Entrance Confirmed View

private struct EntranceConfirmedView: View {
    let entranceName: String
    let onContinue: () -> Void
    @State private var appeared = false

    var body: some View {
        ZStack {
            VecturBackground()

            VStack(spacing: 0) {
                Spacer(minLength: 40)

                VStack(spacing: 22) {
                    ZStack {
                        Circle()
                            .fill(VecturTheme.green.opacity(0.14))
                            .frame(width: 132, height: 132)
                        Circle()
                            .stroke(VecturTheme.green.opacity(0.34), lineWidth: 1)
                            .frame(width: 132, height: 132)
                        Image(systemName: "checkmark")
                            .font(.system(size: 56, weight: .heavy))
                            .foregroundStyle(.white)
                            .frame(width: 78, height: 78)
                            .background(VecturTheme.green)
                            .clipShape(Circle())
                    }
                    .scaleEffect(appeared ? 1 : 0.72)
                    .opacity(appeared ? 1 : 0)

                    VStack(spacing: 5) {
                        Text("Entrance confirmed")
                            .font(.system(size: 34, weight: .heavy, design: .rounded))
                            .foregroundStyle(VecturTheme.textPrimary)
                        Text("Starting from \(entranceName)")
                            .font(.title3.weight(.semibold))
                            .foregroundStyle(VecturTheme.textMuted)
                            .multilineTextAlignment(.center)
                    }
                    .opacity(appeared ? 1 : 0)
                    .offset(y: appeared ? 0 : 10)

                    VecturCard {
                        HStack(spacing: 12) {
                            Image(systemName: "location.circle.fill")
                                .font(.system(size: 22, weight: .semibold))
                                .foregroundStyle(VecturTheme.green)
                                .frame(width: 46, height: 46)
                                .background(VecturTheme.green.opacity(0.16))
                                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))

                            VStack(alignment: .leading, spacing: 4) {
                                Text("Entrance")
                                    .font(.system(size: 11, weight: .bold))
                                    .tracking(1.2)
                                    .foregroundStyle(VecturTheme.textMuted)
                                    .textCase(.uppercase)
                                Text(entranceName)
                                    .font(.headline)
                                    .foregroundStyle(VecturTheme.textPrimary)
                                    .lineLimit(1)
                            }

                            Spacer()
                            VecturStatPill(text: "Ready", color: VecturTheme.green)
                        }
                    }
                    .opacity(appeared ? 1 : 0)
                }
                .padding(.horizontal, 24)

                Spacer(minLength: 48)

                Button(action: onContinue) {
                    Label("Choose Destination", systemImage: "location.fill")
                        .vecturPrimaryButton()
                }
                .buttonStyle(.plain)
                .padding(.horizontal, 24)

                Text("Next, pick where you want to go.")
                    .font(.footnote.weight(.medium))
                    .foregroundStyle(VecturTheme.textDisabled)
                    .padding(.top, 14)

                Spacer(minLength: 24)
            }
            .safeAreaPadding(.top)
            .safeAreaPadding(.bottom)
        }
        .onAppear {
            withAnimation(.spring(response: 0.48, dampingFraction: 0.72)) {
                appeared = true
            }
        }
    }
}

#Preview {
    ContentView()
}
