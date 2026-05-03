# File Dossier: AndroidHapticManager.kt

## Path
`apps/androidApp/src/main/kotlin/com/Vectura AI/android/ar/AndroidHapticManager.kt`

## Type
Authored Source (Android Haptics)

## Role
Small Android haptics adapter for route start, imminent turn, re-centering, and arrival feedback.

## Imports / Includes
- `android.content.Context`
- `android.os.Build`
- `android.os.VibrationEffect`
- `android.os.Vibrator`
- `android.os.VibratorManager`

## Exports / Public Surface
- `AndroidHapticManager`
- `isEnabled`
- `routeStarted()`
- `turnImminent()`
- `recentering()`
- `arrived()`

## Logic
- Resolves the appropriate Android vibrator service based on API level.
- Gates all vibration through `isEnabled`.
- Uses one-shot vibration effects on Android O+ and legacy vibration otherwise.

## Uses
- Android `VIBRATE` permission from `AndroidManifest.xml`.

## Used By
- `AndroidArNavigationViewModel.kt`
- `AndroidModule.kt`

## Related Tests
- None.
