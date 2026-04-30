# File Dossier: ModePickerScreen.kt

## Path
app/src/main/java/com/example/vecturai/ui/ModePickerScreen.kt

## Type
source

## Role
Landing screen for mode selection and privacy compliance.

## Imports / Includes
- `com.example.vecturai.ar.CloudAnchorAuthStatus`

## Exports / Public Surface
- `ModePickerScreen` (Composable)

## Main Symbols
- `ModePickerScreen`: The initial UI shown after the splash/main initialization.

## Important Logic by Line Range
- L44-52: App description and headline.
- L53-73: Status surface showing the Cloud Anchor API configuration state.
- L79-91: Navigation buttons to "Map" and "Navigate" modes.
- L94-123: Mandatory `AlertDialog` for Cloud Anchor privacy disclosure.

## Uses
- `CloudAnchorAuthStatus.kt`

## Used By
- `MainActivity.kt`

## Config / Constants / Protocol Details
- Privacy Policy Link: `https://policies.google.com/privacy`.

## Related Tests
N/A

## Notes / Risks
- Disclosure dialog cannot be dismissed without acceptance, ensuring compliance.
