package com.vecturai.android.navigation

import androidx.lifecycle.ViewModel
import com.vecturai.android.data.AndroidReviewedPackageLoader
import com.vecturai.android.qr.QRPayload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidNavigationFlowModel(
    private val packageLoader: AndroidReviewedPackageLoader,
) : ViewModel() {

    sealed interface FlowState {
        data object Home : FlowState
        data object QrScan : FlowState
        data class EntranceConfirmed(val entranceName: String) : FlowState
        data object DestinationSelect : FlowState
        data object RoutePreview : FlowState
        data object ArNavigation : FlowState
        data class PackageError(val message: String) : FlowState
    }

    data class SessionData(
        val confirmedEntrance: String = "",
        val selectedRoom: AndroidReviewedPackageLoader.PackageRoom? = null,
        val routePackage: AndroidReviewedPackageLoader.LoadedPackage? = null,
        val validatedEntranceMarker: AndroidReviewedPackageLoader.PackageMarker? = null,
        val reviewedConfig: AndroidReviewedPackageLoader.ReviewedConfig? = null,
    )

    private val _state = MutableStateFlow<FlowState>(FlowState.Home)
    val state: StateFlow<FlowState> = _state.asStateFlow()

    private val _session = MutableStateFlow(SessionData())
    val session: StateFlow<SessionData> = _session.asStateFlow()

    private val _qrError = MutableStateFlow<String?>(null)
    val qrError: StateFlow<String?> = _qrError.asStateFlow()

    val availableRooms: List<AndroidReviewedPackageLoader.PackageRoom>
        get() = _session.value.reviewedConfig?.rooms.orEmpty()

    init {
        loadPackage()
    }

    fun loadPackage() {
        packageLoader.loadReviewedPackage()
            .onSuccess { config ->
                _session.value = _session.value.copy(reviewedConfig = config)
                _state.value = FlowState.Home
            }
            .onFailure { error ->
                _session.value = SessionData()
                _state.value = FlowState.PackageError(error.message ?: "Unable to load navigation data")
            }
    }

    fun startQRScan() {
        if (_session.value.reviewedConfig != null) {
            _qrError.value = null
            _state.value = FlowState.QrScan
        }
    }

    fun onQRScanned(rawValue: String) {
        val payload = QRPayload.parse(rawValue).getOrElse { error ->
            _qrError.value = error.message
            return
        }

        val config = _session.value.reviewedConfig ?: run {
            _qrError.value = QRPayload.PayloadError.NotJSON.message
            return
        }

        payload.validate(config)?.let { error ->
            _qrError.value = error.message
            return
        }

        confirmEntrance(payload)
    }

    fun clearQRError() {
        _qrError.value = null
    }

    fun confirmEntrance(payload: QRPayload) {
        val config = _session.value.reviewedConfig ?: return
        val marker = config.entranceMarkers.firstOrNull { it.id == payload.entranceId }
        val displayName = marker?.displayName ?: "Entrance"
        _session.value = _session.value.copy(
            confirmedEntrance = displayName,
            validatedEntranceMarker = marker,
        )
        _state.value = FlowState.EntranceConfirmed(displayName)
    }

    fun confirmEntrance(name: String) {
        val marker = _session.value.reviewedConfig?.entranceMarkers?.firstOrNull()
        _session.value = _session.value.copy(
            confirmedEntrance = name,
            validatedEntranceMarker = marker,
        )
        _state.value = FlowState.EntranceConfirmed(name)
    }

    fun proceedToDestinationSelect() {
        _state.value = FlowState.DestinationSelect
    }

    fun selectDestination(room: AndroidReviewedPackageLoader.PackageRoom) {
        val config = _session.value.reviewedConfig ?: return
        val routePackage = packageLoader.computeRoute(config, room.id)
        _session.value = _session.value.copy(
            selectedRoom = room,
            routePackage = routePackage,
        )
        _state.value = FlowState.RoutePreview
    }

    fun startNavigation() {
        val session = _session.value
        if (session.selectedRoom != null && session.routePackage != null) {
            _state.value = FlowState.ArNavigation
        }
    }

    fun endNavigation() {
        _session.value = _session.value.copy(
            confirmedEntrance = "",
            selectedRoom = null,
            routePackage = null,
            validatedEntranceMarker = null,
        )
        _qrError.value = null
        _state.value = FlowState.Home
    }

    fun goBackToDestinationSelect() {
        _session.value = _session.value.copy(
            selectedRoom = null,
            routePackage = null,
        )
        _state.value = FlowState.DestinationSelect
    }

    fun retryPackageLoad() {
        loadPackage()
    }
}
