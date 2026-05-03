import SwiftUI

struct WatchNavigationView: View {
    @EnvironmentObject private var store: WatchNavigationStore

    var body: some View {
        ZStack {
            Color(red: 0.027, green: 0.051, blue: 0.094)
                .ignoresSafeArea()

            if store.payload.hasArrived {
                arrivalView
            } else if store.payload.isActive {
                activeView
            } else {
                idleView
            }
        }
    }

    private var activeView: some View {
        VStack(spacing: 10) {
            HStack(spacing: 8) {
                Image(systemName: store.payload.nextActionIcon)
                    .font(.system(size: 22, weight: .bold))
                    .foregroundStyle(.cyan)
                    .frame(width: 40, height: 40)
                    .background(.cyan.opacity(0.16))
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

                VStack(alignment: .leading, spacing: 2) {
                    Text(store.payload.nextActionText)
                        .font(.headline)
                        .foregroundStyle(.white)
                        .lineLimit(2)
                    if let distance = store.payload.nextActionDistanceMeters {
                        Text("in \(formatMeters(distance))")
                            .font(.caption.weight(.bold))
                            .foregroundStyle(.cyan)
                    }
                }
            }

            ProgressView(value: store.payload.progress)
                .tint(.cyan)

            HStack {
                stat(value: formatETA(store.payload.etaSeconds), label: "ETA")
                stat(value: formatMeters(store.payload.remainingDistanceMeters), label: "Left")
            }

            Text(store.payload.destinationName)
                .font(.caption.weight(.semibold))
                .foregroundStyle(.white.opacity(0.66))
                .lineLimit(1)

            if store.payload.isLowConfidence {
                Text(store.payload.trackingStatus)
                    .font(.caption2.weight(.bold))
                    .foregroundStyle(.orange)
                    .lineLimit(1)
            }

            Button("End") {
                store.endRouteOnPhone()
            }
            .buttonStyle(.bordered)
            .tint(.red)
        }
        .padding(.horizontal, 6)
    }

    private var arrivalView: some View {
        VStack(spacing: 10) {
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 46, weight: .bold))
                .foregroundStyle(.green)
            Text("Arrived")
                .font(.title2.weight(.bold))
                .foregroundStyle(.white)
            Text(store.payload.destinationName)
                .font(.caption.weight(.semibold))
                .foregroundStyle(.white.opacity(0.7))
                .lineLimit(2)
        }
        .multilineTextAlignment(.center)
    }

    private var idleView: some View {
        VStack(spacing: 12) {
            Image(systemName: "iphone")
                .font(.system(size: 42, weight: .semibold))
                .foregroundStyle(.cyan)
            Text("VecturAI")
                .font(.headline)
                .foregroundStyle(.white)
            Text(store.connectionStatus)
                .font(.caption)
                .foregroundStyle(.white.opacity(0.62))
                .multilineTextAlignment(.center)
        }
    }

    private func stat(value: String, label: String) -> some View {
        VStack(spacing: 2) {
            Text(value)
                .font(.caption.weight(.bold))
                .foregroundStyle(.white)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
            Text(label)
                .font(.caption2.weight(.semibold))
                .foregroundStyle(.white.opacity(0.52))
        }
        .frame(maxWidth: .infinity)
    }

    private func formatMeters(_ meters: Double) -> String {
        "\(Int(max(0, meters).rounded())) m"
    }

    private func formatETA(_ seconds: Double) -> String {
        if seconds < 60 { return "<1m" }
        return "\(Int(ceil(seconds / 60)))m"
    }
}

#Preview {
    WatchNavigationView()
        .environmentObject(WatchNavigationStore())
}
