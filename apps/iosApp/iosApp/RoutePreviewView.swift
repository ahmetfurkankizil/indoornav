import SwiftUI

/// Route preview screen shown before AR navigation starts.
/// Modeled after Apple Maps / Google Maps pre-navigation sheet:
/// origin → destination, walking time prominent, single "Start" CTA.
struct RoutePreviewView: View {
    @ObservedObject var flow: NavigationFlowModel

    var body: some View {
        VStack(spacing: 0) {
            // Navigation bar
            HStack {
                Button(action: { flow.goBackToDestinationSelect() }) {
                    HStack(spacing: 4) {
                        Image(systemName: "chevron.left")
                            .fontWeight(.semibold)
                        Text("Back")
                    }
                    .font(.body)
                    .foregroundStyle(.blue)
                }

                Spacer()

                Text("Route Preview")
                    .font(.headline)

                Spacer()

                Text("Back").font(.body).hidden()
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)

            Divider()

            Spacer()

            // Route card
            VStack(spacing: 0) {
                // Walking time — most prominent
                if let distance = flow.routePackage?.totalDistance, distance > 0 {
                    VStack(spacing: 4) {
                        HStack(alignment: .firstTextBaseline, spacing: 6) {
                            Image(systemName: "figure.walk")
                                .font(.title3)
                                .foregroundStyle(.blue)
                            Text(formatWalkingTime(distance / 1.2))
                                .font(.system(size: 34, weight: .bold, design: .rounded))
                            Text("walking")
                                .font(.title3)
                                .foregroundStyle(.secondary)
                                .padding(.bottom, 2)
                        }
                        Text("~\(String(format: "%.0f", distance)) m")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                    .padding(.bottom, 28)
                }

                // Origin → Destination
                VStack(spacing: 0) {
                    // Origin row
                    HStack(spacing: 12) {
                        ZStack {
                            Circle()
                                .fill(Color.green.opacity(0.15))
                                .frame(width: 32, height: 32)
                            Circle()
                                .fill(Color.green)
                                .frame(width: 10, height: 10)
                        }
                        VStack(alignment: .leading, spacing: 1) {
                            Text("From")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                            Text(flow.confirmedEntrance)
                                .font(.body).fontWeight(.medium)
                        }
                        Spacer()
                    }

                    // Connector line
                    HStack(spacing: 0) {
                        Rectangle()
                            .fill(Color(.separator))
                            .frame(width: 1.5, height: 28)
                            .padding(.leading, 15)
                        Spacer()
                    }

                    // Destination row
                    HStack(spacing: 12) {
                        ZStack {
                            RoundedRectangle(cornerRadius: 6)
                                .fill(Color.blue.opacity(0.15))
                                .frame(width: 32, height: 32)
                            Image(systemName: "mappin")
                                .font(.system(size: 14))
                                .foregroundStyle(.blue)
                        }
                        VStack(alignment: .leading, spacing: 1) {
                            Text("To")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                            Text(flow.selectedRoom?.displayName ?? "")
                                .font(.body).fontWeight(.medium)
                        }
                        Spacer()
                    }
                }
                .padding(.horizontal, 20)
            }
            .padding(.vertical, 28)
            .padding(.horizontal, 16)
            .background(Color(.secondarySystemBackground))
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .padding(.horizontal, 20)

            Spacer()

            // Start AR Navigation
            VStack(spacing: 12) {
                Button(action: { flow.startNavigation() }) {
                    Label("Start AR Navigation", systemImage: "arkit")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 4)
                }
                .buttonStyle(.borderedProminent)
                .controlSize(.large)
                .padding(.horizontal, 24)

                Text("Point your camera at the entrance sign to begin")
                    .font(.footnote)
                    .foregroundStyle(Color(.tertiaryLabel))
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
            }
            .padding(.bottom, 40)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(.systemBackground))
    }

    private func formatWalkingTime(_ seconds: Double) -> String {
        if seconds < 60 { return "< 1 min" }
        let mins = Int(ceil(seconds / 60.0))
        return "~\(mins) min"
    }
}
