# File Dossier: ArBridge.kt

## Path
`apps/androidApp/src/main/kotlin/com/Vectura AI/android/ar/ArBridge.kt`

## Type
Authored Source (Platform Adapter)

## Role
Coordinates state between the Android AR UI and the shared KMP domain logic. Acts as a bridge for session lifecycle and navigation events.

## Imports / Includes
- `com.Vectura AI.core.store.AppStore`
- `kotlinx.coroutines.flow.StateFlow`

## Exports / Public Surface
- `ArBridge`: Class for state synchronization.

## Main Symbols
- `currentStateLabel`: Observable UI state label.
- `onAlignmentEstablished(...)`: Callback for AR world alignment.
- `onEntranceMarkerDetected(...)`: Trigger for session initiation.

## Important Logic by Line Range
- **20-40**: State management for UI labels and alignment flags.
- **58-61**: ARCore marker detection callback.

## Uses
- `AppStore`: Observes shared `NavigationState`.

## Used By
- `AndroidModule.kt`: Registered as a Koin singleton.
- Android AR/navigation code as needed through dependency injection.

## Config / Constants / Protocol Details
- Standard labels: "Waiting for Marker", "Navigating", "Arrived".

## Related Tests
- None.

## Notes / Risks
- Currently a lightweight bridge; the modernized Android AR flow primarily uses `AndroidArNavigationViewModel`.
