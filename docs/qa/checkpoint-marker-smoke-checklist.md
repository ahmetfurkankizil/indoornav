# Checkpoint Marker Smoke Checklist

## Purpose
Verify that checkpoint markers work correctly alongside the existing entrance-marker flow.

---

## Prerequisites
- [ ] Device with AR capability (iOS: ARKit / Android: ARCore)
- [ ] Printed entrance marker + at least one printed checkpoint marker
- [ ] Building package with `checkpointMarkers` array populated
- [ ] Markers physically placed along the route

---

## A. Single-Marker Route (regression)
- [ ] Load a package with **no** checkpoint markers
- [ ] Start navigation → scan entrance marker → arrows appear
- [ ] Walk the route → progress advances → arrival detected
- [ ] Demo/simulate mode still works
- [ ] Confirm: identical behavior to v1.5

## B. Checkpoint-Marker Route
- [ ] Load a package with 1+ checkpoint markers
- [ ] Start navigation → scan entrance marker → arrows appear
- [ ] Walk toward first checkpoint marker
- [ ] Verify: checkpoint marker detection logged in debug output
- [ ] Verify: session does **NOT** restart
- [ ] Verify: alignment correction applied (check debug panel)
- [ ] Continue walking → progress continues forward
- [ ] Walk past second checkpoint (if present) → correction applied again
- [ ] Verify: arrival detection works normally

## C. Confidence & Off-Route
- [ ] Check debug panel shows confidence state (alignment, progress, off-route)
- [ ] Deliberately walk away from route → status changes to MINOR_DRIFT or OFF_ROUTE
- [ ] Walk back → status recovers to ON_ROUTE
- [ ] Cover camera briefly → tracking limited state shown

## D. Edge Cases
- [ ] Rapidly scan same checkpoint marker → deduplication prevents repeated correction
- [ ] Observe checkpoint from far away (low confidence) → correction may be rejected
- [ ] Walk entire route without seeing any checkpoint → still works (single-marker fallback)

## E. Fallback Plan
If checkpoint detection is inconsistent:
1. Remove checkpoint markers from the route
2. Rely on entrance-only flow (v1.5 behavior)
3. Use demo/simulate mode for presentation
4. Check lighting conditions near checkpoint markers
5. Verify marker print quality and physical dimensions match config
