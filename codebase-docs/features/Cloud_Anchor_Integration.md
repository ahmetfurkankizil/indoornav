# Feature: Cloud Anchor Integration

## Purpose
Enables world-scale persistent AR by hosting local anchors to Google Cloud and resolving them later to localize the device within a saved graph.

## Implemented In
- `app/src/main/java/com/example/vecturai/ar/CloudAnchorHelper.kt`
- `app/src/main/java/com/example/vecturai/ar/Relocalizer.kt`
- `app/src/main/java/com/example/vecturai/ar/ArSessionConfig.kt`
- `app/src/main/java/com/example/vecturai/ar/PoseUtils.kt`

## Used By
- Mapping Subsystem
- Navigation Subsystem

## Main Flow
1. **Setup:** `ArSessionConfig` enables Cloud Anchor mode in the ARCore session.
2. **Hosting (Mapping):** User drops a pin; `CloudAnchorHelper.hostAnchor` sends the pose to Google. The returned `cloudAnchorId` is saved in the `MapNode`.
3. **Resolving (Navigation):** `CloudAnchorHelper.resolveAnchor` is called with IDs from the loaded graph.
4. **Consensus & Localization:** 
   - `Relocalizer` uses multiple resolved anchors to compute a weighted Procrustes alignment.
   - Outlier rejection removes anchors with large residuals to ensure a stable fit.
   - The resulting `graphToSessionPose` aligns the virtual graph with the real world.
5. **Caching:** `GraphRepository` saves `LocalizationHint` (last fit and user pose) to speed up subsequent session startups.

## Key Symbols
- `Relocalizer.fitGraphToSession()`
- `CloudAnchorHelper.resolveAnchor()`
- `Correspondence`
- `LocalizationHint`

## Config / Env / Flags
- `ARCORE_API_KEY`: Required for API authentication.
- `ttlDays`: Default set to 1 day.

## Data Structures / Protocols
- `HostedCloudAnchor`: Container for ID and the temporary local anchor.
- `Correspondence`: Pairs a graph pose with its resolved session pose.

## Related Tests
- [RelocalizerTest.kt](file:///c:/Users/emirh/Desktop/vecturDENEME/app/src/test/java/com/example/vecturai/ar/RelocalizerTest.kt)

## Related File Dossiers
- [CloudAnchorHelper.kt](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-docs/files/app_src_main_java_com_example_vecturai_ar_CloudAnchorHelper.kt.md)
- [Relocalizer.kt](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-docs/files/app_src_main_java_com_example_vecturai_ar_Relocalizer.kt.md)
- [ArSessionConfig.kt](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-docs/files/app_src_main_java_com_example_vecturai_ar_ArSessionConfig.kt.md)

## Risks / Notes
- Resolution success depends heavily on lighting and visual feature similarity.
- Multi-anchor fitting reduces drift but requires several anchors to be visible or resolved sequentially.
