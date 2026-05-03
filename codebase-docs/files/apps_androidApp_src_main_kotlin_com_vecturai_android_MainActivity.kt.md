# File Dossier: MainActivity.kt

## Path
`apps/androidApp/src/main/kotlin/com/Vectura AI/android/MainActivity.kt`

## Type
Authored Source (Android Activity)

## Role
Launcher Activity for the Android app. Hosts only the Home/PackageError Compose surface and launches `ArCameraActivity` for the camera-owned visitor flow.

## Imports / Includes
- `android.content.Intent`
- `androidx.activity.ComponentActivity`
- `androidx.activity.compose.setContent`
- `com.Vectura AI.android.ar.ArCameraActivity`
- `AndroidNavigationFlowModel`
- `AndroidNavigationApp`
- Koin `viewModel`

## Exports / Public Surface
- `MainActivity`

## Logic
- Creates `AndroidNavigationFlowModel` via Koin.
- Calls `setContent` with `AndroidNavigationApp`.
- Home CTA launches `ArCameraActivity` with an explicit `Intent`.

## Uses
- `AndroidNavigationApp`
- `AndroidNavigationFlowModel`
- `ArCameraActivity`

## Used By
- `AndroidManifest.xml`: Defined as the LAUNCHER activity.

## Related Tests
- None.
