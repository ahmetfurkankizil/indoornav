package com.vecturai.feature.preview

import com.vecturai.core.domain.Room
import com.vecturai.core.domain.Route
import com.vecturai.core.repository.BuildingRepository
import com.vecturai.core.routing.RouteEngine

/**
 * Use case for previewing a route before starting AR navigation.
 *
 * Provides a 2D route preview with step-by-step instructions,
 * distance, and estimated time — without requiring AR or camera.
 *
 * TODO: Generate 2D map overlay data for route visualization
 * TODO: Provide step-by-step turn cards for the preview screen
 */
class RoutePreviewUseCase(
    private val buildingRepository: BuildingRepository,
    private val routeEngine: RouteEngine,
) {

    /**
     * Compute a preview route for display on the Route Preview screen.
     *
     * @param buildingId Building identifier
     * @param fromNodeId Starting node (usually nearest entrance)
     * @param destination Target room
     * @return Preview data with route details, or null if no path exists
     */
    suspend fun getRoutePreview(
        buildingId: String,
        fromNodeId: String,
        destination: Room,
    ): RoutePreview? {
        val graph = buildingRepository.getNavGraph(buildingId) ?: return null
        val destNodeId = destination.entryNodeIds.firstOrNull() ?: return null
        val route = routeEngine.computeRoute(graph, fromNodeId, destNodeId) ?: return null

        return RoutePreview(
            route = route.copy(destinationRoom = destination),
            stepsDescription = route.segments.map { segment ->
                StepPreview(
                    instruction = segment.instruction,
                    distanceMeters = segment.distanceMeters,
                )
            },
        )
    }
}

/**
 * Preview data for displaying a route without AR.
 */
data class RoutePreview(
    val route: Route,
    val stepsDescription: List<StepPreview>,
) {
    val totalDistanceMeters: Double get() = route.totalDistanceMeters
    val estimatedTimeSeconds: Int get() = route.estimatedTimeSeconds
    val stepCount: Int get() = stepsDescription.size
}

/**
 * A single step in the route preview.
 */
data class StepPreview(
    val instruction: String,
    val distanceMeters: Double,
)
