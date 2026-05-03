# ADR-029: Demo Device Path Requires Strict Entrance Marker Asset Matching and Calibrated Reviewed Package Coordinates

**Status:** Accepted
**Date:** 2026-03-23
**Deciders:** Vectura AI iOS team
**Relates to:** ADR-026, ADR-028

## Context

Phase 4 established entrance-marker-based AR alignment, but the device path still had a lenient fallback: `ARMarkerDetector` would accept *any* detected image anchor and treat it as the entrance marker. This was acceptable during development but creates two problems for a real demo:

1. **False positive alignment**: A random poster, logo, or texture could trigger alignment, placing arrows in a completely wrong position.
2. **No asset validation**: If the AR reference image asset was missing from the Xcode asset catalog, the session would start silently without detection capability — the user would wait 30 seconds and hit the timeout every time.

Additionally, the reviewed package node coordinates were placeholder values from the manual editing phase. A real demo requires measured, calibrated positions matching the physical house layout.

## Decision

### Strict marker detection on real devices

`ARMarkerDetector` maintains a `knownMarkers` dictionary keyed by `referenceImageName`. Only images whose detected name matches a registered known marker are accepted. Unknown images are logged and rejected:

```swift
guard let known = knownMarkers[detectedName] else {
    print("[MarkerDetector] Rejected unknown image: '\(detectedName)'")
    return
}
```

The "any image anchor" fallback code path has been removed entirely.

### Asset catalog validation at session start

`ARSessionManager.startSession()` loads the `AR Resources` group and verifies that the expected reference image exists before configuring the AR session. On real devices, if the image is missing, the session does not start — instead, the `onMarkerAssetMissing` callback fires and the UI shows a configuration error overlay. On Simulator, this check is skipped (AR Resources are not available in the simulator environment).

### AR reference image asset in the Xcode asset catalog

The entrance marker reference image is stored in the standard Xcode asset catalog structure:

```
Assets.xcassets/
  AR Resources.arresourcegroup/
    entrance_marker_main.arreferenceimage/
      Contents.json     (physicalWidth: 0.21)
      entrance_marker.png
```

The `referenceImageName` in `entrance_markers.json` must match the `.arreferenceimage` directory name (without extension). The demo readiness check validates this consistency.

### Calibration workflow for physical space

Node coordinates in `nav_graph.json` use a building-local coordinate system with the entrance marker as origin (+X right, +Y up, -Z forward into the building). Calibration involves measuring real-world distances and updating the JSON files. The calibration guide documents the full procedure.

### Validation and regression tooling

Two scripts protect demo integrity:

- `scripts/check-demo-readiness.sh`: Comprehensive 7-section pre-demo validator (package, bundle consistency, AR assets, naming, QR contract, route connectivity, Xcode project).
- `scripts/regression-checks.sh`: Fast 4-section pre-commit checks (QR parsing, package loading, route existence, marker consistency).

## Consequences

- On real devices, only the registered entrance marker triggers alignment. Random images cannot produce false alignment.
- A missing AR reference image asset produces an immediate, clear error instead of a silent 30-second timeout.
- The Simulator path remains unaffected — it skips the asset check and uses the simulated alignment flow.
- Package coordinates must be calibrated to the physical space before a demo. The calibration guide provides the procedure.
- Both validation scripts must pass before any demo. The demo operator guide references them.
- The `referenceImageName` is a contract between three locations: `entrance_markers.json`, the asset catalog directory name, and the ARKit detection result. All three must agree.
