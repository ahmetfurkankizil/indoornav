# First-Day Setup Guide

Get VecturAI building and running in ~15 minutes.

## Prerequisites

| Tool | Version | Install |
|------|---------|---------|
| JDK | 21+ | `brew install --cask temurin` |
| Android Studio | Latest | [developer.android.com](https://developer.android.com/studio) |
| Xcode | 15+ | Mac App Store (iOS only) |
| Git | 2.x | `brew install git` |
| Python 3 | 3.8+ | Pre-installed on macOS |

## 1. Clone and Explore

```bash
git clone <repo-url>
cd vecturai
make help           # see all available targets
```

## 2. Run Tests

```bash
make test-preprocessor
# Expected: ~170 tests pass
```

## 3. Generate Demo Package

```bash
make preprocess
make verify-package
```

## 4. Build Android

```bash
make android-debug
# APK at: apps/androidApp/build/outputs/apk/debug/
```

Install on device:
```bash
make android-install  # requires connected device with USB debugging
```

## 5. Open iOS

```bash
make ios-open
```

In Xcode:
1. Select your device or simulator
2. Build and run (⌘R)
3. Trust the developer certificate on device if prompted

## 6. Run a Demo (Scripted)

1. Launch the app
2. The sample building loads automatically (in Dev/Demo config)
3. Tap a room → "Kitchen"
4. Tap "Start Navigation"
5. Tap "Simulate Scan" (skips real marker)
6. Tap "Advance" repeatedly to walk the route
7. Arrival overlay appears at ~95%

## 7. Run a Live Test (Real Marker)

1. Print the entrance marker (21×21 cm, matte paper)
2. Mount flat on wall at ~1.2m height, good lighting
3. Launch app in Dev mode
4. Select room → Start Navigation
5. Point camera at marker from ~1–2m
6. Walk the route — arrows guide you live

## 8. Full Verification

```bash
make verify-all      # tests + Android build + package check
make verify-ios      # iOS build (requires Xcode)
```

## What's Safe to Delete/Reset

| Artifact | How to Reset | Impact |
|----------|-------------|--------|
| Build cache | `make clean` | Rebuilds from scratch |
| Demo package | `make preprocess` | Regenerated from authoring config |
| Local history | Clear app data on device | Loses visit history only |
| Gradle cache | `rm -rf ~/.gradle/caches` | Slower next build |

## Troubleshooting

| Problem | Solution |
|---------|----------|
| Gradle fails | Ensure JDK 21: `java -version` |
| Android build fails | Open in Android Studio, sync Gradle |
| iOS build fails | Open `.xcodeproj`, resolve signing |
| Tests fail | Run `make test-preprocessor` and check output |
| Package invalid | Run `make preprocess` to regenerate |
