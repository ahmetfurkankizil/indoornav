# File Dossier: CloudAnchorHelper.kt

## Path
app/src/main/java/com/example/vecturai/ar/CloudAnchorHelper.kt

## Type
source

## Role
Coroutine-based facade for ARCore Cloud Anchor asynchronous operations.

## Imports / Includes
- `com.google.ar.core.Anchor`
- `com.google.ar.core.Session`
- `kotlinx.coroutines.suspendCancellableCoroutine`

## Exports / Public Surface
- `HostedCloudAnchor` (data class)
- `CloudAnchorHelper` (object)
- `hostAnchor(...)` (suspend function)
- `resolveAnchor(...)` (suspend function)

## Main Symbols
- `hostAnchor`: Wraps `hostCloudAnchorAsync` in a coroutine.
- `resolveAnchor`: Wraps `resolveCloudAnchorAsync` in a coroutine.
- `ensureCloudAnchorModeEnabled`: Defensive check to enable Cloud Anchor mode if it was disabled.

## Important Logic by Line Range
- L20-62: `hostAnchor` implementation. Handles cancellation by canceling the ARCore future and detaching the local anchor.
- L67-96: `resolveAnchor` implementation. Manages async callback and result mapping.

## Uses
- `ArSessionConfig.kt`
- ARCore SDK

## Used By
- `MappingViewModel.kt`
- `NavigationViewModel.kt`

## Config / Constants / Protocol Details
- Default TTL: 1 day.

## Related Tests
N/A

## Notes / Risks
- Returns `Result.failure` on timeout or ARCore session errors.
- Detaches anchors immediately on failure to prevent memory leaks/visual ghosting.
