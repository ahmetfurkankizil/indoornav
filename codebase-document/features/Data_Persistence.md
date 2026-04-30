# Feature: Data Persistence

## Purpose
Enables the application to save mapped indoor environments to local storage and retrieve them later for navigation.

## Implemented In
- `app/src/main/java/com/example/vecturai/persistence/GraphRepository.kt`
- `app/src/main/java/com/example/vecturai/graph/MapGraph.kt`

## Used By
- Mapping Subsystem (Save)
- Navigation Subsystem (Load/List)

## Main Flow
1. **Save:** `MappingViewModel` provides a `MapGraph`; `GraphRepository` serializes it to JSON and writes to `filesDir/graphs`.
2. **List:** `NavigationViewModel` requests building names; `GraphRepository` scans the directory for `.json` files.
3. **Load:** `NavigationViewModel` requests a building by name; `GraphRepository` reads and deserializes the JSON file.

## Key Symbols
- `GraphRepository.save()`
- `GraphRepository.load()`
- `@Serializable`

## Config / Env / Flags
- Internal storage path: `graphs/` subdirectory.

## Data Structures / Protocols
- JSON (via Kotlinx Serialization)

## Related Tests
N/A

## Related File Dossiers
- [MapGraph.kt](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-document/files/app_src_main_java_com_example_vecturai_graph_MapGraph.kt.md)
- [GraphRepository.kt](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-document/files/app_src_main_java_com_example_vecturai_persistence_GraphRepository.kt.md)

## Risks / Notes
- Storage is private to the application.
- JSON schema changes require careful handling (currently using `ignoreUnknownKeys = true`).
