# Feature: UI Components

## Purpose
Modular and reusable UI elements that drive the application's user interface and navigation flow.

## Implemented In
- `app/src/main/java/com/example/vecturai/MainActivity.kt`
- `app/src/main/java/com/example/vecturai/ui/ModePickerScreen.kt` [Pending Mapping]
- `app/src/main/java/com/example/vecturai/ui/CameraPermissionGate.kt` [Pending Mapping]

## Used By
- All Subsystems

## Main Flow
1. **Entry:** `MainActivity` hosts the `NavHost`.
2. **App Start:** `ModePickerScreen` prompts for permission disclosure and mode selection.
3. **Guardrails:** `CameraPermissionGate` ensures the camera is available before AR initialization.
4. **Transition:** Navigates to Mapping or Navigation routes based on user input.

## Key Symbols
- `Route` (Enum)
- `IndoorNavApp` (Composable)

## Config / Env / Flags
- `cloud_anchor_disclosure_accepted`: SharedPreferences flag for GDPR/Privacy compliance.

## Data Structures / Protocols
- Compose Navigation (`NavController`)

## Related Tests
N/A

## Related File Dossiers
- [MainActivity.kt](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-document/files/app_src_main_java_com_example_vecturai_MainActivity.kt.md)

## Risks / Notes
- The app uses a "Mode Selection" pattern rather than a unified map view to keep operations distinct and manageable.
