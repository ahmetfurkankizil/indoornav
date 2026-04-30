# Feature: UI Theme

## Purpose
Defines the visual language, color palette, and typography for the application using Material Design 3.

## Implemented In
- `app/src/main/java/com/example/vecturai/ui/theme/Theme.kt`
- `app/src/main/java/com/example/vecturai/ui/theme/Color.kt`
- `app/src/main/java/com/example/vecturai/ui/theme/Type.kt`

## Used By
- All UI Screens

## Main Flow
1. `VecturaiTheme` is applied in `MainActivity`.
2. It detects the system theme (Light/Dark).
3. On Android 12+, it optionally applies dynamic colors derived from the user's wallpaper.
4. Fallback schemes (`Teal`, `BlueGrey`, `Coral`) are used for older versions or non-dynamic preference.

## Key Symbols
- `VecturaiTheme`
- `Teal80`, `Coral40`

## Config / Env / Flags
- `dynamicColor`: Set to `true` by default.

## Data Structures / Protocols
- Material3 `ColorScheme`

## Related Tests
N/A

## Related File Dossiers
- [MainActivity.kt](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-document/files/app_src_main_java_com_example_vecturai_MainActivity.kt.md)

## Risks / Notes
- The theme is tailored for readability in high-brightness AR environments.
