package com.VecturAI.core.ar

import com.VecturAI.core.domain.CheckpointMarker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

/**
 * Coordinates checkpoint-based alignment corrections.
 *
 * When the native AR layer observes a known checkpoint marker, it calls
 * [onCheckpointObserved] with the observation data. This coordinator:
 * 1. Resolves the marker's known building-local reference pose
 * 2. Computes what the alignment transform SHOULD be based on the observation
 * 3. Compares against the current alignment to compute a correction delta
 * 4. Applies a BOUNDED correction (max translation / rotation)
 * 5. Updates the alignment transform and notifies listeners
 *
 * Design: conservative, bounded corrections to avoid jarring jumps.
 */
class CorrectionCoordinator(
    /** Maximum translation correction per observation (meters). */
    private val maxTranslationMeters: Double = 2.0,
    /** Maximum rotation correction per observation (degrees). */
    private val maxRotationDegrees: Double = 15.0,
    /** Minimum confidence to accept an observation. */
    private val minObservationConfidence: Double = 0.3,
    /** Minimum interval between corrections from the same marker (ms). */
    private val deduplicationIntervalMs: Long = 5000L,
) {

    private var knownCheckpoints: Map<String, CheckpointMarker> = emptyMap()
    private var currentAlignment: AlignmentTransform? = null

    // Correction tracking
    private var _correctionCount: Int = 0
    val correctionCount: Int get() = _correctionCount

    private var _lastCorrectionTimeMs: Long = 0L
    val lastCorrectionTimeMs: Long get() = _lastCorrectionTimeMs

    private var _totalCorrectionMagnitude: Double = 0.0
    val totalCorrectionMagnitude: Double get() = _totalCorrectionMagnitude

    /** Last marker observation time per marker ID (for deduplication). */
    private val lastObservationTimes = mutableMapOf<String, Long>()

    private val _confidenceState = MutableStateFlow(NavigationConfidenceState())
    val confidenceState: StateFlow<NavigationConfidenceState> = _confidenceState.asStateFlow()

    /**
     * Configure with checkpoint markers from the building package.
     *
     * Call this during session preparation, after route is computed.
     */
    fun configure(
        checkpoints: List<CheckpointMarker>,
        initialAlignment: AlignmentTransform,
    ) {
        knownCheckpoints = checkpoints.associateBy { it.id }
        currentAlignment = initialAlignment
        _correctionCount = 0
        _lastCorrectionTimeMs = 0L
        _totalCorrectionMagnitude = 0.0
        lastObservationTimes.clear()

        _confidenceState.value = NavigationConfidenceState(
            alignmentConfidence = AlignmentConfidence.HIGH,
            progressConfidence = ProgressConfidence.RELIABLE,
            offRouteStatus = OffRouteStatus.ON_ROUTE,
            recommendation = RecoveryRecommendation.CONTINUE,
        )
    }

    /**
     * Process a checkpoint marker observation from the native AR layer.
     *
     * @return CorrectionResult describing whether correction was applied and by how much
     */
    fun onCheckpointObserved(event: MarkerObservationEvent): CorrectionResult {
        // 1. Validate marker exists
        val checkpoint = knownCheckpoints[event.markerId]
            ?: return CorrectionResult(applied = false, reason = "Unknown marker: ${event.markerId}")

        // 2. Check confidence threshold
        if (event.confidence < minObservationConfidence) {
            return CorrectionResult(applied = false, reason = "Low confidence: ${event.confidence}")
        }

        // 3. Deduplication: skip if we saw this marker very recently
        val lastTime = lastObservationTimes[event.markerId]
        if (lastTime != null && event.timestampMs > 0 && event.timestampMs - lastTime < deduplicationIntervalMs) {
            return CorrectionResult(applied = false, reason = "Too soon since last observation")
        }

        // 4. Need a current alignment to correct
        val currentAlign = currentAlignment
            ?: return CorrectionResult(applied = false, reason = "No current alignment")

        // 5. Compute what alignment SHOULD be based on this observation
        val observedResult = MarkerAlignmentResult(
            markerId = checkpoint.id,
            entranceNodeId = checkpoint.nearestNodeId,
            markerBuildingX = checkpoint.positionX,
            markerBuildingY = checkpoint.positionY,
            markerBuildingZ = checkpoint.positionZ,
            markerArX = event.arX,
            markerArY = event.arY,
            markerArZ = event.arZ,
            markerArRotationYDeg = event.arRotationYDeg,
            markerBuildingRotationYDeg = checkpoint.rotationYDegrees,
            confidence = event.confidence,
        )
        val observedAlignment = AlignmentTransform.fromMarkerAlignment(observedResult)

        // 6. Compute correction delta
        val deltaX = observedAlignment.offsetX - currentAlign.offsetX
        val deltaY = observedAlignment.offsetY - currentAlign.offsetY
        val deltaZ = observedAlignment.offsetZ - currentAlign.offsetZ
        val deltaRot = normalizeAngle(observedAlignment.rotationYDeg - currentAlign.rotationYDeg)

        val translationMag = sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)
        val rotationMag = abs(deltaRot)

        // 7. Bound the correction
        val translationScale = if (translationMag > maxTranslationMeters && translationMag > 0) {
            maxTranslationMeters / translationMag
        } else 1.0

        val boundedDeltaX = deltaX * translationScale
        val boundedDeltaY = deltaY * translationScale
        val boundedDeltaZ = deltaZ * translationScale
        val boundedDeltaRot = deltaRot.coerceIn(-maxRotationDegrees, maxRotationDegrees)

        val boundedTranslationMag = sqrt(
            boundedDeltaX * boundedDeltaX +
                    boundedDeltaY * boundedDeltaY +
                    boundedDeltaZ * boundedDeltaZ
        )

        // 8. Apply correction
        val correctedAlignment = AlignmentTransform(
            offsetX = currentAlign.offsetX + boundedDeltaX,
            offsetY = currentAlign.offsetY + boundedDeltaY,
            offsetZ = currentAlign.offsetZ + boundedDeltaZ,
            rotationYDeg = currentAlign.rotationYDeg + boundedDeltaRot,
        )

        currentAlignment = correctedAlignment
        _correctionCount++
        _lastCorrectionTimeMs = event.timestampMs
        _totalCorrectionMagnitude += boundedTranslationMag
        lastObservationTimes[event.markerId] = event.timestampMs

        // 9. Update confidence
        updateConfidenceAfterCorrection(boundedTranslationMag, event)

        return CorrectionResult(
            applied = true,
            translationDeltaMeters = boundedTranslationMag,
            rotationDeltaDeg = abs(boundedDeltaRot),
            newAlignment = correctedAlignment,
            reason = "Checkpoint correction applied",
        )
    }

    /**
     * Get the current (possibly corrected) alignment transform.
     */
    fun getCurrentAlignment(): AlignmentTransform? = currentAlignment

    /**
     * Update alignment directly (e.g., on entrance marker re-scan).
     */
    fun updateAlignment(alignment: AlignmentTransform) {
        currentAlignment = alignment
        _confidenceState.value = _confidenceState.value.copy(
            alignmentConfidence = AlignmentConfidence.HIGH,
        )
    }

    /**
     * Reset all correction state (e.g., on new session).
     */
    fun reset() {
        knownCheckpoints = emptyMap()
        currentAlignment = null
        _correctionCount = 0
        _lastCorrectionTimeMs = 0L
        _totalCorrectionMagnitude = 0.0
        lastObservationTimes.clear()
        _confidenceState.value = NavigationConfidenceState()
    }

    private fun updateConfidenceAfterCorrection(magnitude: Double, event: MarkerObservationEvent) {
        val alignment = if (magnitude < 0.5) AlignmentConfidence.HIGH
        else if (magnitude < 1.5) AlignmentConfidence.MODERATE
        else AlignmentConfidence.LOW

        _confidenceState.value = _confidenceState.value.copy(
            alignmentConfidence = alignment,
            lastCorrectionTimeMs = event.timestampMs,
            correctionCount = _correctionCount,
            correctionMagnitudeMeters = _totalCorrectionMagnitude,
            lastMarkerIdSeen = event.markerId,
            lastMarkerRole = MarkerRole.CHECKPOINT,
        )
    }

    private fun normalizeAngle(deg: Double): Double {
        var a = deg % 360.0
        if (a > 180.0) a -= 360.0
        if (a < -180.0) a += 360.0
        return a
    }
}
