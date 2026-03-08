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
cd vecturai

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

If marker scanning is unreliable, use **Simulate Scan** instead.

## Troubleshooting

| Issue | Fix |
|-------|-----|
| No building loaded | Check `DemoPackageProvider.buildPackage()` is called at startup |
| Gradle sync fails | `./gradlew --refresh-dependencies` |
| iOS build error | Clean Xcode build folder (⇧⌘K), rebuild |
| ARCore not available | Use Android device with ARCore support, or use Simulate Scan |
