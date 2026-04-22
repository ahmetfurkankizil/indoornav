# File Dossier: verify-ios-local.sh

## Path
`scripts\verify-ios-local.sh`

## Type
Authored Source

## Role
Authored Source for the scripts component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
#!/usr/bin/env bash
# ─────────────────────────────────────────────────
# VecturAI — iOS Local Build Verification
# Requires Xcode and macOS.
# ─────────────────────────────────────────────────
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "═══ iOS Local Verification ═══"
echo ""

# Step 1: Build shared framework
echo "1. Building shared KMP framework for iOS..."
./gradlew :shared:core:linkDebugFrameworkIosArm64
echo "✅ Shared framework built"
echo ""

# Step 2: Xcode build (simulator)
if command -v xcodebuild &>/dev/null; then
    echo "2. Building iosApp for simulator..."
    xcodebuild build \
        -project apps/iosApp/iosApp.xcodeproj \
        -scheme iosApp \
        -destination 'platform=iOS Simulator,name=iPhone 15' \
        -configuration Debug \
        CODE_SIGNING_ALLOWED=NO \
        2>&1 | tail -5

    if [[ ${PIPESTATUS[0]} -eq 0 ]]; then
        echo "✅ iOS simulator build succeeded"
    else
        echo "❌ iOS simulator build failed
```

## Status
Mapped (Pass 3 Normalization)
