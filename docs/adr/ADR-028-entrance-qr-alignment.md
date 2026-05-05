# ADR-028: Entrance QR Establishes Initial iOS Alignment and Facing Before AR Guidance Begins

**Status:** Accepted
**Date:** 2026-03-23
**Deciders:** VecturAI iOS team
**Relates to:** ADR-026, ADR-027, ADR-025

## Context

Phase 3 completed rolling-lookahead AR guidance, but the QR scan was still simulated and AR alignment was not grounded in any real marker detection. This meant:

1. The QR "scan" was a button press — no real payload, no validation.
2. AR alignment was invented at session start (offset = 0, rotation = 0), ignoring the entrance marker position in the reviewed package.
3. Navigation arrows appeared immediately on AR startup, before any real-world anchor was established.
4. There was no contract between QR payload, reviewed package entrance metadata, and AR world alignment.

## Decision

### QR payload is the entry gate

The QR code encodes a small JSON payload:

```json
{
  "type": "VecturAI-entrance",
  "buildingId": "house-demo-01",
  "entranceId": "marker-entrance-a",
  "v": 1
}
```

The app validates `buildingId` against `manifest.json` and `entranceId` against `entrance_markers.json`. Invalid payloads produce a clear, retryable error — no silent acceptance.

### Reviewed package entrance metadata is the alignment source

The `entrance_markers.json` record (already part of the reviewed package) provides everything needed for AR alignment:

| Field | Used for |
|---|---|
| `referenceImageName` | ARKit detection image name |
| `physicalWidthMeters` | ARKit `ARReferenceImage` physical size |
| `position` | Building-local marker position (x, y, z) |
| `rotationYDegrees` | Building-local yaw |
| `startNodeId` | Route start node (already used by Dijkstra) |

No new schema files are required.

### AR rendering is gated on alignment

Navigation arrows are not placed or shown until the entrance marker is physically detected by ARKit. The pre-alignment overlay ("Point the camera at the entrance QR") is shown until alignment is locked.

### Alignment timeout with retry

If the marker is not detected within 30 seconds, the overlay enters a retry/cancel state. Retry resets the detector and restarts the AR session. Cancel returns to the flow. There is no automatic fallback to invented alignment.

### Post-alignment behavior is unchanged

After alignment is locked, Phase 3 rolling lookahead, fade-behind, and distance-based arrival continue exactly as before.

## Consequences

- No navigation is possible without a real (or simulated-on-Simulator) QR scan.
- Mismatched building or unknown entrance produces a clear rejection message.
- AR world is anchored to the real-world entrance marker position, not an arbitrary origin.
- The alignment timeout prevents the user from being stuck on a black screen indefinitely.
- The reviewed package remains the single source of truth for all spatial data.
- The Simulator retains a simulated scan path so development and demos work without a physical device.
