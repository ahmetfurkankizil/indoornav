# Apple Watch Companion Setup

This repo includes the code-side implementation for an Apple Watch navigation companion.
The iPhone remains the source of truth for QR scanning, ARKit alignment, routing, and
progress. The Watch receives compact navigation snapshots over WatchConnectivity.

## What Is Implemented

iOS target:

- `WatchNavigationPayload.swift` - shared payload and message keys.
- `WatchNavigationBridge.swift` - `WCSession` sender/receiver for iPhone.
- `ARNavigationViewModel` now sends Watch snapshots for route start, next action,
  progress, tracking confidence, arrival, and route end.
- `HapticManager` mirrors key navigation haptics to the Watch companion when reachable.

Watch source templates:

- `apps/watchApp/VecturAIWatch/VecturAIWatchApp.swift`
- `apps/watchApp/VecturAIWatch/WatchNavigationView.swift`
- `apps/watchApp/VecturAIWatch/WatchNavigationStore.swift`
- `apps/watchApp/VecturAIWatch/WatchNavigationPayload.swift`

## Xcode Target Work

1. Open `apps/iosApp/iosApp.xcodeproj` in Xcode.
2. Add a new target:
   - File > New > Target
   - watchOS > App
   - Choose a companion Watch app for the existing iOS app if Xcode offers that option.
   - Product name suggestion: `VecturAIWatch`.
3. Use bundle identifiers that match your team setup, for example:
   - iOS: `com.vecturai.ios`
   - Watch: `com.vecturai.ios.watchkitapp`
4. In the new Watch target, replace the generated Swift files with the four files in
   `apps/watchApp/VecturAIWatch/`.
5. Make sure those four files are members of the Watch target only.
6. Keep the iOS files `WatchNavigationPayload.swift` and `WatchNavigationBridge.swift`
   in the iOS app target.
7. Build using an `iPhone + Apple Watch` run destination.

## Expected Runtime Flow

1. Launch VecturAI on iPhone.
2. Launch the Watch companion once so WatchConnectivity activates.
3. On iPhone: scan QR, choose destination, start AR navigation.
4. Watch should update to:
   - current next action
   - next-action distance
   - remaining distance
   - ETA
   - tracking status
5. Watch should play haptics for:
   - route start
   - imminent turn
   - recentering
   - arrival
6. The Watch `End` button sends an end-route command back to the iPhone.

## Notes And Limitations

- The Watch does not do AR, QR scanning, or independent indoor positioning.
- `WKInterfaceDevice.current().play(...)` haptics are most reliable while the Watch app
  is active.
- Progress updates are throttled on iPhone to avoid flooding WatchConnectivity.
- No App Group is required for this MVP because state is transferred through
  WatchConnectivity messages and application context.
- If the Watch does not update, open both apps once, then restart the route. Xcode/Watch
  pairing can be slow to activate after installing a fresh Watch target.

## Manual Smoke Test

- Start route on iPhone: Watch changes from idle to navigation HUD.
- Simulate or walk progress: ETA and remaining meters update.
- Get near a turn: Watch receives a haptic.
- Arrive: Watch shows arrived state and plays success haptic.
- Tap `End` on Watch: iPhone returns from AR navigation.
