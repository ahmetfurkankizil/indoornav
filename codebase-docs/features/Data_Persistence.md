# Feature: Data Persistence

## Purpose
Enables the application to save mapped indoor environments and localization state to local storage.

## Implemented In
- `app/src/main/java/com/example/vecturai/persistence/GraphRepository.kt`
- `app/src/main/java/com/example/vecturai/graph/MapGraph.kt`

## Used By
- Mapping Subsystem (Save)
- Navigation Subsystem (Load/List/Hints)

## Main Flow
1. **Save:** `MappingViewModel` provides a `MapGraph`; `GraphRepository` serializes it to JSON and writes to `filesDir/graphs`.
2. **List:** `NavigationViewModel` requests building names; `GraphRepository` scans the directory for `.json` files.
3. **Load:** `NavigationViewModel` requests a building by name; `GraphRepository` reads and deserializes the JSON file.
4. **Localization Hints:** `GraphRepository.saveLocalizationHint` persists the last known session alignment and user pose to `filesDir/graph_hints`. This allows faster re-localization on subsequent runs.

## Key Symbols
- `GraphRepository.save()`
- `GraphRepository.saveLocalizationHint()`
- `LocalizationHint`
- `GraphPoseHint`

## Config / Env / Flags
- Internal storage paths: `graphs/` and `graph_hints/` subdirectories.

## Data Structures / Protocols
- JSON (via Kotlinx Serialization)
- `LocalizationHint`: Stores `lastResolvedAnchorIds` and `lastUserGraphPose`.

## Related Tests
N/A

## Related File Dossiers
- [MapGraph.kt](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-docs/files/app_src_main_java_com_example_vecturai_graph_MapGraph.kt.md)
- [GraphRepository.kt](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-docs/files/app_src_main_java_com_example_vecturai_persistence_GraphRepository.kt.md)

## Risks / Notes
- Schema changes in `MapGraph` or `LocalizationHint` require careful handling.
- Hints are building-specific.
