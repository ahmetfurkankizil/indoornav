import SwiftUI

/// Root content view for the VecturAI iOS app.
///
/// This view hosts the Compose Multiplatform UI controller for standard screens
/// and can present the native AR navigation view when triggered.
///
/// TODO: Integrate Compose Multiplatform UIViewController via ComposeUIViewController
/// TODO: Wire navigation state to present ARNavigationView
struct ContentView: View {
    @State private var showARView = false

    var body: some View {
        NavigationStack {
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

                // TODO: Replace with Compose Multiplatform UI
                Text("Compose Multiplatform UI will be hosted here")
                    .font(.caption)
                    .foregroundStyle(.tertiary)
                    .padding()

                Button(action: { showARView = true }) {
                    Label("Start AR Navigation", systemImage: "arkit")
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
            .fullScreenCover(isPresented: $showARView) {
                ARNavigationView(isPresented: $showARView)
            }
        }
    }
}

#Preview {
    ContentView()
}
