package com.vecturai.android.ar

import com.vecturai.core.domain.NavigationState
import com.vecturai.core.store.AppStore
import kotlinx.coroutines.flow.StateFlow

/**
 * Bridge between shared KMP AR coordinator and native Android AR layer.
 *
 * Follows the shared ArNavigationBridge interface contract.
 * Connects ArNavigationCoordinator events to the native AR components.
 *
 * TODO: Wire to ArNavigationCoordinator via Koin DI once shared framework builds.
 */
class ArBridge {

    /** Current session state label (for debug display). */
    var currentStateLabel: String = "Idle"
        private set

    /** Whether marker alignment is established. */
    var isWorldAligned: Boolean = false
        private set

    /** Current instruction text. */
    var currentInstruction: String = ""
        private set

    /** Arrow count currently renderable. */
    var arrowCount: Int = 0
        private set

    // ── ArNavigationBridge contract ────────────────

    fun startSession(buildingId: String, destinationName: String) {
        currentStateLabel = "Waiting for Marker"
        currentInstruction = "Point camera at the entrance marker"
    }

    fun stopSession() {
        currentStateLabel = "Idle"
        isWorldAligned = false
        currentInstruction = ""
        arrowCount = 0
    }

    fun onAlignmentEstablished(
        offsetX: Double, offsetY: Double, offsetZ: Double,
        rotationYDeg: Double,
    ) {
        isWorldAligned = true
        currentStateLabel = "Aligned"
    }

    fun onRenderableRouteUpdated(arrowCount: Int, instruction: String) {
        this.arrowCount = arrowCount
        currentInstruction = instruction
        currentStateLabel = "Rendering Route"
    }

    fun onSessionStateChanged(stateLabel: String) {
        currentStateLabel = stateLabel
    }

    /**
     * Observe shared navigation state.
     */
    fun observeNavigationState(appStore: AppStore): StateFlow<NavigationState> {
        return appStore.navigationState
    }

    /**
     * Called when entrance marker is detected by ARCore.
     */
    fun onEntranceMarkerDetected(markerPayload: String) {
        isWorldAligned = true
        currentStateLabel = "Navigating"
    }

    fun onDestinationReached() {
        currentStateLabel = "Arrived"
    }
}
