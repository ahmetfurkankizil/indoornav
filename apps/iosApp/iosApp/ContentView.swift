import SwiftUI

/// Root content view for the VecturAI iOS app.
///
/// Drives the full user flow via NavigationFlowModel:
///   home → qrScan → entranceConfirmed → destinationSelect → routePreview → arNavigation
///
/// AR never launches without an explicit destination selection.
struct ContentView: View {
    @StateObject private var flow = NavigationFlowModel()

    var body: some View {
        ZStack {
            switch flow.state {
            case .home:
                HomeView(flow: flow)

            case .qrScan:
                QRScanView(flow: flow)

            case .entranceConfirmed:
                // Bottom sheet is presented over destination select;
                // we transition to destinationSelect immediately after
                // the sheet is dismissed.
                DestinationSelectView(flow: flow)
                    .onAppear {
                        // entranceConfirmed shows a brief sheet then moves on
                    }

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
                        entranceMarker: flow.validatedEntranceMarker
                    )
                    .ignoresSafeArea()
                }

            case .packageError(let message):
                PackageErrorView(message: message, onRetry: { flow.retryPackageLoad() })
            }

            // Entrance confirmed bottom sheet
            if case .entranceConfirmed(let entrance) = flow.state {
                EntranceConfirmedSheet(
                    entranceName: entrance,
                    onContinue: { flow.proceedToDestinationSelect() }
                )
            }
        }
        .animation(.easeInOut(duration: 0.25), value: flowStateKey)
    }

    /// Stable key for animation grouping.
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
        VStack(spacing: 24) {
            Spacer()

            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 64))
                .foregroundStyle(.orange)

            Text("Navigation Data Unavailable")
                .font(.title2)
                .fontWeight(.bold)

            Text(message)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)

            Button(action: onRetry) {
                Label("Retry", systemImage: "arrow.clockwise")
                    .font(.headline)
                    .padding()
                    .frame(maxWidth: .infinity)
                    .background(.blue)
                    .foregroundColor(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            .padding(.horizontal, 32)

            Spacer()
        }
    }
}

// MARK: - Home View (Dashboard)

private struct HomeView: View {
    @ObservedObject var flow: NavigationFlowModel

    var body: some View {
        VStack(spacing: 24) {
            Spacer()

            Image(systemName: "location.fill.viewfinder")
                .font(.system(size: 72))
                .foregroundStyle(.blue)

            Text("VecturAI")
                .font(.largeTitle)
                .fontWeight(.bold)

            Text("Indoor Navigation")
                .font(.title2)
                .foregroundStyle(.secondary)

            Spacer()

            VStack(spacing: 8) {
                Text("Navigate indoors with AR-guided directions")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)

                Text("Scan a QR code at the building entrance to begin")
                    .font(.caption)
                    .foregroundStyle(.tertiary)
            }
            .padding(.horizontal, 32)

            Spacer()

            Button(action: { flow.startQRScan() }) {
                Label("Scan QR Code", systemImage: "qrcode.viewfinder")
                    .font(.headline)
                    .padding()
                    .frame(maxWidth: .infinity)
                    .background(.blue)
                    .foregroundColor(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            .padding(.horizontal, 32)
            .padding(.bottom, 40)
        }
    }
}

// MARK: - Entrance Confirmed Bottom Sheet

private struct EntranceConfirmedSheet: View {
    let entranceName: String
    let onContinue: () -> Void

    var body: some View {
        VStack {
            Spacer()

            VStack(spacing: 16) {
                Capsule()
                    .fill(Color(.systemGray4))
                    .frame(width: 36, height: 5)
                    .padding(.top, 8)

                Image(systemName: "checkmark.circle.fill")
                    .font(.system(size: 40))
                    .foregroundStyle(.green)

                Text("Starting from \(entranceName)")
                    .font(.title3)
                    .fontWeight(.semibold)

                Text("Choose your destination to begin navigation")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)

                Button(action: onContinue) {
                    Text("Continue")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(.blue)
                        .foregroundColor(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                .padding(.horizontal, 24)
                .padding(.bottom, 20)
            }
            .frame(maxWidth: .infinity)
            .background(
                RoundedRectangle(cornerRadius: 24)
                    .fill(Color(.systemBackground))
                    .shadow(color: .black.opacity(0.15), radius: 20, y: -5)
            )
        }
        .transition(.move(edge: .bottom))
        .ignoresSafeArea(edges: .bottom)
    }
}

// MARK: - Preview
// NOTE: This preview instantiates NavigationFlowModel which loads the reviewed
// package from the bundle. It only shows the Home screen (no AR, no camera).
// Do NOT add previews for ARNavigationView or QRScanView — they require
// hardware (ARKit/camera) that will fail or consume excessive resources in
// the preview canvas. If you need to preview AR-related views, use the
// simulator with a "Simulate" button instead.
#Preview {
    ContentView()
}
