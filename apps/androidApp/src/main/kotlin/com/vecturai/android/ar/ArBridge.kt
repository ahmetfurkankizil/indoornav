package com.vecturai.android.ar

import com.vecturai.core.domain.NavigationState
import com.vecturai.core.store.AppStore
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

/**
 * Bridge between shared KMP navigation state and the native Android AR layer.
 *
 * This class translates the platform-agnostic [NavigationState] into data
 * that the ARCore renderer can consume (e.g., world-space arrow positions,
 * current instruction text, progress percentage).
 *
 * TODO: Accept AppStore via DI (Koin) and expose derived AR-specific state
 * TODO: Convert route segments from nav-graph coordinates to ARCore world coordinates
 * TODO: Provide entrance marker detection callbacks back to shared state
 */
class ArBridge {

    /**
     * Observes the shared navigation state and maps it to AR-renderable data.
     *
     * TODO: Implement coordinate transformation:
     *   - navGraphPosition (from shared RouteSegment) → ARCore world anchor pose
     *   - Account for entrance marker alignment offset
     */
    fun observeNavigationState(appStore: AppStore): StateFlow<NavigationState> {
        return appStore.navigationState
    }

    /**
     * Called when the entrance marker is successfully detected by ARCore.
     *
     * TODO: Extract pose from ARCore anchor
     * TODO: Notify shared state that alignment is complete
     * TODO: Trigger route rendering
     *
     * @param markerPayload The QR code content decoded from the entrance marker
     */
    fun onEntranceMarkerDetected(markerPayload: String) {
        // TODO: Parse marker payload, update shared NavigationState to Navigating
    }

    /**
     * Called when the user reaches the destination in AR view.
     *
     * TODO: Update shared state to Arrived
     * TODO: Record visit in history
     */
    fun onDestinationReached() {
        // TODO: Transition NavigationState to Arrived
    }
}
