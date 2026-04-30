# File Dossier: ArAssetUtils.kt

## Path
app/src/main/java/com/example/vecturai/ui/ArAssetUtils.kt

## Type
source

## Role
Helper for validating binary AR assets.

## Imports / Includes
- `android.content.Context`

## Exports / Public Surface
- `hasValidGlbAsset(context, assetPath)` (function)

## Main Symbols
- `hasValidGlbAsset`: Verifies GLB magic header, version (2), and declared file length.

## Important Logic by Line Range
- L5-25: Opens asset stream, verifies `glTF` magic, checks version, and validates declared length against actual file size.

## Uses
- Android AssetManager

## Used By
- `NavigationScreen.kt`

## Config / Constants / Protocol Details
- GLB Magic Header: `glTF`.

## Related Tests
N/A

## Notes / Risks
- Lightweight check; does not validate the entire file structure.
