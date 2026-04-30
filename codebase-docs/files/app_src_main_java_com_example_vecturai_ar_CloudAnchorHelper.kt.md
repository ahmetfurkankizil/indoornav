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
- `resolveAnchor`: Wraps `resolveCloudAnchorAsync` in a coroutine with timeout handling and `ResolveCloudAnchorFuture` cancellation.
- `assertCloudAnchorModeEnabled`: Defensive check to ensure Cloud Anchor mode is active.

## Important Logic by Line Range
- L20-56: `hostAnchor` implementation.
- L58-103: `resolveAnchor` implementation with `withTimeout` and future cancellation.

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
