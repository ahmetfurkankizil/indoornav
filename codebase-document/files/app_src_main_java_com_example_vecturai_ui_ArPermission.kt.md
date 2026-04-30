# File Dossier: ArPermission.kt

## Path
app/src/main/java/com/example/vecturai/ui/ArPermission.kt

## Type
source

## Role
Jetpack Compose permission handler for AR camera access.

## Imports / Includes
- `androidx.activity.compose.rememberLauncherForActivityResult`
- `androidx.activity.result.contract.ActivityResultContracts`

## Exports / Public Surface
- `CameraPermissionGate` (Composable)

## Main Symbols
- `CameraPermissionGate`: Wraps content and displays a grant request UI if camera access is missing.

## Important Logic by Line Range
- L31-41: State-backed permission check and result launcher.
- L43-45: `LaunchedEffect` to trigger the permission dialog on first appearance if not already granted.
- L50-67: Fallback UI shown when permission is denied or pending.

## Uses
- Android Permissions (`Manifest.permission.CAMERA`)

## Used By
- `MappingScreen.kt`
- `NavigationScreen.kt`

## Config / Constants / Protocol Details
N/A

## Related Tests
N/A

## Notes / Risks
- Does not handle "Don't ask again" state with a direct link to system settings.
