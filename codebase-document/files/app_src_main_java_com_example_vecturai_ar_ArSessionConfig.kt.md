# File Dossier: ArSessionConfig.kt

## Path
app/src/main/java/com/example/vecturai/ar/ArSessionConfig.kt

## Type
source

## Role
Central configuration for ARCore session parameters.

## Imports / Includes
- `com.google.ar.core.Config`
- `com.google.ar.core.Session`

## Exports / Public Surface
- `ArSessionConfig` (object)
- `configureIndoorCloudSession(session, config)` (function)

## Main Symbols
- `configureIndoorCloudSession`: Sets optimal settings for indoor AR mapping and navigation.

## Important Logic by Line Range
- L8: Enables `CloudAnchorMode.ENABLED`.
- L12-14: Conditional check and activation for `DepthMode.AUTOMATIC`.
- L9-11: Configures `AUTO` focus and `ENVIRONMENTAL_HDR` lighting.

## Uses
- ARCore SDK

## Used By
- `CloudAnchorHelper.kt`
- `MappingViewModel.kt`
- `NavigationViewModel.kt`

## Config / Constants / Protocol Details
- `UpdateMode.LATEST_CAMERA_IMAGE`: Ensures the UI renders the latest available frame.

## Related Tests
N/A

## Notes / Risks
- Must be called before `session.configure(config)` to take effect.
- Depth mode is optional depending on device capability.
