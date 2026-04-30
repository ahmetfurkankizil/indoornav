package com.vecturai.android.ar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ar.core.Frame
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import com.vecturai.android.data.AndroidReviewedPackageLoader
import com.vecturai.android.data.ArrowPlacementType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

data class ArNavigationUiState(
    val sessionStateLabel: String = "Initializing",
    val trackingStatusLabel: String = "Tracking",
    val trackingStatusIcon: TrackingStatusIcon = TrackingStatusIcon.Location,
    val destinationLabel: String = "",
    val arrivalLocationLabel: String = "",
    val arrowCount: Int = 0,
    val isAligned: Boolean = false,
    val hasArrived: Boolean = false,
    val isSimulated: Boolean = false,
    val totalDistance: Double = 0.0,
    val routeStepCount: Int = 0,
    val progress: Double = 0.0,
    val remainingDistance: Double = 0.0,
    val distanceToDestination: Double = 0.0,
    val isLowConfidence: Boolean = false,
    val alignmentTimedOut: Boolean = false,
    val markerAssetError: String? = null,
    val sessionErrorMessage: String? = null,
    val sessionErrorIsArCoreInstall: Boolean = false,
    val timeoutReasonMessage: String = "No matching entrance poster detected",
    val timeoutHintMessage: String = "",
    val nextActionIcon: NavigationActionIcon = NavigationActionIcon.Straight,
    val nextActionText: String = "Follow the path",
    val nextActionDistance: Double? = null,
    val projectedArrows: List<ArRouteRenderer.ProjectedArrow> = emptyList(),
)

enum class NavigationActionIcon {
    Straight,
    TurnLeft,
    TurnRight,
    UTurn,
    Destination,
}

enum class TrackingStatusIcon {
    Location,
    HoldSteady,
    Eye,
    Recenter,
    Lost,
}

