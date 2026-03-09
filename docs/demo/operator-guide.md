# Operator Guide

The **operator** is the technical person who sets up, builds, and troubleshoots the demo. They prepare the environment so the **presenter** can focus on the narrative.

## Before the Demo

### 1. Build the App

```bash
# Android
make android-debug
# Install to device: make android-install

# iOS
make ios-open
# Build in Xcode → select device → Run
```

### 2. Prepare the Demo Package

```bash
make preprocess          # Generate package from authoring config
make verify-package      # Verify package integrity
```

### 3. Print and Place Markers

- Print entrance marker at dimensions matching config (default: 21×21 cm)
- Mount on flat wall at ~1.2–1.5m height, well-lit area
- For checkpoint markers: see [Checkpoint Placement Guide](checkpoint-marker-placement.md)

### 4. Device Setup

- Set device screen brightness to max
- Disable auto-lock
- Disable Do Not Disturb
- Clear camera lens
- Verify Wi-Fi (not strictly needed, but avoids OS popups)

### 5. Pre-Flight Check

- [ ] App launches without crash
- [ ] Sample building appears in building list
- [ ] Rooms are searchable
- [ ] "Simulate Scan" works (if Demo/Dev mode)
- [ ] AR camera opens (if Live mode)

## During the Demo

| Situation | Operator Action |
|-----------|----------------|
| App launches | Stand behind presenter, monitor debug panel |
| Marker won't scan | Check lighting, angle, distance (1–2m). If stuck: "Simulate Scan" |
| Arrows misaligned | Suggest rescan from a different angle |
| Progress stalls | Walk the presenter closer to route corridor |
| Checkpoint not detected | OK — falls back to single-marker flow |
| App crash | Relaunch, tap building → room → start navigation |
| Battery warning | Plug in charging cable |

## After the Demo

- [ ] Close the app
- [ ] Note any issues in a quick log
- [ ] Collect markers if temporary placement

## Fallback Plan

1. **Simulate Scan**: Use Demo mode's simulate button to bypass marker detection
2. **Demo Mode**: Switch to fully scripted demo with "Advance" button
3. **Video backup**: Have a screen recording of a successful demo run ready

## Recovery Commands

```bash
# Rebuild everything from scratch
make clean && make preprocess && make android-debug

# Verify everything works
make verify-all

# Clear corrupted local history (safe to delete)
# On Android: Clear app data in Settings
# On iOS: Delete and reinstall the app
```
