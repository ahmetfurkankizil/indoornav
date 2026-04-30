# File Dossier: GraphRepository.kt

## Path
app/src/main/java/com/example/vecturai/persistence/GraphRepository.kt

## Type
source

## Role
Handles file system persistence for graphs and localization metadata.

## Imports / Includes
- `kotlinx.serialization.json.Json`
- `java.io.File`

## Exports / Public Surface
- `GraphRepository` (class)
- `save(graph)` / `load(name)`
- `saveLocalizationHint(name, hint)` / `loadLocalizationHint(name)`
- `LocalizationHint` (data class)

## Main Symbols
- `graphsDir`: Directory for graph JSON files.
- `hintsDir`: Directory for building-specific localization hints.
- `LocalizationHint`: Stores `lastResolvedAnchorIds` and `lastUserGraphPose` for faster startup.

## Important Logic by Line Range
- L35-50: JSON serialization/deserialization logic.
- L60-80: Hint management logic.

## Uses
- `MapGraph.kt`

## Used By
- `MappingViewModel.kt`
- `NavigationViewModel.kt`

## Config / Constants / Protocol Details
- Uses `encodeToString` and `decodeFromString` for persistence.
- Filenames derived from building titles.

## Related Tests
N/A

## Notes / Risks
- `ignoreUnknownKeys = true` allows for some schema evolution without crashes.
- Repository is initialized with `context.filesDir`.
