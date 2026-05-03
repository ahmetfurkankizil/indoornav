# File Dossier: Vectura AIApp.kt

## Path
`apps/androidApp/src/main/kotlin/com/Vectura AI/android/Vectura AIApp.kt`

## Type
Authored Source (Android Application)

## Role
Initializes global application state, specifically the Koin DI framework.

## Imports / Includes
- `org.koin.android.ext.koin.androidContext`
- `org.koin.core.context.startKoin`
- All KMP and platform modules.

## Exports / Public Surface
- `Vectura AIApp`: Application class.

## Logic
- `onCreate()`: Starts Koin and loads all relevant modules (Core, Data, Features, Android).

## Uses
- All subsystem Koin modules.

## Used By
- `AndroidManifest.xml`: Defined as the application class.

## Related Tests
- None.
