# File Dossier: AndroidManifest.xml

## Path
`apps/androidApp/src/main/AndroidManifest.xml`

## Type
Authored Config (Android Manifest)

## Role
Declares application components, permissions, hardware requirements, and ARCore metadata for the Android app.

## Logic
- Permissions: `CAMERA`, `INTERNET`, `VIBRATE`.
- Feature requirement: `android.hardware.camera.ar` required.
- Activities: `MainActivity` is the launcher/home activity; `.ar.ArCameraActivity` is a non-exported AR camera flow Activity.
- Application theme: platform light no-action-bar theme for Compose ownership.
- Metadata: `com.google.ar.core` required.

## Used By
- Android OS / Play Store.
- Android Studio run configuration discovery.
