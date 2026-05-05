package com.VecturAI.feature.routing

import com.VecturAI.core.domain.NavigationState
import com.VecturAI.core.domain.Room
import com.VecturAI.core.domain.Route
import com.VecturAI.core.repository.BuildingRepository
import com.VecturAI.core.routing.RouteEngine
import com.VecturAI.core.store.AppStore

/**
 * Use case orchestrating the route navigation flow.
 *
 * Coordinates between route computation, state transitions, and
 * the app store to drive the full navigation experience.
 *
 * TODO: Handle entrance marker pose to determine starting node
 * TODO: Implement route recalculation if user goes off-path
 * TODO: Track segment progress during active navigation
 */
class RouteNavigationUseCase(
    private val buildingRepository: BuildingRepository,
    private val routeEngine: RouteEngine,
    private val appStore: AppStore,
) {

    /**
     * Start a navigation session to the given room.
     *
     * Transitions state: Idle → Scanning
     * The AR layer should then detect the entrance marker to proceed.
     *
     * @param buildingId Building to navigate in
     * @param destination Target room
     */
    suspend fun startNavigation(buildingId: String, destination: Room) {
        appStore.selectDestination(destination)
        appStore.updateNavigationState(
            NavigationState.Scanning(
                buildingId = buildingId,
                targetRoom = destination,
            )
        )
    }

    /**
     * Called after the entrance marker is detected and world is aligned.
     *
     * Computes the route and transitions state: Scanning → Navigating
     *
     * @param buildingId Building identifier
     * @param startNodeId Node nearest to the detected entrance marker
     * @param destination Target room
     * @return Computed route, or null if no path found
     */
    suspend fun onMarkerDetected(
        buildingId: String,
        startNodeId: String,
        destination: Room,
    ): Route? {
        appStore.setLoading(true)

        val graph = buildingRepository.getNavGraph(buildingId)
        if (graph == null) {
            appStore.updateNavigationState(
                NavigationState.Error("Navigation graph not available")
            )
            appStore.setLoading(false)
            return null
        }

        // Use the first entry node of the destination room
        val destNodeId = destination.entryNodeIds.firstOrNull()
        if (destNodeId == null) {
            appStore.updateNavigationState(
                NavigationState.Error("Destination has no entry point")
            )
            appStore.setLoading(false)
            return null
        }

        val route = routeEngine.computeRoute(graph, startNodeId, destNodeId)
        if (route != null) {
            val routeWithRoom = route.copy(destinationRoom = destination)
            appStore.updateNavigationState(
                NavigationState.Navigating(route = routeWithRoom)
            )
        } else {
            appStore.updateNavigationState(
                NavigationState.Error("No route found to ${destination.name}")
            )
        }

        appStore.setLoading(false)
        return route
    }

    /**
     * Mark navigation as complete.
     *
     * Transitions state: Navigating → Arrived
     */
    fun onArrived() {
        val current = appStore.navigationState.value
        if (current is NavigationState.Navigating) {
            appStore.updateNavigationState(
                NavigationState.Arrived(
                    route = current.route,
                    destinationRoom = current.route.destinationRoom
                        ?: Room(id = "", name = "Unknown"),
                )
            )
        }
    }

    /**
     * Cancel the current navigation and reset to idle.
     */
    fun cancelNavigation() {
        appStore.updateNavigationState(NavigationState.Idle)
        appStore.selectDestination(null)
    }
}
