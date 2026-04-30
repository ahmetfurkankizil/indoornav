# File Dossier: GraphRepository.kt

## Path
app/src/main/java/com/example/vecturai/persistence/GraphRepository.kt

## Type
source

## Role
Local file-based persistence for navigation graphs.

## Imports / Includes
- `android.content.Context`
- `kotlinx.coroutines.Dispatchers`
- `kotlinx.serialization.json.Json`

## Exports / Public Surface
- `GraphRepository` (class)
- `save(graph)` (suspend function)
- `load(buildingName)` (suspend function)
- `listBuildings()` (suspend function)

## Main Symbols
- `directory` (property): target directory `filesDir/graphs`.
- `json` (property): Configured `Json` instance with `prettyPrint` and `ignoreUnknownKeys`.
- `toFileName()` (extension function): Sanitizes building names for safe file storage.

## Important Logic by Line Range
- L20: Application-specific directory creation.
- L22-25: IO-dispatched JSON serialization and write.
- L27-30: IO-dispatched read and deserialization.
- L40-44: Filename sanitization logic.

## Uses
- `MapGraph.kt`
- Kotlinx Serialization

## Used By
- `MappingViewModel.kt`
- `NavigationViewModel.kt`

## Config / Constants / Protocol Details
- JSON format: Pretty printed, ignores unknown keys for forward compatibility.
- Storage Location: `filesDir/graphs` (private internal storage).

## Related Tests
N/A

## Notes / Risks
- Uses `Dispatchers.IO` for all file operations to avoid blocking the main thread.
- File naming logic replaces non-alphanumeric characters with underscores.
