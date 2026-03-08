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

## Tracking Quality
- [ ] "Normal" tracking in good conditions
- [ ] "Limited" warning appears if camera obstructed
- [ ] Low-confidence indicator shows when far from route (> 3m)

## Recenter / Rescan
- [ ] "Rescan" button visible during navigation
- [ ] Tapping "Rescan" shows rescan instruction
- [ ] Re-pointing at marker re-aligns route
- [ ] Progress preserves (doesn't regress) after rescan

## Arrival (Live)
- [ ] Walk close to destination (~1.5m)
- [ ] Progress reaches ≥ 95% or remaining < 1.5m
- [ ] Arrival overlay appears
- [ ] "You've arrived!" shown
- [ ] Session saved to history

## Fallback
- [ ] If marker won't scan: "Simulate Scan" available in dev/demo mode
- [ ] Simulate path still works as before
- [ ] Advance button available in demo mode only

## Known Issues
- Drift may accumulate on routes > 15m
- Single-marker-only; no multi-marker correction
- Progress is route-relative, not absolute position
- Off-route walking shows low-confidence but doesn't auto-cancel
