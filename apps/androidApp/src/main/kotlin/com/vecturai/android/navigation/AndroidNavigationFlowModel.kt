package com.vecturai.android.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ar.core.Frame
import com.vecturai.android.data.AndroidReviewedPackageLoader
import com.vecturai.android.qr.ArFrameQrScanner
import com.vecturai.android.qr.QRPayload
import com.vecturai.data.remote.RemoteBuildingDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AndroidNavigationFlowModel(
    private val packageLoader: AndroidReviewedPackageLoader,
    private val remoteSource: RemoteBuildingDataSource,
) : ViewModel() {

    sealed interface HomeState {
        data object Home : HomeState
        data class PackageError(val message: String) : HomeState
    }

    private val _state = MutableStateFlow<HomeState>(HomeState.Home)
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        loadPackage()
    }

    fun retryPackageLoad() {
        loadPackage()
    }

    private fun loadPackage() {
        packageLoader.loadReviewedPackage()
            .onSuccess {
                _state.value = HomeState.Home
            }
            .onFailure { error ->
                _state.value = HomeState.PackageError(
                    error.message ?: "Unable to load navigation data",
                )
            }
    }
}

class ArCameraFlowViewModel(
    private val packageLoader: AndroidReviewedPackageLoader,
    private val remoteSource: RemoteBuildingDataSource,
) : ViewModel() {

    sealed interface Phase {
        data object Loading : Phase
        data object QrScan : Phase
        data class EntranceConfirmed(val entranceName: String) : Phase
        data object DestinationSelect : Phase
        data object RoutePreview : Phase
        data object ArNavigation : Phase
        data class FatalError(val message: String) : Phase
    }

    data class SessionData(
        val confirmedEntrance: String = "",
        val selectedRoom: AndroidReviewedPackageLoader.PackageRoom? = null,
        val routePackage: AndroidReviewedPackageLoader.LoadedPackage? = null,
        val validatedEntranceMarker: AndroidReviewedPackageLoader.PackageMarker? = null,
        val reviewedConfig: AndroidReviewedPackageLoader.ReviewedConfig? = null,
        val qrToken: String = "",
    )

    data class RouteSummary(
        val distanceMeters: Double,
        val stepCount: Int,
    )

    private val _phase = MutableStateFlow<Phase>(Phase.Loading)
    val phase: StateFlow<Phase> = _phase.asStateFlow()

    private val _session = MutableStateFlow(SessionData())
    val session: StateFlow<SessionData> = _session.asStateFlow()

    private val _qrError = MutableStateFlow<String?>(null)
    val qrError: StateFlow<String?> = _qrError.asStateFlow()

    private val qrScanner = ArFrameQrScanner { rawValue ->
        onQRScanned(rawValue)
    }

    val availableRooms: List<AndroidReviewedPackageLoader.PackageRoom>
        get() = _session.value.reviewedConfig?.rooms.orEmpty()

    fun routeSummaryFor(room: AndroidReviewedPackageLoader.PackageRoom): RouteSummary? {
        val config = _session.value.reviewedConfig ?: return null
        val routePackage = packageLoader.computeRoute(config, room.id) ?: return null
        return RouteSummary(
            distanceMeters = routePackage.totalDistance,
            stepCount = (routePackage.routeNodeIds.size - 1).coerceAtLeast(1),
        )
    }

    init {
        loadPackageForCameraFlow()
    }

    fun entranceMarkerForSession(): AndroidReviewedPackageLoader.PackageMarker? =
        _session.value.reviewedConfig?.entranceMarkers?.firstOrNull()

    fun onQrFrame(frame: Frame, rotationDegrees: Int) {
        if (_phase.value == Phase.QrScan) {
            qrScanner.scan(frame, rotationDegrees)
        }
    }

    fun clearQRError() {
        _qrError.value = null
        qrScanner.reset()
    }

    fun onQRScanned(rawValue: String): Boolean {
        val payload = QRPayload.parse(rawValue).getOrElse { error ->
            _qrError.value = error.message
            return false
        }

        // Token-based dynamic fetch
        viewModelScope.launch {
            _phase.value = Phase.Loading
            val jsonString = remoteSource.fetchBuildingPackageByToken(payload.token)
            if (jsonString == null) {
                _qrError.value = "Building data not found on server"
                _phase.value = Phase.QrScan
                return@launch
            }

            packageLoader.parseUnifiedPackage(jsonString)
                .onSuccess { config ->
                    _session.value = _session.value.copy(
                        reviewedConfig = config,
                        qrToken = payload.token
                    )
                    confirmEntrance(payload)
                }
                .onFailure { error ->
                    _qrError.value = "Failed to parse package: ${error.message}"
                    _phase.value = Phase.QrScan
                }
        }

        return true
    }

    fun proceedToDestinationSelect() {
        _phase.value = Phase.DestinationSelect
    }

    fun selectDestination(room: AndroidReviewedPackageLoader.PackageRoom) {
        val config = _session.value.reviewedConfig ?: return
        val routePackage = packageLoader.computeRoute(config, room.id) ?: return
        _session.value = _session.value.copy(
            selectedRoom = room,
            routePackage = routePackage,
        )
        _phase.value = Phase.RoutePreview
    }

    fun startNavigation() {
        val session = _session.value
        if (session.selectedRoom != null && session.routePackage != null) {
            _phase.value = Phase.ArNavigation
        }
    }

    fun goBackToDestinationSelect() {
        _session.value = _session.value.copy(
            selectedRoom = null,
            routePackage = null,
        )
        _phase.value = Phase.DestinationSelect
    }

    override fun onCleared() {
        qrScanner.close()
        super.onCleared()
    }

    private fun loadPackageForCameraFlow() {
        // Initial state is just scanning, we fetch the package AFTER the scan
        _phase.value = Phase.QrScan
    }

    private fun confirmEntrance(payload: QRPayload) {
        val config = _session.value.reviewedConfig ?: return
        val marker = config.entranceMarkers.firstOrNull { it.id == payload.entranceId }
        val displayName = marker?.displayName ?: "Entrance"
        _session.value = _session.value.copy(
            confirmedEntrance = displayName,
            validatedEntranceMarker = marker,
        )
        _phase.value = Phase.EntranceConfirmed(displayName)
    }
}
