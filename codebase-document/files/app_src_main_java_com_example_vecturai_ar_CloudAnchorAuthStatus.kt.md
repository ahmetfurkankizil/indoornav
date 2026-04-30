# File Dossier: CloudAnchorAuthStatus.kt

## Path
app/src/main/java/com/example/vecturai/ar/CloudAnchorAuthStatus.kt

## Type
source

## Role
Utility to verify ARCore API key presence and status.

## Imports / Includes
- `android.content.Context`
- `android.content.pm.PackageManager`

## Exports / Public Surface
- `CloudAnchorAuthStatus` (data class)
- `CloudAnchorAuthStatus.from(context)` (static function)

## Main Symbols
- `from(context)`: Checks for the `com.google.android.ar.API_KEY` meta-data in the manifest.
- `readArCoreApiKey()`: Extension function to extract the key from `ApplicationInfo.metaData`.

## Important Logic by Line Range
- L13-27: Logic to return a configured or unconfigured status based on the API key presence.
- L31-41: Manifest meta-data extraction with backward compatibility for older Android versions.

## Uses
- Android PackageManager

## Used By
- `MainActivity.kt`
- `ModePickerScreen.kt`

## Config / Constants / Protocol Details
- Manifest key: `com.google.android.ar.API_KEY`.

## Related Tests
N/A

## Notes / Risks
- Does not verify if the key is *valid* on Google Cloud, only that it is *present* in the APK.
