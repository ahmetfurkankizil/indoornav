import SwiftUI

/// Minimal route preview screen shown before AR navigation starts.
/// Displays destination, start point, approximate distance, and a
/// prominent "Start Navigation" button.
struct RoutePreviewView: View {
    @ObservedObject var flow: NavigationFlowModel

    var body: some View {
        VStack(spacing: 0) {
            // Header
            HStack {
                Button(action: { flow.goBackToDestinationSelect() }) {
                    Image(systemName: "chevron.left")
                        .font(.title3)
                        .foregroundStyle(.blue)
                }
                Spacer()
                Text("Route Preview")
                    .font(.headline)
                Spacer()
                Image(systemName: "chevron.left").font(.title3).hidden()
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)

            Spacer()

            // Route info card
            VStack(spacing: 20) {
                Image(systemName: "map")
                    .font(.system(size: 48))
                    .foregroundStyle(.blue)

                // Start → Destination
                VStack(spacing: 12) {
                    routeRow(
                        icon: "location.circle.fill",
                        color: .green,
                        label: "Start",
                        value: flow.confirmedEntrance
                    )

                    // Dotted connector
                    Rectangle()
                        .fill(.secondary.opacity(0.3))
                        .frame(width: 2, height: 24)
                        .padding(.leading, 12)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.leading, 40)

                    routeRow(
                        icon: "flag.checkered",
                        color: .red,
                        label: "Destination",
                        value: flow.selectedRoom?.displayName ?? ""
                    )
                }
                .padding(.horizontal, 24)

                // Distance
                if let distance = flow.routePackage?.totalDistance, distance > 0 {
                    HStack(spacing: 6) {
                        Image(systemName: "ruler")
                            .foregroundStyle(.secondary)
                        Text("~\(String(format: "%.0f", distance))m")
                            .font(.title3)
                            .fontWeight(.medium)
                    }
                    .padding(.top, 4)
                }
            }
            .padding(28)
            .background(Color(.systemGray6))
            .clipShape(RoundedRectangle(cornerRadius: 20))
            .padding(.horizontal, 24)

            Spacer()

            // Start Navigation button
            Button(action: { flow.startNavigation() }) {
                Label("Start Navigation", systemImage: "arkit")
                    .font(.headline)
                    .padding()
                    .frame(maxWidth: .infinity)
                    .background(.blue)
                    .foregroundColor(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 14))
            }
            .padding(.horizontal, 32)
            .padding(.bottom, 40)
        }
    }

    private func routeRow(icon: String, color: Color, label: String, value: String) -> some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .foregroundStyle(color)
                .font(.title3)
                .frame(width: 28)
            VStack(alignment: .leading, spacing: 2) {
                Text(label)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Text(value)
                    .font(.body)
                    .fontWeight(.medium)
            }
            Spacer()
        }
    }
}
