import SwiftUI

/// Vectura AI iOS App Entry Point
///
/// This SwiftUI app hosts the Compose Multiplatform UI for non-AR screens
/// and provides the native ARKit-based navigation view when AR is triggered.
@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(VecturTheme.canvas)
        }
    }
}
