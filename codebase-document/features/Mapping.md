# Feature: Mapping

## Purpose
Enables users to digitize indoor environments by dropping anchors (Cloud Anchors) and connecting them into a navigable graph.

## Implemented In
- `app/src/main/java/com/example/vecturai/ui/mapping/MappingScreen.kt`
- `app/src/main/java/com/example/vecturai/ui/mapping/MappingViewModel.kt`

## Used By
- Application Users

## Main Flow
1. User enters mapping mode and provides a building name.
2. User walks through the environment.
3. **Manual Drop:** User clicks "Drop Pin" to anchor a point of interest (e.g., entrance).
4. **Auto-drop:** The system automatically hosts an anchor every 4 meters of horizontal movement.
5. **Connectivity:** The system automatically creates "edges" between consecutive anchors.
6. **Tagging:** User can label any anchor (e.g., "Room 302") to make it a searchable destination.
7. User saves the graph to local storage.

## Key Symbols
- `MappingViewModel.hostAnchorAt()`
- `MappingViewModel.AUTO_DROP_DISTANCE_M`
- `MapGraph`

## Config / Env / Flags
- Auto-drop toggle in the UI.

## Data Structures / Protocols
- `MappingMarker`: UI-friendly representation of a node in the current AR session.

## Related Tests
N/A

## Related File Dossiers
- [MappingScreen.kt](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-document/files/app_src_main_java_com_example_vecturai_ui_mapping_MappingScreen.kt.md)
- [MappingViewModel.kt](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-document/files/app_src_main_java_com_example_vecturai_ui_mapping_MappingViewModel.kt.md)

## Risks / Notes
- Mapping quality depends on walking speed and environmental textures.
- The origin is set at the first hosted anchor, making the graph relative to that physical point.
