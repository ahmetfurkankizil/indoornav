# File Dossier: MainActivity.kt

## Path
app/src/main/java/com/example/vecturai/MainActivity.kt

## Type
source

## Role
Main entry point and high-level navigation orchestrator for the application.

## Imports / Includes
- `androidx.navigation.compose.NavHost`
- `com.example.vecturai.ar.CloudAnchorAuthStatus`
- `com.example.vecturai.persistence.GraphRepository`
- `com.example.vecturai.ui.ModePickerScreen`

## Exports / Public Surface
- `MainActivity` (class)

## Main Symbols
- `IndoorNavApp` (Composable): The root UI container.
- `Route` (Enum): Defines the navigation paths (`ModePicker`, `Mapping`, `Navigation`).
- `KEY_CLOUD_ANCHOR_DISCLOSURE`: Preference key for privacy disclosure state.

## Important Logic by Line Range
- L37-47: Initialization of core dependencies (`GraphRepository`, `CloudAnchorAuthStatus`) and preference-backed disclosure state.
- L49-79: `NavHost` configuration mapping routes to screen entrypoints.
- L57-62: Callback for privacy disclosure acceptance, persisted via `SharedPreferences`.

## Uses
- `GraphRepository.kt`
- `CloudAnchorAuthStatus.kt` [Pending Mapping]
- `ModePickerScreen.kt` [Pending Mapping]

## Used By
- Android OS (System Launch)

## Config / Constants / Protocol Details
- Uses `VecturaiTheme` for global styling.
- Privacy disclosure is mandatory for Cloud Anchor features.

## Related Tests
N/A

## Notes / Risks
- Permission handling is delegated to the specific screens (Mapping/Navigation) via `CameraPermissionGate`.
