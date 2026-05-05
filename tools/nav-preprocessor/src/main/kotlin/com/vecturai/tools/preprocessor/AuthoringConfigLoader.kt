package com.vecturai.tools.preprocessor

import com.vecturai.tools.preprocessor.model.AuthoringConfig
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Loads and deserializes the authoring config JSON file.
 */
class AuthoringConfigLoader {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
        prettyPrint = false
    }

    /**
     * Load and parse authoring config from a JSON file.
     *
     * @throws ValidationException on parse errors or missing file
     */
    fun load(configPath: String): AuthoringConfig {
        val file = File(configPath)
        if (!file.exists()) {
            throw ValidationException("Authoring config not found: $configPath")
        }
        if (!file.canRead()) {
            throw ValidationException("Authoring config not readable: $configPath")
        }

        val content = file.readText()
        if (content.isBlank()) {
            throw ValidationException("Authoring config is empty")
        }

        return try {
            json.decodeFromString<AuthoringConfig>(content)
        } catch (e: Exception) {
            throw ValidationException("Failed to parse authoring config: ${e.message}")
        }
    }

    /**
     * Validate basic structural requirements of the config.
     *
     * @return List of validation errors (empty = valid)
     */
    fun validateStructure(config: AuthoringConfig): List<String> {
        val errors = mutableListOf<String>()

        if (config.buildingId.isBlank()) errors.add("buildingId must not be blank")
        if (config.buildingName.isBlank()) errors.add("buildingName must not be blank")
        if (config.floorId.isBlank()) errors.add("floorId must not be blank")
        if (config.asset.glbFile.isBlank()) errors.add("asset.glbFile must not be blank")
        if (config.entranceMarkers.isEmpty()) errors.add("At least one entrance marker required")
        if (config.nodes.size < 2) errors.add("At least 2 nodes required")
        if (config.edges.isEmpty()) errors.add("At least 1 edge required")
        if (config.rooms.isEmpty()) errors.add("At least 1 room required")

        // Check for blank IDs
        config.nodes.forEachIndexed { i, n ->
            if (n.id.isBlank()) errors.add("Node[$i] has blank id")
        }
        config.edges.forEachIndexed { i, e ->
            if (e.id.isBlank()) errors.add("Edge[$i] has blank id")
            if (e.from.isBlank()) errors.add("Edge[$i] has blank 'from'")
            if (e.to.isBlank()) errors.add("Edge[$i] has blank 'to'")
        }
        config.rooms.forEachIndexed { i, r ->
            if (r.id.isBlank()) errors.add("Room[$i] has blank id")
            if (r.displayName.isBlank()) errors.add("Room[$i] has blank displayName")
            if (r.destinationNodeId.isBlank()) errors.add("Room[$i] has blank destinationNodeId")
        }
        config.entranceMarkers.forEachIndexed { i, m ->
            if (m.id.isBlank()) errors.add("Marker[$i] has blank id")
            if (m.startNodeId.isBlank()) errors.add("Marker[$i] has blank startNodeId")
            if (m.physicalWidthMeters <= 0) errors.add("Marker[$i] physicalWidthMeters must be > 0")
            if (m.physicalHeightMeters <= 0) errors.add("Marker[$i] physicalHeightMeters must be > 0")
            val validBases = setOf("+x", "-x", "+z", "-z")
            if (m.forwardBasis !in validBases) {
                errors.add("Marker[$i] forwardBasis must be one of $validBases, got '${m.forwardBasis}'")
            }
        }

        // Rendering config ranges
        val r = config.routeRendering
        if (r.arrowSpacingMeters < 0.3) errors.add("arrowSpacingMeters must be >= 0.3")
        if (r.lookaheadDistanceMeters < 1.0) errors.add("lookaheadDistanceMeters must be >= 1.0")
        if (r.destinationThresholdMeters < 0.3) errors.add("destinationThresholdMeters must be >= 0.3")
        if (r.turnMarkerThresholdDegrees < 5.0) errors.add("turnMarkerThresholdDegrees must be >= 5.0")
        if (r.arrowHeightOffsetMeters < 0.0) errors.add("arrowHeightOffsetMeters must be >= 0.0")

        return errors
    }
}
