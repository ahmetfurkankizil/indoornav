# File Dossier: demo-setup.md

## Path
`docs\demo\demo-setup.md`

## Type
Authored Source

## Role
Authored Source for the docs component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
# Demo Setup

## Prerequisites
- macOS with Xcode 15+ (for iOS)
- Android Studio with SDK 34+ (for Android)
- JDK 17+
- Kotlin 2.1.10+

## Quick Start

```bash
# Clone
git clone <repo-url>
cd Vectura AI

# Verify build
make test-preprocessor

# Android
make android-debug
make android-install    # if device connected

# iOS
make ios-open           # opens Xcode
# Then: Product → Run (⌘R) for simulator
```

## Demo Configuration

The app uses `AppConfig.Demo` profile for presentations:
- Sample package preloaded automatically
- Simulate Scan enabled
- Debug overlays hidden by default
- "DEMO MODE" label shown

To switch profile, set in app startup code:
```kotlin
AppConfig.current = AppConfig.Demo
```

## Physical Marker (Optional)

For live marker scanning:
1. Print marker from `sample/demo-building/markers/entrance_marker_main.png`
2. Size: 21×21 cm on matte paper
3. Mount flat at eye level
4. Ensure good lighting (no glare)

If marker scanning is unreliable, use **Simulate Scan** instea
```

## Status
Mapped (Pass 3 Normalization)
