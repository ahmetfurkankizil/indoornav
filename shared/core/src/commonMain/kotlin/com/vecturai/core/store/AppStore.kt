package com.vecturai.core.store

import com.vecturai.core.ar.ArSessionState
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
 */
class AppStore {

    // ── Navigation State ───────────────────────────────────

    private val _navigationState = MutableStateFlow<NavigationState>(NavigationState.Idle)
    val navigationState: StateFlow<NavigationState> = _navigationState.asStateFlow()

    fun updateNavigationState(newState: NavigationState) {
        _navigationState.value = newState
    }

    // ── AR Session State ──────────────────────────────────

    private val _arSessionState = MutableStateFlow<ArSessionState>(ArSessionState.Idle)
    val arSessionState: StateFlow<ArSessionState> = _arSessionState.asStateFlow()

    fun updateArSessionState(newState: ArSessionState) {
        _arSessionState.value = newState
    }

    // ── Selected Building ──────────────────────────────────

    private val _selectedBuildingId = MutableStateFlow<String?>(null)
    val selectedBuildingId: StateFlow<String?> = _selectedBuildingId.asStateFlow()

    fun selectBuilding(buildingId: String) {
        _selectedBuildingId.value = buildingId
    }

    // ── Selected Destination ───────────────────────────────

    private val _selectedDestination = MutableStateFlow<Room?>(null)
    val selectedDestination: StateFlow<Room?> = _selectedDestination.asStateFlow()

    fun selectDestination(room: Room?) {
        _selectedDestination.value = room
    }

    // ── Loading State ──────────────────────────────────────

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    // ── Reset ──────────────────────────────────────────────

    fun reset() {
        _navigationState.value = NavigationState.Idle
        _arSessionState.value = ArSessionState.Idle
        _selectedBuildingId.value = null
        _selectedDestination.value = null
        _isLoading.value = false
    }
}

