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
            // Anchors ZStack to full screen including safe areas.
            Color(.systemBackground).ignoresSafeArea()

            switch flow.state {
            case .home:
                HomeView(flow: flow)

            case .qrScan:
                QRScanView(flow: flow)

            case .entranceConfirmed:
                DestinationSelectView(flow: flow)

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
        VStack(spacing: 20) {
            Spacer()

            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 56))
                .foregroundStyle(.orange)
                .symbolRenderingMode(.hierarchical)

            VStack(spacing: 8) {
                Text("Unable to Load Navigation Data")
                    .font(.title3).fontWeight(.semibold)
                Text(message)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
            }

            Button(action: onRetry) {
                Label("Try Again", systemImage: "arrow.clockwise")
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 4)
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.large)
            .padding(.horizontal, 32)

            Spacer()
        }
    }
}

// MARK: - Home View

private struct HomeView: View {
    @ObservedObject var flow: NavigationFlowModel
    var body: some View {
        VStack(spacing: 0) {
            Spacer()

            // App identity
            VStack(spacing: 16) {
                Image(systemName: "mappin.and.ellipse")
                    .font(.system(size: 72))
                    .foregroundStyle(.blue)
                    .symbolRenderingMode(.hierarchical)

                VStack(spacing: 4) {
                    Text("VecturAI")
                        .font(.largeTitle).fontWeight(.bold)
                    Text("Indoor AR Navigation")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
            }

            Spacer()
            Spacer()

            // Primary action
            VStack(spacing: 12) {
                Button(action: { flow.startQRScan() }) {
                    Label("Scan Entrance Code", systemImage: "qrcode.viewfinder")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 4)
                }
                .buttonStyle(.borderedProminent)
                .controlSize(.large)
                .padding(.horizontal, 24)

                Text("Scan the QR code at the building entrance to begin")
                    .font(.footnote)
                    .foregroundStyle(Color(.tertiaryLabel))
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
            }

            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

// MARK: - Entrance Confirmed Bottom Sheet

private struct EntranceConfirmedSheet: View {
    let entranceName: String
    let onContinue: () -> Void

    var body: some View {
        VStack {
            Spacer()

            VStack(spacing: 0) {
                // Drag handle
                Capsule()
                    .fill(Color(.systemGray4))
                    .frame(width: 36, height: 5)
                    .padding(.top, 12)
                    .padding(.bottom, 24)

                Image(systemName: "checkmark.circle.fill")
                    .font(.system(size: 44))
                    .foregroundStyle(.green)
                    .symbolRenderingMode(.hierarchical)
                    .padding(.bottom, 12)

                Text("Entrance Confirmed")
                    .font(.title3).fontWeight(.semibold)
                    .padding(.bottom, 4)

                Text("Starting from \(entranceName)")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .padding(.bottom, 24)

                Button(action: onContinue) {
                    Text("Choose Destination")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 4)
                }
                .buttonStyle(.borderedProminent)
                .controlSize(.large)
                .padding(.horizontal, 24)
                .padding(.bottom, 12)
            }
            .frame(maxWidth: .infinity)
            .background(Color(.systemBackground))
            .clipShape(RoundedRectangle(cornerRadius: 20))
            .shadow(color: .black.opacity(0.1), radius: 16, y: -4)
        }
        .transition(.move(edge: .bottom))
        .ignoresSafeArea(edges: .bottom)
    }
}

#Preview {
    ContentView()
}
