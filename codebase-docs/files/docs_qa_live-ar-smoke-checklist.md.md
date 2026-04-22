# File Dossier: live-ar-smoke-checklist.md

## Path
`docs\qa\live-ar-smoke-checklist.md`

## Type
Authored Source

## Role
Authored Source for the docs component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
# Live AR Smoke Test Checklist

Manual QA on a real device with physical printed marker.

## Setup
- [ ] Marker printed at 21×21 cm on matte paper
- [ ] Mounted flat at ~1.2m height
- [ ] Good lighting, no direct glare on marker
- [ ] Device: iPhone 12+ or ARCore-compatible Android

## Marker Detection
- [ ] "Waiting for Marker" state shown
- [ ] Point camera at marker from ~1–2m distance
- [ ] Marker detected within 2–5 seconds
- [ ] "Navigating" state shown after detection
- [ ] Arrows appear aligned with real space

## Live Progress
- [ ] Walk forward along expected route corridor
- [ ] Progress bar advances as you move
- [ ] Remaining distance decreases plausibly
- [ ] Progress percent matches approximate walking distance
- [ ] No backwards progress during forward walking

## Checkpoint Correction (v1.6)
- [ ] If package has checkpoint markers, walk toward one
- [ ] Checkpoint detection logged (does NOT restart session)
- [ ] Alignment correction applied (arrows adjust subtly)
- [ 
```

## Status
Mapped (Pass 3 Normalization)
