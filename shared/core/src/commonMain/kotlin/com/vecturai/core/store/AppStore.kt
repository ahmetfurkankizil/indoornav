package com.vecturai.core.store

import com.vecturai.core.domain.NavigationState
import com.vecturai.core.domain.Room
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Central application state store.
 *
 * Holds the reactive state for the entire app using [StateFlow].
 * Both the Compose UI and native AR shells observe this store
 * to stay in sync with the current navigation state.
 *
 * This is intentionally kept simple for the MVP — a single observable
 * state holder rather than a full Redux-style architecture. Can be
 * evolved to a more sophisticated state management solution if needed.
 */
class AppStore {

    // ── Navigation State ───────────────────────────────────

    private val _navigationState = MutableStateFlow<NavigationState>(NavigationState.Idle)

    /** Observable navigation state. Observed by both Compose UI and native AR. */
    val navigationState: StateFlow<NavigationState> = _navigationState.asStateFlow()

    /**
     * Update the navigation state.
     *
     * @param newState The new navigation state
     */
    fun updateNavigationState(newState: NavigationState) {
        _navigationState.value = newState
    }

    // ── Selected Building ──────────────────────────────────

    private val _selectedBuildingId = MutableStateFlow<String?>(null)

    /** Currently selected building ID. */
    val selectedBuildingId: StateFlow<String?> = _selectedBuildingId.asStateFlow()

    fun selectBuilding(buildingId: String) {
        _selectedBuildingId.value = buildingId
    }

    // ── Selected Destination ───────────────────────────────

    private val _selectedDestination = MutableStateFlow<Room?>(null)

    /** Currently selected destination room. */
    val selectedDestination: StateFlow<Room?> = _selectedDestination.asStateFlow()

    fun selectDestination(room: Room?) {
        _selectedDestination.value = room
    }

    // ── Loading State ──────────────────────────────────────

    private val _isLoading = MutableStateFlow(false)

    /** Whether a long-running operation is in progress. */
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    // ── Reset ──────────────────────────────────────────────

    /**
     * Reset all state to initial values.
     * Called when starting a new navigation session or on logout.
     */
    fun reset() {
        _navigationState.value = NavigationState.Idle
        _selectedBuildingId.value = null
        _selectedDestination.value = null
        _isLoading.value = false
    }
}
