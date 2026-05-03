import SwiftUI

@main
struct VecturAIWatchApp: App {
    @StateObject private var store = WatchNavigationStore()

    var body: some Scene {
        WindowGroup {
            WatchNavigationView()
                .environmentObject(store)
        }
    }
}
