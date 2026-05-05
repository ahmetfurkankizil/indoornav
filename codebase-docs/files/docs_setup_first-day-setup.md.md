# File Dossier: first-day-setup.md

## Path
`docs\setup\first-day-setup.md`

## Type
Authored Source

## Role
Authored Source for the docs component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
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
cd VecturAI
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


```

## Status
Mapped (Pass 3 Normalization)
