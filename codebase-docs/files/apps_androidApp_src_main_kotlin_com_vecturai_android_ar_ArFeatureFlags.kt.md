# File Dossier: ArFeatureFlags.kt

## Path
`apps/androidApp/src/main/kotlin/com/vecturai/android/ar/ArFeatureFlags.kt`

## Type
Authored Source (Config)

## Role
Centralized feature flags for Android-specific AR behavior.

## Imports / Includes
None.

## Exports / Public Surface
- `ArFeatureFlags`: Object containing flags.

## Main Symbols
- `ArUnifiedCameraPipeline`: Boolean flag. If true, the QR scanner and AR navigation both use the same ARCore session to avoid camera contention and speed up the transition. Currently set to `false`.

## Important Logic
None.

## Uses
None.

## Used By
- `ArSessionManager.kt`: Checks flag before stopping/starting sessions.
- `QRScanScreen.kt`: Uses flag to choose between `CameraQrPreview` and `ArCoreQrPreview`.

## Config / Constants / Protocol Details
- `ArUnifiedCameraPipeline`: `false` (default).

## Related Tests
None.

## Notes / Risks
- This is a development/experimentation flag to test single-session transitions.
