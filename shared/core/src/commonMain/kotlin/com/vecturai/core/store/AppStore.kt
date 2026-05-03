package com.Vectura AI.core.store

import com.Vectura AI.core.ar.ArSessionState
import com.Vectura AI.core.domain.NavigationState
import com.Vectura AI.core.domain.Room
import com.Vectura AI.core.navigation.ArrivalStatus
import com.Vectura AI.core.navigation.NavigationSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Central application state store.
 *
 * Holds the reactive state for the entire app using [StateFlow].
 * Both the Compose UI and native AR shells observe this store.
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

    // ── Current Session ──────────────────────────────────

    private val _currentSession = MutableStateFlow<NavigationSession?>(null)
    val currentSession: StateFlow<NavigationSession?> = _currentSession.asStateFlow()

    fun updateCurrentSession(session: NavigationSession?) {
        _currentSession.value = session
    }

    // ── Arrival Status ──────────────────────────────────

    private val _arrivalStatus = MutableStateFlow<ArrivalStatus>(ArrivalStatus.NotArrived)
    val arrivalStatus: StateFlow<ArrivalStatus> = _arrivalStatus.asStateFlow()

    fun updateArrivalStatus(status: ArrivalStatus) {
        _arrivalStatus.value = status
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
        _currentSession.value = null
        _arrivalStatus.value = ArrivalStatus.NotArrived
        _selectedBuildingId.value = null
        _selectedDestination.value = null
        _isLoading.value = false
    }
}