class AndroidArNavigationViewModel(
    private val markerDetector: ArMarkerDetector,
    private val routeRenderer: ArRouteRenderer,
    private val haptics: AndroidHapticManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ArNavigationUiState())
    val uiState: StateFlow<ArNavigationUiState> = _uiState.asStateFlow()

    private var routePackage: AndroidReviewedPackageLoader.LoadedPackage? = null
    private var entranceMarker: AndroidReviewedPackageLoader.PackageMarker? = null
    private var userCumulativeDistance = 0.0
    private var destinationThreshold = 1.5
    private var alignmentOffsetX = 0.0
    private var alignmentOffsetY = 0.0
    private var alignmentOffsetZ = 0.0
    private var alignmentRotYDeg = 0.0
    private var alignmentTimeoutJob: Job? = null
    private var lastPoseSampleMs = 0L
    private var lastProjectedUpdateMs = 0L
    private var lastHapticArrowId: String? = null

    fun configure(
        routePackage: AndroidReviewedPackageLoader.LoadedPackage,
        entranceMarker: AndroidReviewedPackageLoader.PackageMarker?,
    ) {
        alignmentTimeoutJob?.cancel()
        routeRenderer.clearArrows()
        markerDetector.fullReset()

        this.routePackage = routePackage
        this.entranceMarker = entranceMarker
        userCumulativeDistance = 0.0
        alignmentOffsetX = 0.0
        alignmentOffsetY = 0.0
        alignmentOffsetZ = 0.0
        alignmentRotYDeg = 0.0
        lastPoseSampleMs = 0L
        lastProjectedUpdateMs = 0L
        lastHapticArrowId = null
        destinationThreshold = routePackage.config.routeRendering.destinationThresholdMeters
        routeRenderer.configureRendering(routePackage.config.routeRendering.lookaheadDistanceMeters)

        val marker = entranceMarker ?: routePackage.config.entranceMarkers.firstOrNull()
        val markerImageName = marker?.referenceImageName ?: "entrance_marker_main"
        markerDetector.configure(
            markerId = marker?.id ?: "marker-main-entrance",
            markerName = markerImageName,
            nearestNodeId = marker?.startNodeId ?: "n01",
            buildingX = marker?.position?.x ?: 0.0,
            buildingY = marker?.position?.y ?: 1.6,
            buildingZ = marker?.position?.z ?: 0.0,
            buildingRotationYDeg = marker?.rotationYDegrees ?: 0.0,
        )
        markerDetector.onMarkerDetected = { handleMarkerDetected(it) }

        _uiState.value = ArNavigationUiState(
            sessionStateLabel = "Waiting for Poster",
            destinationLabel = routePackage.destinationName,
            arrivalLocationLabel = routePackage.config.manifest.run {
                formatArrivalLocation(buildingName = buildingName, floorId = floorId)
            },
            totalDistance = routePackage.totalDistance,
            routeStepCount = (routePackage.routeNodeIds.size - 1).coerceAtLeast(1),
            remainingDistance = routePackage.totalDistance,
        )
        startAlignmentTimeout()
    }

    fun retryAlignment() {
        alignmentTimeoutJob?.cancel()
        markerDetector.reset()
        _uiState.update {
            it.copy(
                alignmentTimedOut = false,
                sessionStateLabel = "Waiting for Poster",
                markerAssetError = null,
                sessionErrorMessage = null,
                timeoutReasonMessage = "No matching entrance poster detected",
                timeoutHintMessage = "",
            )
        }
        startAlignmentTimeout()
    }

    fun simulateAlignment() {
        val marker = entranceMarker ?: routePackage?.config?.entranceMarkers?.firstOrNull()
        _uiState.update { it.copy(isSimulated = true) }
        alignmentTimeoutJob?.cancel()
        handleMarkerDetected(
            MarkerDetectionEvent(
                markerId = marker?.id ?: "marker-main-entrance",
                entranceNodeId = marker?.startNodeId ?: "n01",
                markerBuildingX = marker?.position?.x ?: 0.0,
                markerBuildingY = marker?.position?.y ?: 1.6,
                markerBuildingZ = marker?.position?.z ?: 0.0,
                markerArX = 0.0,
                markerArY = 0.0,
                markerArZ = -1.0,
                markerArRotationYDeg = 0.0,
                markerBuildingRotationYDeg = marker?.rotationYDegrees ?: 0.0,
                confidence = 1.0,
                role = ArMarkerDetector.MarkerDetectionRole.ENTRANCE,
            )
        )
    }

    fun advanceProgress() {
        val pkg = routePackage ?: return
        userCumulativeDistance = min(userCumulativeDistance + 2.0, pkg.totalDistance)
        val remaining = max(0.0, pkg.totalDistance - userCumulativeDistance)
        routeRenderer.updateVisibility(userCumulativeDistance)
        val next = computeNextAction(remaining)
        _uiState.update {
            it.copy(
                progress = if (pkg.totalDistance > 0) userCumulativeDistance / pkg.totalDistance else 1.0,
                remainingDistance = remaining,
                distanceToDestination = remaining,
                arrowCount = routeRenderer.renderedArrowCount,
                nextActionIcon = next.icon,
                nextActionText = next.text,
                nextActionDistance = next.distance,
            )
        }
        checkArrival(remaining)
    }

    fun onFrame(frame: Frame, width: Int, height: Int) {
        markerDetector.processFrame(frame)
        updateTrackingStatus(frame)

        if (_uiState.value.isAligned && !_uiState.value.hasArrived) {
            val now = System.currentTimeMillis()
            if (!_uiState.value.isSimulated && now - lastPoseSampleMs >= 500L) {
                lastPoseSampleMs = now
                sampleCameraPose(frame)
            }
            if (now - lastProjectedUpdateMs >= 100L) {
                lastProjectedUpdateMs = now
                val viewMatrix = FloatArray(16)
                val projectionMatrix = FloatArray(16)
                frame.camera.getViewMatrix(viewMatrix, 0)
                frame.camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100f)
                val projected = routeRenderer.projectVisibleArrows(
                    viewMatrix = viewMatrix,
                    projectionMatrix = projectionMatrix,
                    width = width,
                    height = height,
                )
                _uiState.update { it.copy(projectedArrows = projected) }
            }
        }
    }

    fun endNavigation() {
        alignmentTimeoutJob?.cancel()
        alignmentTimeoutJob = null
        routeRenderer.clearArrows()
        markerDetector.fullReset()
        userCumulativeDistance = 0.0
        alignmentOffsetX = 0.0
        alignmentOffsetY = 0.0
        alignmentOffsetZ = 0.0
        alignmentRotYDeg = 0.0
        lastPoseSampleMs = 0L
        lastProjectedUpdateMs = 0L
        lastHapticArrowId = null
        _uiState.update {
            it.copy(
                sessionStateLabel = "Ended",
                isAligned = false,
                hasArrived = false,
                isSimulated = false,
                totalDistance = 0.0,
                routeStepCount = 0,
                progress = 0.0,
                remainingDistance = 0.0,
                distanceToDestination = 0.0,
                arrowCount = 0,
                isLowConfidence = false,
                alignmentTimedOut = false,
                markerAssetError = null,
                sessionErrorMessage = null,
                nextActionIcon = NavigationActionIcon.Straight,
                nextActionText = "Follow the path",
                nextActionDistance = null,
                projectedArrows = emptyList(),
            )
        }
    }

    override fun onCleared() {
        endNavigation()
        super.onCleared()
    }

    private fun startAlignmentTimeout() {
        alignmentTimeoutJob?.cancel()
        alignmentTimeoutJob = viewModelScope.launch {
            delay(30_000L)
            if (_uiState.value.isAligned) return@launch
            val reason = markerDetector.detectionFailureReason
            val (title, hint, label) = when (reason) {
                ArMarkerDetector.DetectionFailureReason.NoCandidatesSeen -> Triple(
                    "No entrance poster detected in camera",
                    "Make sure the printed entrance poster is visible, well-lit, and within 1-2 meters.",
                    "No poster detected",
                )

                is ArMarkerDetector.DetectionFailureReason.CandidatesRejected -> Triple(
                    "Detected ${reason.seen} image(s) but none matched the expected entrance poster",
                    "The printed poster must exactly match the bundled reference image. Detected: ${reason.names.joinToString()}",
                    "Poster mismatch",
                )

                ArMarkerDetector.DetectionFailureReason.AssetMissing -> Triple(
                    "Entrance poster asset not found in app bundle",
                    "Rebuild the app with the correct AR reference image.",
                    "Asset missing",
                )
            }
            _uiState.update {
                it.copy(
                    alignmentTimedOut = true,
                    sessionStateLabel = label,
                    timeoutReasonMessage = title,
                    timeoutHintMessage = hint,
                )
            }
        }
    }

    private fun handleMarkerDetected(event: MarkerDetectionEvent) {
        alignmentTimeoutJob?.cancel()
        val rotDeg = event.markerArRotationYDeg - event.markerBuildingRotationYDeg
        val cosR = cos(Math.toRadians(rotDeg))
        val sinR = sin(Math.toRadians(rotDeg))
        val rotatedBuildingX = event.markerBuildingX * cosR + event.markerBuildingZ * sinR
        val rotatedBuildingZ = -event.markerBuildingX * sinR + event.markerBuildingZ * cosR

        alignmentOffsetX = event.markerArX - rotatedBuildingX
        alignmentOffsetY = event.markerArY - event.markerBuildingY
        alignmentOffsetZ = event.markerArZ - rotatedBuildingZ
        alignmentRotYDeg = rotDeg

        val pkg = routePackage ?: return
        if (!_uiState.value.isSimulated) {
            userCumulativeDistance = 0.0
        }

        routeRenderer.setAlignmentTransform(
            offsetX = alignmentOffsetX,
            offsetY = alignmentOffsetY,
            offsetZ = alignmentOffsetZ,
            rotationYDeg = alignmentRotYDeg,
        )
        routeRenderer.placeAllArrows(pkg.arrows)
        routeRenderer.updateVisibility(userCumulativeDistance)
        haptics.routeStarted()

        val next = computeNextAction(pkg.totalDistance)
        _uiState.update {
            it.copy(
                isAligned = true,
                alignmentTimedOut = false,
                sessionStateLabel = "Navigating",
                remainingDistance = pkg.totalDistance,
                isLowConfidence = false,
                arrowCount = routeRenderer.renderedArrowCount,
                nextActionIcon = next.icon,
                nextActionText = next.text,
                nextActionDistance = next.distance,
            )
        }
    }

    private fun sampleCameraPose(frame: Frame) {
        if (frame.camera.trackingState != TrackingState.TRACKING) return
        val pkg = routePackage ?: return
        val pose = frame.camera.pose
        val radians = -alignmentRotYDeg * Math.PI / 180.0
        val cosR = cos(radians)
        val sinR = sin(radians)
        val tx = pose.tx().toDouble() - alignmentOffsetX
        val tz = pose.tz().toDouble() - alignmentOffsetZ
        val bx = tx * cosR + tz * sinR
        val bz = -tx * sinR + tz * cosR

        val routePoints = pkg.routePoints
        if (routePoints.size < 2) return

        val segmentLengths = routePoints.zipWithNext { a, b ->
            val dx = b.first - a.first
            val dz = b.second - a.second
            sqrt(dx * dx + dz * dz)
        }

        var bestDistance = Double.MAX_VALUE
        var bestCumulative = 0.0
        var cumulative = 0.0

        for (i in 0 until routePoints.lastIndex) {
            val a = routePoints[i]
            val b = routePoints[i + 1]
            val dx = b.first - a.first
            val dz = b.second - a.second
            val denom = dx * dx + dz * dz
            if (denom <= 0.0001) {
                cumulative += segmentLengths[i]
                continue
            }
            val t = (((bx - a.first) * dx + (bz - a.second) * dz) / denom).coerceIn(0.0, 1.0)
            val px = a.first + t * dx
            val pz = a.second + t * dz
            val distance = sqrt((bx - px) * (bx - px) + (bz - pz) * (bz - pz))
            if (distance < bestDistance) {
                bestDistance = distance
                bestCumulative = cumulative + t * segmentLengths[i]
            }
            cumulative += segmentLengths[i]
        }

        val dxDest = bx - pkg.destinationPosition.first
        val dzDest = bz - pkg.destinationPosition.second
        val destinationDistance = sqrt(dxDest * dxDest + dzDest * dzDest)

        if (bestCumulative > userCumulativeDistance) {
            userCumulativeDistance = bestCumulative
        }
        val progress = if (pkg.totalDistance > 0) {
            (userCumulativeDistance / pkg.totalDistance).coerceIn(0.0, 1.0)
        } else {
            1.0
        }
        val remaining = max(0.0, pkg.totalDistance - userCumulativeDistance)
        val lowConfidence = bestDistance > 3.0 || frame.camera.trackingState != TrackingState.TRACKING

        routeRenderer.updateVisibility(userCumulativeDistance)
        val next = computeNextAction(destinationDistance)
        _uiState.update {
            it.copy(
                progress = progress,
                remainingDistance = remaining,
                distanceToDestination = destinationDistance,
                isLowConfidence = lowConfidence,
                arrowCount = routeRenderer.renderedArrowCount,
                nextActionIcon = next.icon,
                nextActionText = next.text,
                nextActionDistance = next.distance,
            )
        }

        checkArrival(destinationDistance)
    }

    private fun updateTrackingStatus(frame: Frame) {
        val camera = frame.camera
        val (label, icon, low) = if (camera.trackingState == TrackingState.TRACKING) {
            Triple("Tracking", TrackingStatusIcon.Location, false)
        } else {
            when (camera.trackingFailureReason) {
                TrackingFailureReason.EXCESSIVE_MOTION -> Triple("Hold steady", TrackingStatusIcon.HoldSteady, true)
                TrackingFailureReason.INSUFFICIENT_FEATURES -> Triple("Re-centering...", TrackingStatusIcon.Recenter, true)
                TrackingFailureReason.INSUFFICIENT_LIGHT -> Triple("Hold steady", TrackingStatusIcon.Eye, true)
                TrackingFailureReason.CAMERA_UNAVAILABLE -> Triple("Re-centering...", TrackingStatusIcon.Lost, true)
                else -> Triple("Re-centering...", TrackingStatusIcon.Recenter, true)
            }
        }

        val wasLow = _uiState.value.isLowConfidence
        _uiState.update {
            it.copy(
                trackingStatusLabel = label,
                trackingStatusIcon = icon,
                isLowConfidence = if (it.isAligned) it.isLowConfidence || low else it.isLowConfidence,
            )
        }
        if (_uiState.value.isAligned && low && !wasLow) {
            haptics.recentering()
        }
    }

    private data class NextAction(
        val icon: NavigationActionIcon,
        val text: String,
        val distance: Double?,
    )

    private fun computeNextAction(distanceToDestination: Double): NextAction {
        val pkg = routePackage ?: return NextAction(NavigationActionIcon.Straight, "Follow the path", null)
        val upcoming = pkg.arrows
            .filter { it.cumulativeDistance > userCumulativeDistance && it.type != ArrowPlacementType.FOLLOW }

        val base = upcoming.firstOrNull()?.let { next ->
            val distance = next.cumulativeDistance - userCumulativeDistance
            if (distance < 2.0 && next.id != lastHapticArrowId &&
                next.type in setOf(ArrowPlacementType.TURN_LEFT, ArrowPlacementType.TURN_RIGHT, ArrowPlacementType.U_TURN)
            ) {
                lastHapticArrowId = next.id
                haptics.turnImminent()
            }
            when (next.type) {
                ArrowPlacementType.TURN_LEFT -> NextAction(
                    NavigationActionIcon.TurnLeft,
                    if (distance < 3.0) "Turn left now" else "Turn left ahead",
                    distance,
                )

                ArrowPlacementType.TURN_RIGHT -> NextAction(
                    NavigationActionIcon.TurnRight,
                    if (distance < 3.0) "Turn right now" else "Turn right ahead",
                    distance,
                )

                ArrowPlacementType.U_TURN -> NextAction(
                    NavigationActionIcon.UTurn,
                    if (distance < 3.0) "U-turn now" else "U-turn ahead",
                    distance,
                )

                ArrowPlacementType.DESTINATION -> NextAction(
                    NavigationActionIcon.Destination,
                    if (distance < 3.0) "You're almost there" else "Continue to destination",
                    distance,
                )

                ArrowPlacementType.FOLLOW -> null
            }
        } ?: NextAction(NavigationActionIcon.Straight, "Continue straight", null)

        return if (distanceToDestination < destinationThreshold * 2) {
            NextAction(NavigationActionIcon.Destination, "You're almost there", base.distance)
        } else {
            base
        }
    }

    private fun checkArrival(distanceToDestination: Double) {
        if (_uiState.value.hasArrived || distanceToDestination > destinationThreshold) return
        alignmentTimeoutJob?.cancel()
        routeRenderer.hideAllArrows()
        haptics.arrived()
        _uiState.update {
            it.copy(
                hasArrived = true,
                sessionStateLabel = "Arrived",
                nextActionIcon = NavigationActionIcon.Destination,
                nextActionText = "You've reached ${it.destinationLabel}",
                arrowCount = 0,
                projectedArrows = emptyList(),
            )
        }
    }

    private fun formatArrivalLocation(buildingName: String, floorId: String): String {
        val building = buildingName.trim()
        val floor = floorId.trim()
        val floorLabel = when {
            floor.isBlank() -> ""
            floor.startsWith("floor", ignoreCase = true) -> floor
            else -> "Floor $floor"
        }
        return listOf(building, floorLabel)
            .filter { it.isNotBlank() }
            .joinToString(" - ")
    }
}
