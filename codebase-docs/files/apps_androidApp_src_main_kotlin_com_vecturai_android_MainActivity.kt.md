# File Dossier: MainActivity.kt

## Path
`apps/androidApp/src/main/kotlin/com/vecturai/android/MainActivity.kt`

## Type
Authored Source (Android Activity)

## Role
Application entry point for the single-activity Android app. Hosts the Compose navigation flow and obtains Android flow/AR ViewModels from Koin.

## Imports / Includes
- `androidx.activity.ComponentActivity`
- `androidx.activity.compose.setContent`
- `com.vecturai.android.navigation.AndroidNavigationFlowModel`
- `com.vecturai.android.ar.AndroidArNavigationViewModel`
- `com.vecturai.android.ui.AndroidNavigationApp`
- `org.koin.androidx.viewmodel.ext.android.viewModel`

## Exports / Public Surface
- `MainActivity`: Main launcher activity.

## Logic
- Creates `AndroidNavigationFlowModel` and `AndroidArNavigationViewModel` via Koin.
- Calls `setContent` with `AndroidNavigationApp`, making Compose own the visitor flow and AR screen state.

## Uses
- `AndroidNavigationApp`
- `AndroidNavigationFlowModel`
- `AndroidArNavigationViewModel`

## Used By
- `AndroidManifest.xml`: Defined as the LAUNCHER activity.

## Related Tests
- None.
