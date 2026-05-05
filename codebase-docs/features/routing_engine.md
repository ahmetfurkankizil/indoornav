# Feature: Navigation Routing Engine

- **Feature Name**: Navigation Routing Engine
- **Purpose**: Calculates optimal paths between points in a building's navigation graph.
- **Implemented In**:
    - `shared/core/src/commonMain/kotlin/com/VecturAI/core/routing/`
- **Used By**:
    - `NavigationSessionCoordinator`
    - `NavigationRepository`
- **Main Flow**:
    1. Receives a request with `fromNodeId` and `toNodeId`.
    2. Uses a `RouteEngine` implementation (default: `DijkstraRouteEngine`) to find the shortest path in the `NavGraph`.
    3. Reconstructs the path as a sequence of `RouteSegment`s.
    4. Generates human-readable instructions (e.g., "Turn left") based on heading changes between segments.
- **Key Symbols**:
    - `RouteEngine` (Interface)
    - `DijkstraRouteEngine` (Shortest-path implementation)
- **Config / Env / Flags**:
    - `AVERAGE_WALKING_SPEED` (1.2 m/s)
    - `TURN_THRESHOLD` (30.0 degrees)
- **Data Structures / Protocols**:
    - `NavGraph`: Adjacency list representation of the building's floor plan.
    - `Route`: The resulting ordered list of segments and metadata.
- **Related Tests**:
    - `shared/core/src/commonTest/kotlin/com/VecturAI/core/routing/DijkstraRouteEngineTest.kt` (TBD in Batch B09 or similar)
- **Related File Dossiers**:
    - [DijkstraRouteEngine.kt](../files/shared_core_src_commonMain_kotlin_com_VecturAI_core_routing_DijkstraRouteEngine.kt.md)
    - [RouteEngine.kt](../files/shared_core_src_commonMain_kotlin_com_VecturAI_core_routing_RouteEngine.kt.md)
- **Risks / Notes**:
    - Assumes the graph is small enough for a simple Dijkstra implementation without a priority queue (uses `minByOrNull` on a filtered map).
    - Heading calculations assume a plan-view orientation (X/Z plane).
