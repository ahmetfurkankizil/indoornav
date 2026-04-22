# File Dossier: operator-guide.md

## Path
`docs\demo\operator-guide.md`

## Type
Authored Source

## Role
Authored Source for the docs component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
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


```

## Status
Mapped (Pass 3 Normalization)
