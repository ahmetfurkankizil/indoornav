# File Dossier: checkpoint-marker-smoke-checklist.md

## Path
`docs\qa\checkpoint-marker-smoke-checklist.md`

## Type
Authored Source

## Role
Authored Source for the docs component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
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
- [
```

## Status
Mapped (Pass 3 Normalization)
