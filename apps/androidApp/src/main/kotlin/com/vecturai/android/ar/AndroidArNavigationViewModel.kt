package com.vecturai.android.ar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ar.core.Frame
import com.google.ar.core.Pose
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
    val trackingStatusLabel: String = "Initializing...",
    val cameraPose: Pose? = null,
    val trackingStatusIcon: TrackingStatusIcon = TrackingStatusIcon.Location,
    val destinationLabel: String = "",
    val arrivalLocationLabel: String = "",
    val arrowCount: Int = 0,
    val isAligned: Boolean = false,
    val hasArrived: Boolean = false,
    val isSimulated: Boolean = false,
    val isAnyToAny: Boolean = false,
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
    val timeoutReasonMessage: String = "Still searching for alignment markers",
    val timeoutHintMessage: String = "Hold steady and make sure the area is well lit.",
    val nextActionIcon: NavigationActionIcon = NavigationActionIcon.Straight,
    val nextActionText: String = "Follow the path",
    val nextActionDistance: Double? = null,
    val isOffPath: Boolean = false,
    val isWrongWay: Boolean = false,
    // Minimap: user's position in building coordinate space
    val userBuildingX: Double = 0.0,
    val userBuildingZ: Double = 0.0,
    val userStableBuildingX: Double = 0.0,
    val userStableBuildingZ: Double = 0.0,
    val userHeadingRad: Double = 0.0,
    // Cross-floor transition
    val pendingTransition: AndroidReviewedPackageLoader.FloorTransition? = null,
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
    internal val routeRenderer: ArRouteRenderer,
    private val hapticManager: AndroidHapticManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ArNavigationUiState())
    val uiState: StateFlow<ArNavigationUiState> = _uiState.asStateFlow()

    private var routePackage: AndroidReviewedPackageLoader.LoadedPackage? = null
    private var entranceMarker: AndroidReviewedPackageLoader.PackageMarker? = null
    private var originRoom: AndroidReviewedPackageLoader.PackageRoom? = null
    private var buildingId: String? = null
    private var userCumulativeDistance = 0.0
    private var destinationThreshold = 1.5
    private var alignmentOffsetX = 0.0
    private var alignmentOffsetY = 0.0
    private var alignmentOffsetZ = 0.0
    private var alignmentRotYDeg = 0.0
    private var alignmentTimeoutJob: Job? = null
    private var lastPoseSampleMs = 0L
    private var lastAlignmentMs = 0L
    private var lastHapticArrowId: String? = null
    private var pendingSimulateAlignment = false
    private var simulateAlignmentRequestedMs = 0L
    // Current camera pose, updated every frame for behind-camera arrow culling
    private var currentCameraPose: Pose? = null
    
    var currentLegIndex = 0
        private set

    fun getRoutePackage(): AndroidReviewedPackageLoader.LoadedPackage? = routePackage

    fun configure(
        routePackage: AndroidReviewedPackageLoader.LoadedPackage,
        entranceMarker: AndroidReviewedPackageLoader.PackageMarker?,
        originRoom: AndroidReviewedPackageLoader.PackageRoom? = null
    ) {
        alignmentTimeoutJob?.cancel()
        routeRenderer.clearArrows()
        markerDetector.fullReset()

        val isSameBuilding = buildingId == routePackage.config.manifest.buildingId
        this.routePackage = routePackage
        this.entranceMarker = entranceMarker
        this.originRoom = originRoom
        this.buildingId = routePackage.config.manifest.buildingId

        lastPoseSampleMs = 0L
        lastAlignmentMs = 0L
        lastHapticArrowId = null
        destinationThreshold = routePackage.config.routeRendering.destinationThresholdMeters
        routeRenderer.configureRendering(
            lookaheadDistanceMeters = routePackage.config.routeRendering.lookaheadDistanceMeters,
            arrowHeightOffsetMeters = routePackage.config.routeRendering.arrowHeightOffsetMeters,
        )

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

        // Reset the base UI state first (clears previous route info)
        _uiState.value = ArNavigationUiState(
            sessionStateLabel = "Initializing AR...",
            destinationLabel = routePackage.destinationName,
            arrivalLocationLabel = routePackage.config.manifest.run {
                formatArrivalLocation(buildingName = buildingName, floorId = floorId)
            },
            totalDistance = routePackage.totalDistance,
            routeStepCount = routePackage.legs.firstOrNull()?.routeNodeIds?.size ?: 0,
            remainingDistance = routePackage.legs.firstOrNull()?.totalDistance ?: 0.0,
            isAnyToAny = true,
        )

        // Force aligned state instantly
        _uiState.update { it.copy(
            isAligned = false,
            sessionStateLabel = "Navigation Active"
        ) }
        
        currentLegIndex = 0

        routeRenderer.placeAllArrows(
            routePackage.legs.firstOrNull()?.arrows ?: emptyList()
        )

        // STABILITY: Don't reset alignment if we are already aligned in the same building
        // UNLESS we have a custom origin room which requires a re-anchor
        if (!isSameBuilding || originRoom != null) {
            if (originRoom != null) {
                // ANY-TO-ANY: Anchor to the selected starting room immediately
                val startNode = routePackage.config.nodes.find { it.id == originRoom.destinationNodeId }
                if (startNode != null) {
                    alignmentOffsetX = -startNode.x
                    alignmentOffsetY = -startNode.y
                    alignmentOffsetZ = -startNode.z
                    alignmentRotYDeg = 0.0
                }
            } else {
                alignmentOffsetX = 0.0
                alignmentOffsetY = 0.0
                alignmentOffsetZ = 0.0
                alignmentRotYDeg = 0.0
            }
            userCumulativeDistance = 0.0
        } else {
            userCumulativeDistance = 0.0
        }

        simulateAlignmentRequestedMs = System.currentTimeMillis()
        pendingSimulateAlignment = true
        println("[AR] FORCING INSTANT START. originRoom: ${originRoom?.id ?: "Entrance (Default)"}")

        startAlignmentTimeout()
    }

    fun isPendingSimulatedAlignment(): Boolean = pendingSimulateAlignment

    fun retryAlignment() {
        alignmentTimeoutJob?.cancel()
        markerDetector.reset()
        _uiState.update {
            it.copy(
                alignmentTimedOut = false,
                sessionStateLabel = "Initializing AR...",
                markerAssetError = null,
                sessionErrorMessage = null,
                timeoutReasonMessage = "Still initializing AR...",
                timeoutHintMessage = "Hold steady for a moment.",
            )
        }
        startAlignmentTimeout()
    }

    fun requestSimulatedAlignment() {
        pendingSimulateAlignment = true
    }

    fun simulateAlignment(frame: Frame) {
        pendingSimulateAlignment = false
        val pkg = routePackage ?: return
        val marker = entranceMarker ?: pkg.config.entranceMarkers.firstOrNull()
        _uiState.update { it.copy(isSimulated = true, isAligned = true, isAnyToAny = true) }
        alignmentTimeoutJob?.cancel()

        val cameraPose = frame.camera.pose

        // Camera forward in world space (OpenGL -Z is forward)
        val camForward = cameraPose.rotateVector(floatArrayOf(0f, 0f, -1f))
        val camX = cameraPose.tx().toDouble()
        val camZ = cameraPose.tz().toDouble()

        // Anchor the start 1.0m in front of the camera so the first arrow is visible
        val offsetPose = cameraPose.compose(Pose.makeTranslation(0f, 0f, -1.0f))
        val arX = offsetPose.tx().toDouble()
        val arY = offsetPose.ty().toDouble()
        val arZ = offsetPose.tz().toDouble()
        val fwdVec = offsetPose.rotateVector(floatArrayOf(0f, 0f, -1f))
        // Flip by 180 degrees to correct backwards arrows
        val arYawDeg = Math.toDegrees(Math.atan2(-fwdVec[0].toDouble(), fwdVec[2].toDouble())) + 180.0

        val room = originRoom
        val startNodeId = room?.destinationNodeId
            ?: marker?.startNodeId
            ?: pkg.config.nodes.find { it.type == "entrance" }?.id
            ?: "n01"

        val startNode = pkg.config.nodes.find { it.id == startNodeId }
        val currentLeg = pkg.legs.getOrNull(currentLegIndex) ?: return
        val nextNodeId = currentLeg.routeNodeIds.getOrNull(1)
        val nextNode = pkg.config.nodes.find { it.id == nextNodeId }
        var buildYawDeg = 0.0
        if (startNode != null && nextNode != null) {
            val dx = nextNode.x - startNode.x
            val dz = nextNode.z - startNode.z
            buildYawDeg = Math.toDegrees(Math.atan2(dx, dz))
        }

        handleMarkerDetected(
            MarkerDetectionEvent(
                markerId = if (room != null) "simulated-start-${room.id}" else (marker?.id ?: "simulated-entrance"),
                entranceNodeId = startNodeId,
                markerArX = arX,
                markerArY = arY - 1.1, // Lower arrows to waist height
                markerArZ = arZ,
                markerArRotationYDeg = arYawDeg,
                markerBuildingX = startNode?.x ?: 0.0,
                markerBuildingY = startNode?.y ?: 0.0,
                markerBuildingZ = startNode?.z ?: 0.0,
                markerBuildingRotationYDeg = buildYawDeg,
                confidence = 1.0,
                role = ArMarkerDetector.MarkerDetectionRole.ENTRANCE
            )
        )

        // Place all arrows and compute which ones are actually in front of the camera.
        // Skip arrows that are behind the camera to fix the "1-2 steps ahead" visual bug.
        routeRenderer.placeAllArrows(currentLeg.arrows)

        userCumulativeDistance = 0.0
        updateUiState(currentLeg.totalDistance)
    }

    fun advanceProgress() {
        val pkg = routePackage ?: return
        val currentLeg = pkg.legs.getOrNull(currentLegIndex) ?: return
        userCumulativeDistance = min(userCumulativeDistance + 2.0, currentLeg.totalDistance)
        val remaining = max(0.0, currentLeg.totalDistance - userCumulativeDistance)
        updateUiState(remaining)
        checkArrival(remaining)
    }

    private fun updateUiState(remaining: Double) {
        val pkg = routePackage ?: return
        val currentLeg = pkg.legs.getOrNull(currentLegIndex) ?: return
        val camPose = currentCameraPose
        if (camPose != null) {
            val camFwd = camPose.rotateVector(floatArrayOf(0f, 0f, -1f))
            routeRenderer.updateVisibility(
                userCumulativeDistance,
                cameraWorldX = camPose.tx(),
                cameraWorldZ = camPose.tz(),
                cameraForwardX = camFwd[0],
                cameraForwardZ = camFwd[2],
            )
        } else {
            routeRenderer.updateVisibility(userCumulativeDistance)
        }
        val next = computeNextAction(remaining)
        _uiState.update {
            it.copy(
                progress = if (currentLeg.totalDistance > 0) userCumulativeDistance / currentLeg.totalDistance else 1.0,
                remainingDistance = remaining,
                distanceToDestination = remaining, // TODO: total remaining across all legs
                arrowCount = routeRenderer.renderedArrowCount,
                nextActionIcon = next.icon,
                nextActionText = next.text,
                nextActionDistance = next.distance,
            )
        }
    }

    fun updateCameraPose(frame: Frame) {
        val pose = frame.camera.pose
        currentCameraPose = pose
        _uiState.update { it.copy(cameraPose = pose) }

        if (pendingSimulateAlignment) {
            val now = System.currentTimeMillis()
            val stableEnough = now - simulateAlignmentRequestedMs > 1500L
            val isTracking = frame.camera.trackingState == TrackingState.TRACKING
            if (stableEnough && isTracking) {
                simulateAlignment(frame)
            }
        }

        if (_uiState.value.isAligned) {
            updateAutoProgress(pose)
        }
    }

    private fun updateAutoProgress(pose: Pose) {
        val pkg = routePackage ?: return

        val worldX = pose.tx().toDouble()
        val worldZ = pose.tz().toDouble()

        val relX = worldX - alignmentOffsetX
        val relZ = worldZ - alignmentOffsetZ

        val rotRad = Math.toRadians(alignmentRotYDeg)
        val cosR = kotlin.math.cos(rotRad)
        val sinR = kotlin.math.sin(rotRad)

        // Convert AR world position to building coordinate space
        val buildingX = relX * cosR + relZ * sinR
        val buildingZ = -relX * sinR + relZ * cosR

        // Compute user heading in building space from camera forward vector
        val fwd = pose.rotateVector(floatArrayOf(0f, 0f, -1f))
        val arFwdX = fwd[0].toDouble()
        val arFwdZ = fwd[2].toDouble()
        val buildingFwdX = arFwdX * cosR + arFwdZ * sinR
        val buildingFwdZ = -arFwdX * sinR + arFwdZ * cosR
        val userHeadingRad = Math.atan2(buildingFwdX, buildingFwdZ)

        var bestDistanceOnPath = userCumulativeDistance
        val currentLeg = pkg.legs[currentLegIndex]
        val nodes = currentLeg.routeNodeIds.mapNotNull { id -> pkg.config.nodes.find { it.id == id } }

        var tempCumulative = 0.0
        var minDistanceToSegmentSq = Double.MAX_VALUE

        for (i in 0 until nodes.size - 1) {
            val a = nodes[i]
            val b = nodes[i + 1]
            val dx = b.x - a.x
            val dz = b.z - a.z
            val segLenSq = dx * dx + dz * dz
            if (segLenSq < 0.001) continue

            var t = ((buildingX - a.x) * dx + (buildingZ - a.z) * dz) / segLenSq
            t = t.coerceIn(0.0, 1.0)

            val projX = a.x + t * dx
            val projZ = a.z + t * dz

            val distSq = (buildingX - projX) * (buildingX - projX) + (buildingZ - projZ) * (buildingZ - projZ)

            if (distSq < minDistanceToSegmentSq && distSq < 16.0) {
                minDistanceToSegmentSq = distSq
                bestDistanceOnPath = tempCumulative + t * kotlin.math.sqrt(segLenSq)
            }
            tempCumulative += kotlin.math.sqrt(segLenSq)
        }

        // Interpolate stable position strictly on the path to prevent minimap jitter
        var d = 0.0
        var stableX = nodes.firstOrNull()?.x ?: 0.0
        var stableZ = nodes.firstOrNull()?.z ?: 0.0
        for (i in 0 until nodes.size - 1) {
            val a = nodes[i]
            val b = nodes[i + 1]
            val dx = b.x - a.x
            val dz = b.z - a.z
            val segLen = kotlin.math.sqrt(dx * dx + dz * dz)
            if (bestDistanceOnPath <= d + segLen + 0.001) {
                val t = if (segLen > 0) (bestDistanceOnPath - d) / segLen else 0.0
                val tClamped = t.coerceIn(0.0, 1.0)
                stableX = a.x + tClamped * dx
                stableZ = a.z + tClamped * dz
                break
            }
            d += segLen
        }

        val isGoingBackwards = bestDistanceOnPath < userCumulativeDistance - 1.5
        val isFarFromPath = minDistanceToSegmentSq > 16.0

        if (System.currentTimeMillis() % 1000 < 50) {
            println("[RouteDebug] Pos: (${buildingX.format(2)}, ${buildingZ.format(2)}) Dist: ${bestDistanceOnPath.format(2)}m Offset: ${sqrt(minDistanceToSegmentSq).format(2)}m")
        }

        if (isFarFromPath || isGoingBackwards) {
            if (!_uiState.value.isOffPath && !_uiState.value.isWrongWay) {
                hapticManager.warning()
            }
        }

        _uiState.update {
            it.copy(
                isOffPath = isFarFromPath,
                isWrongWay = isGoingBackwards,
                userBuildingX = buildingX,
                userBuildingZ = buildingZ,
                userStableBuildingX = stableX,
                userStableBuildingZ = stableZ,
                userHeadingRad = userHeadingRad,
            )
        }

        val maxJump = 1.5
        // SMOOTHING: Use a low-pass filter on bestDistanceOnPath to prevent jittery arrow visibility
        val alpha = 0.3
        val smoothedBest = if (userCumulativeDistance > 0) {
            userCumulativeDistance * (1.0 - alpha) + bestDistanceOnPath * alpha
        } else bestDistanceOnPath

        val safeNextDistance = min(userCumulativeDistance + maxJump, smoothedBest)

        if (!isFarFromPath && safeNextDistance > userCumulativeDistance - 0.01) {
            userCumulativeDistance = max(userCumulativeDistance, safeNextDistance)

            val remaining = max(0.0, currentLeg.totalDistance - userCumulativeDistance)
            updateUiState(remaining)
            checkArrival(remaining)
        }
    }


    private fun Double.format(digits: Int) = "%.${digits}f".format(this)

    fun onFrame(frame: Frame, _width: Int, _height: Int) {
        updateCameraPose(frame)
        markerDetector.processFrame(frame)
        updateTrackingStatus(frame)

        if (pendingSimulateAlignment) {
            simulateAlignment(frame)
        }

        if (_uiState.value.isAligned && !_uiState.value.hasArrived) {
            val now = System.currentTimeMillis()
            if (!_uiState.value.isSimulated && now - lastPoseSampleMs >= 500L) {
                lastPoseSampleMs = now
                // sampleCameraPose removed in favor of updateAutoProgress
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
        lastAlignmentMs = 0L
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
            )
        }
    }

    override fun onCleared() {
        endNavigation()
        super.onCleared()
    }

    fun registerDynamicMarker(index: Int, marker: AndroidReviewedPackageLoader.PackageMarker) {
        val name = marker.referenceImageName ?: "dynamic_marker"
        markerDetector.registerMarker(
            index = index,
            name = name,
            marker = ArMarkerDetector.KnownMarker(
                markerId = marker.id,
                role = ArMarkerDetector.MarkerDetectionRole.ENTRANCE,
                nearestNodeId = marker.startNodeId,
                buildingX = marker.position.x,
                buildingY = marker.position.y,
                buildingZ = marker.position.z,
                buildingRotationYDeg = marker.rotationYDegrees ?: 0.0,
            )
        )
    }

    private fun startAlignmentTimeout() {
        alignmentTimeoutJob?.cancel()
        alignmentTimeoutJob = viewModelScope.launch {
            delay(30_000L)
            if (_uiState.value.isAligned) return@launch
            val reason = markerDetector.detectionFailureReason
            val (title, hint, label) = when (reason) {
                ArMarkerDetector.DetectionFailureReason.NoCandidatesSeen -> Triple(
                    "Still searching for alignment markers",
                    "Hold steady and make sure the area is well lit.",
                    "Searching...",
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
        val now = System.currentTimeMillis()
        
        // If we see a different marker than before, it might be a localization correction
        val isNewMarker = event.markerId != entranceMarker?.id
        
        if (_uiState.value.isAligned && (now - lastAlignmentMs < 5000) && !isNewMarker) {
            return
        }

        alignmentTimeoutJob?.cancel()
        
        // Dynamic re-routing: If we scan a localization marker, we should update our start point
        if (isNewMarker && event.role == ArMarkerDetector.MarkerDetectionRole.CHECKPOINT) {
            // Future: Request server or loader to re-compute path from this specific node
            println("[RouteDebug] Re-localizing at marker: ${event.markerId}")
        }
        val rotDeg = event.markerArRotationYDeg - event.markerBuildingRotationYDeg
        val rotRad = Math.toRadians(rotDeg)
        val cosR = cos(rotRad)
        val sinR = sin(rotRad)
        
        val rotatedBuildingX = event.markerBuildingX * cosR - event.markerBuildingZ * sinR
        val rotatedBuildingZ = event.markerBuildingX * sinR + event.markerBuildingZ * cosR

        val newOffsetX = event.markerArX - rotatedBuildingX
        val newOffsetY = event.markerArY - event.markerBuildingY
        val newOffsetZ = event.markerArZ - rotatedBuildingZ
        val newRotY = rotDeg

        if (_uiState.value.isAligned && lastAlignmentMs > 0L) {
            // STABILITY: For simulated navigation, we LOCK the alignment after the first snap.
            // This prevents "shifting" and "drifting" of arrows during movement.
            if (_uiState.value.isSimulated) return
            
            // Smooth adjustment for real marker tracking corrections
            val alpha = 0.1
            alignmentOffsetX = alignmentOffsetX * (1.0 - alpha) + newOffsetX * alpha
            alignmentOffsetY = alignmentOffsetY * (1.0 - alpha) + newOffsetY * alpha
            alignmentOffsetZ = alignmentOffsetZ * (1.0 - alpha) + newOffsetZ * alpha
            alignmentRotYDeg = alignmentRotYDeg * (1.0 - alpha) + newRotY * alpha
        } else {
            // Snap for initial alignment or after long gap
            alignmentOffsetX = newOffsetX
            alignmentOffsetY = newOffsetY
            alignmentOffsetZ = newOffsetZ
            alignmentRotYDeg = newRotY
            userCumulativeDistance = 0.0
        }

        lastAlignmentMs = now
        val pkg = routePackage ?: return
        val currentLeg = pkg.legs.getOrNull(currentLegIndex) ?: return
        
        routeRenderer.setAlignmentTransform(
            offsetX = alignmentOffsetX,
            offsetY = alignmentOffsetY,
            offsetZ = alignmentOffsetZ,
            rotationYDeg = alignmentRotYDeg,
        )
        routeRenderer.placeAllArrows(currentLeg.arrows)
        routeRenderer.updateVisibility(userCumulativeDistance)
        
        if (!_uiState.value.isAligned) {
            hapticManager.routeStarted()
        }

        val next = computeNextAction(pkg.totalDistance)
        _uiState.update {
            it.copy(
                isAligned = true,
                alignmentTimedOut = false,
                sessionStateLabel = "Navigation Active",
                trackingStatusLabel = "Follow the arrows",
                remainingDistance = pkg.totalDistance,
                isLowConfidence = false,
                arrowCount = routeRenderer.renderedArrowCount,
                nextActionIcon = next.icon,
                nextActionText = next.text,
                nextActionDistance = next.distance,
            )
        }
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
                trackingStatusLabel = if (it.isAligned) "Follow the arrows" else label,
                trackingStatusIcon = icon,
                isLowConfidence = if (it.isAligned) it.isLowConfidence || low else it.isLowConfidence,
            )
        }
        if (_uiState.value.isAligned && low && !wasLow) {
            hapticManager.recentering()
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
                hapticManager.turnImminent()
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
        if (_uiState.value.hasArrived || distanceToDestination > destinationThreshold || _uiState.value.pendingTransition != null) return
        
        val pkg = routePackage ?: return
        
        if (currentLegIndex < pkg.legs.size - 1) {
            val transition = pkg.legs[currentLegIndex].transition
            _uiState.update { it.copy(pendingTransition = transition) }
            hapticManager.arrived()
        } else {
            // STABILITY: Don't cancel everything on arrival. Keep the alignment!
            hapticManager.arrived()
            
            _uiState.update {
                it.copy(
                    hasArrived = true,
                    sessionStateLabel = "Arrived",
                    nextActionIcon = NavigationActionIcon.Destination,
                    nextActionText = "You've reached ${it.destinationLabel}",
                    // We keep isAligned = true so user can pick a new destination immediately
                )
            }
        }
    }

    fun advanceToNextLeg() {
        val pkg = routePackage ?: return
        if (currentLegIndex >= pkg.legs.size - 1) return
        
        currentLegIndex++
        val nextLeg = pkg.legs[currentLegIndex]
        
        userCumulativeDistance = 0.0
        
        _uiState.update { it.copy(
            pendingTransition = null,
            routeStepCount = nextLeg.routeNodeIds.size,
            remainingDistance = nextLeg.totalDistance,
        ) }
        
        routeRenderer.clearArrows()
        routeRenderer.placeAllArrows(nextLeg.arrows)

        hapticManager.recentering() // Feedback for leg transition

        // Re-anchor to the elevator/stair of the new floor
        requestSimulatedAlignment()
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
