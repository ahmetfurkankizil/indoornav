package com.VecturAI.core.routing

import com.VecturAI.core.domain.NavGraph
import com.VecturAI.core.domain.Route

/**
 * Interface for computing navigation routes within a building.
 *
 * Implementations receive a [NavGraph] and compute the optimal route
 * between two nodes. The interface is designed to be strategy-swappable —
 * the MVP uses shortest-path (Dijkstra), but future versions may use
 * accessibility-weighted, fastest, or scenic routing strategies.
 *
 * Usage:
 * ```kotlin
 * val engine: RouteEngine = DijkstraRouteEngine()
 * val route = engine.computeRoute(graph, "node-entrance", "node-room-42")
 * ```
 */
interface RouteEngine {

    /**
     * Compute a route between two nodes in the navigation graph.
     *
     * @param graph The navigation graph to route on
     * @param fromNodeId Starting node ID
     * @param toNodeId Destination node ID
     * @return Computed [Route] if a path exists, null otherwise
     */
    suspend fun computeRoute(
        graph: NavGraph,
        fromNodeId: String,
        toNodeId: String,
    ): Route?

    /**
     * Human-readable name of this routing strategy.
     * Used for logging and future strategy selection UI.
     */
    val strategyName: String
}
