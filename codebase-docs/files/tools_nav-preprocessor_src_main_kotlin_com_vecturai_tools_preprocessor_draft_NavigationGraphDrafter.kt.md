# File Dossier: NavigationGraphDrafter.kt

## Metadata
- **Path**: `tools/nav-preprocessor/src/main/kotlin/com/vecturai/tools/preprocessor/draft/NavigationGraphDrafter.kt`
- **Type**: Kotlin Source (Drafting Logic)
- **Feature**: `preprocessing`
- **Status**: Mapped

## Role
Generates a draft navigation graph (nodes and edges) from the discovered spatial zones. It uses heuristics to place waypoints and establish connectivity between rooms.

## Public Surface
- `draft(zones: List<Zone>, grid: OccupancyGrid, floorY: Double): DraftNavGraph`: Generates nodes at centroids and connects adjacent zones.

## Main Symbols
- `DraftNode`: Internal node representation with spatial coordinates and zone membership.
- `DraftEdge`: Bidirectional link between nodes with Euclidean distance cost.

## Important Logic
- **Centroid Placement** (L75-88): Places a primary node at the center of every discovered zone. The largest zone (typically a hallway) is marked as a "junction", while others are "room_entry".
- **Waypoint Generation** (L166-227): For the largest zone, it generates extra waypoints along its longest axis (every 3 meters) to ensure pathfinding can navigate through long corridors.
- **Adjacency Connectivity** (L119-158): Connects zones if their centroids are within 8 meters (`adjacencyThreshold`). It specifically links the two closest nodes between the target zones to minimize edge length.

## Used By
- `DraftPipeline.kt`: Step 6 of the extraction flow.

## Notes / Risks
- **Heuristic-Based**: The 8-meter threshold for connectivity is a heuristic; it may fail to connect distant rooms or may incorrectly connect rooms through walls if the scan is sparse.
- **Draft Quality**: This graph is intended as a starting point. Complex layouts (multi-story, winding corridors) will almost always require manual refinement.
