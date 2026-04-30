# Feature: Navigation

## Purpose
Provides turn-by-turn AR guidance to lead a user from their current location to a selected room or landmark within a mapped building.

## Implemented In
- `app/src/main/java/com/example/vecturai/ui/navigation/NavigationScreen.kt`
- `app/src/main/java/com/example/vecturai/ui/navigation/NavigationViewModel.kt`
- `app/src/main/java/com/example/vecturai/ui/navigation/DestinationPicker.kt`

## Used By
- Application Users

## Main Flow
1. **Selection:** User picks a saved building graph and a destination room.
2. **Localization:** The app resolves Cloud Anchors from the graph to synchronize the local AR space with the graph coordinate system.
3. **Routing:** `Pathfinder` computes the shortest path between the closest node and the target.
4. **Guidance:** A 3D arrow appears in front of the user, pointing toward the next waypoint.
5. **Advancement:** As the user reaches a waypoint (within 1.2m), the arrow updates to point to the next one.
6. **Arrival:** Guidance ends when the user reaches the final destination node.

## Key Symbols
- `NavigationViewModel.startResolveLoopIfReady()`
- `NavigationViewModel.updateNavigationProgress()`
- `ArrowRenderer.WAYPOINT_ADVANCE_DISTANCE_M`

## Config / Env / Flags
- `MAX_PARALLEL_RESOLVES`: Set to 40 for optimal performance/stability balance.

## Data Structures / Protocols
- `NavigationPhase`: Loading -> SelectBuilding -> Localizing -> PickingDestination -> Navigating -> Arrived.

## Related Tests
N/A

## Related File Dossiers
- [NavigationScreen.kt](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-document/files/app_src_main_java_com_example_vecturai_ui_navigation_NavigationScreen.kt.md)
- [NavigationViewModel.kt](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-document/files/app_src_main_java_com_example_vecturai_ui_navigation_NavigationViewModel.kt.md)

## Risks / Notes
- Navigation requires an internet connection to resolve Cloud Anchors.
- Drift correction happens every 15 seconds to maintain alignment over long walks.
