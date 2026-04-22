# File Dossier: AndroidModule.kt

## Path
`apps/androidApp/src/main/kotlin/com/vecturai/android/di/AndroidModule.kt`

## Type
Authored Source (DI Configuration)

## Role
Defines the Koin module for Android-specific dependencies.

## Imports / Includes
- `com.vecturai.android.ar.ArBridge`
- `org.koin.dsl.module`

## Exports / Public Surface
- `androidModule`: The Koin module definition.

## Main Symbols
- `androidModule`: Singleton provider for platform dependencies.

## Logic
- Registers `ArBridge` as a single instance.

## Uses
- `ArBridge`.

## Used By
- `VecturaiApp.kt`: Loaded into the global Koin context.

## Related Tests
- None.
