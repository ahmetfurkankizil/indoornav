# File Dossier: Theme.kt

## Path
app/src/main/java/com/example/vecturai/ui/theme/Theme.kt

## Type
source

## Role
Jetpack Compose theme wrapper.

## Imports / Includes
- `androidx.compose.material3.MaterialTheme`
- `com.example.vecturai.ui.theme.Typography`

## Exports / Public Surface
- `VecturaiTheme` (Composable)

## Main Symbols
- `VecturaiTheme`: Configures the `MaterialTheme` with dynamic or static color schemes.

## Important Logic by Line Range
- L43-51: Logic to select between `dynamicColorScheme` (Android 12+) and the custom `Dark/LightColorScheme`.

## Uses
- `Color.kt`
- `Type.kt`

## Used By
- `MainActivity.kt`

## Config / Constants / Protocol Details
N/A

## Related Tests
N/A

## Notes / Risks
N/A
