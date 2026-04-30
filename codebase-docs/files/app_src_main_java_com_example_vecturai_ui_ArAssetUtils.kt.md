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
- `hasValidGlbAsset`: Checks the first 4 bytes of an asset for the `glTF` magic header.

## Important Logic by Line Range
- L5-14: Opens the asset stream and verifies the header bytes `[0x67, 0x6C, 0x54, 0x46]`.

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
