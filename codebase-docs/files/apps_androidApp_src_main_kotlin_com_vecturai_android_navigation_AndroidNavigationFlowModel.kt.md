# File Dossier: AndroidNavigationFlowModel.kt

## Path
`apps/androidApp/src/main/kotlin/com/vecturai/android/navigation/AndroidNavigationFlowModel.kt`

## Type
Authored Source (Android Navigation Flow State)

## Role
Android ViewModel that mirrors the iOS visitor navigation flow: home, QR scan, entrance confirmation, destination selection, route preview, AR navigation, and package error.

## Imports / Includes
- `androidx.lifecycle.ViewModel`
- `com.vecturai.android.data.AndroidReviewedPackageLoader`
- `com.vecturai.android.qr.QRPayload`
- Kotlin `StateFlow` / `MutableStateFlow`

## Exports / Public Surface
- `AndroidNavigationFlowModel`
- `FlowState`
- `SessionData`
- `state`
- `session`
- `qrError`
- `availableRooms`

## Main Symbols
- `loadPackage()` / `retryPackageLoad()`: Loads bundled reviewed package and transitions to home or package error.
- `startQRScan()` / `onQRScanned(rawValue)`: Starts scanner and validates QR payload.
- `confirmEntrance(...)`: Stores the validated entrance marker and moves to entrance confirmation.
- `proceedToDestinationSelect()`, `selectDestination(room)`, `startNavigation()`: Advance the visitor flow.
- `endNavigation()` / `goBackToDestinationSelect()`: Reset or backtrack session state.

## Important Logic
- QR payloads are validated against the loaded manifest building id and entrance marker ids.
- Destination selection computes a route package immediately so route preview and AR navigation share the same data.
- Ending navigation clears visitor-specific session state and returns to home.

## Uses
- `AndroidReviewedPackageLoader`
- `QRPayload`

## Used By
- `MainActivity.kt`
- `AndroidNavigationApp.kt`
- `QRScanScreen.kt`
- `ArNavigationScreen.kt`

## Related Tests
- None.
