# Feature: Cloud Anchor Integration

## Purpose
Enables world-scale persistent AR by hosting local anchors to Google Cloud and resolving them later to localize the device within a saved graph.

## Implemented In
- `app/src/main/java/com/example/vecturai/ar/CloudAnchorHelper.kt`
- `app/src/main/java/com/example/vecturai/ar/ArSessionConfig.kt`
- `app/src/main/java/com/example/vecturai/ar/PoseUtils.kt`

## Used By
- Mapping Subsystem
- Navigation Subsystem

## Main Flow
1. **Setup:** `ArSessionConfig` enables Cloud Anchor mode in the ARCore session.
2. **Hosting (Mapping):** User drops a pin; `CloudAnchorHelper.hostAnchor` sends the pose to Google. The returned `cloudAnchorId` is saved in the `MapNode`.
3. **Resolving (Navigation):** `CloudAnchorHelper.resolveAnchor` is called with IDs from the loaded graph.
4. **Localization:** Once an anchor is resolved, `PoseUtils.sessionFromGraphPose` computes the offset between the virtual graph coordinates and the current ARCore session.

## Key Symbols
- `CloudAnchorHelper.hostAnchor()`
- `CloudAnchorHelper.resolveAnchor()`
- `Config.CloudAnchorMode.ENABLED`

## Config / Env / Flags
- `ARCORE_API_KEY`: Required for API authentication.
- `ttlDays`: Default set to 1 day for MVP.

## Data Structures / Protocols
- `HostedCloudAnchor`: Container for ID and the temporary local anchor.

## Related Tests
N/A (Requires real-device/live network)

## Related File Dossiers
- [CloudAnchorHelper.kt](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-document/files/app_src_main_java_com_example_vecturai_ar_CloudAnchorHelper.kt.md)
- [ArSessionConfig.kt](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-document/files/app_src_main_java_com_example_vecturai_ar_ArSessionConfig.kt.md)

## Risks / Notes
- Resolution success depends heavily on lighting and visual feature similarity to the hosting time.
- Relies on valid Google Cloud Project configuration and API key restrictions.
