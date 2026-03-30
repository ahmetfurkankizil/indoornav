package com.vecturai.tools.admin.service

import com.vecturai.tools.admin.model.*
import kotlinx.serialization.json.*
import java.io.File

/**
 * Extracts a structured summary from the generated draft artifacts.
 * Reads authoring_config.generated.json, generation_metadata.json, and geometry_stats.json.
 * Fails gracefully — missing or unparseable files produce warnings, not errors.
 */
class DraftSummaryExtractor {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun extract(
        jobId: String,
        jobStatus: JobStatus,
        outputDir: File,
        overrides: RoomOverrides = RoomOverrides(),
    ): DraftSummary {
        val warnings = mutableListOf<String>()

        val configFile = File(outputDir, "authoring_config.generated.json")
        val metadataFile = File(outputDir, "generation_metadata.json")
        val statsFile = File(outputDir, "geometry_stats.json")
        val occupancyFile = File(outputDir, "occupancy_debug.svg")
        val graphFile = File(outputDir, "draft_graph_debug.svg")

        val availability = ArtifactAvailability(
            hasOccupancyPreview = occupancyFile.exists(),
            hasGraphPreview = graphFile.exists(),
            hasAuthoringConfig = configFile.exists(),
            hasGenerationMetadata = metadataFile.exists(),
            hasGeometryStats = statsFile.exists(),
        )

        // Parse authoring config
        var buildingId: String? = null
        var buildingName: String? = null
        var floorId: String? = null
        var counts = GraphCounts()
        var rooms = emptyList<DraftRoom>()

        if (configFile.exists()) {
            try {
                val configJson = Json.parseToJsonElement(configFile.readText()).jsonObject

                buildingId = configJson["buildingId"]?.jsonPrimitive?.contentOrNull
                buildingName = configJson["buildingName"]?.jsonPrimitive?.contentOrNull
                floorId = configJson["floorId"]?.jsonPrimitive?.contentOrNull

                val nodes = configJson["nodes"]?.jsonArray?.size ?: 0
                val edges = configJson["edges"]?.jsonArray?.size ?: 0
                val entranceMarkers = configJson["entranceMarkers"]?.jsonArray?.size ?: 0
                val checkpointMarkers = configJson["checkpointMarkers"]?.jsonArray?.size ?: 0

                val roomsArray = configJson["rooms"]?.jsonArray
                rooms = roomsArray?.map { roomEl ->
                    val obj = roomEl.jsonObject
                    val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: "unknown"
                    val override = overrides.overrides[id]
                    DraftRoom(
                        id = id,
                        displayName = override?.displayName
                            ?: obj["displayName"]?.jsonPrimitive?.contentOrNull
                            ?: "Unknown",
                        category = override?.category
                            ?: obj["category"]?.jsonPrimitive?.contentOrNull,
                        destinationNodeId = obj["destinationNodeId"]?.jsonPrimitive?.contentOrNull,
                        description = override?.description
                            ?: obj["description"]?.jsonPrimitive?.contentOrNull,
                    )
                } ?: emptyList()

                counts = GraphCounts(
                    nodes = nodes,
                    edges = edges,
                    rooms = rooms.size,
                    entranceMarkers = entranceMarkers,
                    checkpointMarkers = checkpointMarkers,
                )

                if (rooms.isEmpty()) {
                    warnings.add("No rooms found in authoring config")
                }
                if (nodes == 0) {
                    warnings.add("No nodes found in authoring config")
                }
            } catch (e: Exception) {
                warnings.add("Failed to parse authoring_config.generated.json: ${e.message}")
            }
        } else {
            warnings.add("authoring_config.generated.json not found")
        }

        // Parse generation metadata
        var genMetadata: GenerationMetadataSummary? = null
        if (metadataFile.exists()) {
            try {
                val metaJson = Json.parseToJsonElement(metadataFile.readText()).jsonObject
                genMetadata = GenerationMetadataSummary(
                    generatedBy = metaJson["generatedBy"]?.jsonPrimitive?.contentOrNull,
                    timestamp = metaJson["timestamp"]?.jsonPrimitive?.contentOrNull,
                    confidence = metaJson["confidence"]?.jsonPrimitive?.contentOrNull,
                    editRequired = metaJson["editRequired"]?.jsonPrimitive?.booleanOrNull,
                    floorY = metaJson["floorY"]?.jsonPrimitive?.doubleOrNull,
                    floorConfidence = metaJson["floorConfidence"]?.jsonPrimitive?.doubleOrNull,
                )
            } catch (e: Exception) {
                warnings.add("Failed to parse generation_metadata.json: ${e.message}")
            }
        }

        // Parse geometry stats
        var geoStats: GeometryStatsSummary? = null
        if (statsFile.exists()) {
            try {
                val statsJson = Json.parseToJsonElement(statsFile.readText()).jsonObject
                val bb = statsJson["boundingBox"]?.jsonObject
                val grid = statsJson["occupancyGrid"]?.jsonObject
                val zones = statsJson["zones"]?.jsonArray

                geoStats = GeometryStatsSummary(
                    totalVertices = statsJson["totalVertices"]?.jsonPrimitive?.intOrNull,
                    meshCount = statsJson["meshCount"]?.jsonPrimitive?.intOrNull,
                    extentX = bb?.get("extentX")?.jsonPrimitive?.doubleOrNull,
                    extentY = bb?.get("extentY")?.jsonPrimitive?.doubleOrNull,
                    extentZ = bb?.get("extentZ")?.jsonPrimitive?.doubleOrNull,
                    occupancyGridWidth = grid?.get("width")?.jsonPrimitive?.intOrNull,
                    occupancyGridHeight = grid?.get("height")?.jsonPrimitive?.intOrNull,
                    occupancyGridCellSize = grid?.get("cellSize")?.jsonPrimitive?.doubleOrNull,
                    zoneCount = zones?.size,
                )
            } catch (e: Exception) {
                warnings.add("Failed to parse geometry_stats.json: ${e.message}")
            }
        }

        // Artifact-level warnings
        if (!occupancyFile.exists()) {
            warnings.add("Occupancy preview (occupancy_debug.svg) not available")
        }
        if (!graphFile.exists()) {
            warnings.add("Graph preview (draft_graph_debug.svg) not available")
        }

        return DraftSummary(
            jobId = jobId,
            status = jobStatus,
            buildingId = buildingId,
            buildingName = buildingName,
            floorId = floorId,
            artifactAvailability = availability,
            counts = counts,
            rooms = rooms,
            generationMetadata = genMetadata,
            geometryStats = geoStats,
            warnings = warnings,
        )
    }
}
