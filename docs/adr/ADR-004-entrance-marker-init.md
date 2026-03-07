# ADR-004: Entrance-Marker-Based Initialization

**Status:** Accepted  
**Date:** 2026-03-08

## Context

AR navigation requires knowing the user's position in the building's coordinate system. This is the "localization problem" — mapping from the AR device's world coordinates to the navigation graph coordinates.

Options considered:
1. **Compass + GPS** — unreliable indoors
2. **Cloud Anchors** — requires Google/Apple infrastructure, internet dependency, privacy concerns
3. **Visual place recognition** — ML-based, requires training data per building
4. **Entrance marker scanning** — controlled physical marker at known position

## Decision

Use **entrance marker scanning** as the sole localization method for MVP.

Each entrance has a physical marker that combines:
- A **QR code** containing the building ID, marker ID, and reference data
- A **visual reference image** that ARKit/ARCore can detect and track for 6-DoF pose estimation

When the user scans the marker:
1. QR data identifies which building and entrance marker
2. AR image detection provides the marker pose in device world coordinates
3. The known marker position in nav-graph coordinates + the detected pose gives the coordinate transform
4. All subsequent tracking uses ARKit/ARCore's SLAM relative to this initial alignment

## Consequences

### Positive
- Simple, reliable, works without internet
- No compass calibration issues indoors
- Deterministic alignment — marker position is precisely known
- Works equally well on both iOS (ARKit) and Android (ARCore)
- User flow is intuitive: "scan the marker to start navigating"

### Negative
- Requires physical markers to be printed and placed at entrances
- User must physically go to an entrance marker to start (cannot start mid-building)
- Alignment may drift over long navigation sessions (ARKit/ARCore SLAM drift)

### Mitigation
- Place markers at main entrances where users naturally enter
- For MVP, single-entrance buildings are the primary target
- Future: add additional alignment checkpoints for drift correction
