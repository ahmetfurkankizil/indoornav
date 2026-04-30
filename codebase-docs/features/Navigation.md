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
2. **Localization:** The app resolves Cloud Anchors from the graph. `Relocalizer` computes a consensus alignment between the AR session and the graph.
3. **Routing:** `Pathfinder` computes an optimized path.
4. **Guidance:** A 3D arrow appears in front of the user, pointing toward the next waypoint.
5. **Path Projection:** The user's position is projected onto the active path segments (`projectToPath`) to determine the "Current Node" even between anchors.
6. **Rerouting:** If the user deviates significantly from the path (`maybeReroute`), a new path is computed automatically.
7. **Advancement:** As the user reaches a waypoint (within `WAYPOINT_ADVANCE_DISTANCE_M`), the arrow updates to the next one.
8. **Arrival:** Guidance ends when the user reaches the final destination node.

## Key Symbols
- `NavigationViewModel.projectToPath()`
- `NavigationViewModel.maybeReroute()`
- `NavigationViewModel.recomputeGraphToSession()`
- `ArrowRenderer.WAYPOINT_ADVANCE_DISTANCE_M`

## Config / Env / Flags
- `MAX_PARALLEL_RESOLVES`: Number of concurrent anchor resolve attempts.
- `REROUTE_DEVIATION_M`: Distance threshold to trigger automatic rerouting.

## Data Structures / Protocols
- `NavigationPhase`: Loading -> SelectBuilding -> Localizing -> PickingDestination -> Navigating -> Arrived.
- `PathProjection`: Stores the closest segment and perpendicular distance.

## Related Tests
N/A

## Related File Dossiers
- [NavigationScreen.kt](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-docs/files/app_src_main_java_com_example_vecturai_ui_navigation_NavigationScreen.kt.md)
- [NavigationViewModel.kt](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-docs/files/app_src_main_java_com_example_vecturai_ui_navigation_NavigationViewModel.kt.md)

## Risks / Notes
- Navigation requires an internet connection for anchor resolution.
- Drift correction and outlier rejection maintain alignment accuracy during movement.
