package com.Vectura AI.tools.admin.service

import com.Vectura AI.tools.admin.model.ExportResult
import com.Vectura AI.tools.admin.model.RoomOverrides
import kotlinx.serialization.json.*
import java.io.File
import java.time.LocalDate

/**
 * Exports a reviewed package from a succeeded draft job's generated artifacts + room overrides.
 * Produces the standard 5-file reviewed package format under `reviewed-package/` in the job directory.
 */
class ReviewedPackageExporter(
    private val jobsBaseDir: String,
) {
    private val json = Json { prettyPrint = true }

    fun export(jobId: String, overrides: RoomOverrides): ExportResult {
        val jobDir = File(jobsBaseDir, jobId)
        val outputDir = File(jobDir, "output")
        val configFile = File(outputDir, "authoring_config.generated.json")
        val warnings = mutableListOf<String>()

        if (!configFile.exists()) {
            return ExportResult(
                jobId = jobId,
                status = "failed",
                warnings = listOf("authoring_config.generated.json not found — cannot export")
            )
        }

        val configJson: JsonObject
        try {
            configJson = Json.parseToJsonElement(configFile.readText()).jsonObject
        } catch (e: Exception) {
            return ExportResult(
                jobId = jobId,
                status = "failed",
                warnings = listOf("Failed to parse authoring_config.generated.json: ${e.message}")
            )
        }

        val exportDir = File(jobDir, "reviewed-package")
        exportDir.mkdirs()

        val files = mutableListOf<String>()

        // 1. manifest.json
        val buildingId = configJson["buildingId"]?.jsonPrimitive?.contentOrNull ?: "draft-unknown"
        val buildingName = configJson["buildingName"]?.jsonPrimitive?.contentOrNull ?: "Draft Building"
        val floorId = configJson["floorId"]?.jsonPrimitive?.contentOrNull ?: "ground"

        val manifest = buildJsonObject {
            put("packageVersion", "1.0")
            put("buildingId", buildingId)
            put("buildingName", buildingName)
            put("floorId", floorId)
            put("reviewStatus", "exported-from-admin")
            put("reviewedBy", "admin-api")
            put("reviewedDate", LocalDate.now().toString())
            put("notes", "Exported from admin draft job $jobId")
            putJsonObject("files") {
                put("rooms", "rooms.json")
                put("navGraph", "nav_graph.json")
                put("entranceMarkers", "entrance_markers.json")
                put("routeRendering", "route_rendering.json")
            }
        }
        File(exportDir, "manifest.json").writeText(json.encodeToString(JsonObject.serializer(), manifest))
        files.add("manifest.json")

        // 2. rooms.json — merge overrides
        val draftRooms = configJson["rooms"]?.jsonArray ?: JsonArray(emptyList())
        val mergedRooms = draftRooms.map { roomEl ->
            val obj = roomEl.jsonObject
            val roomId = obj["id"]?.jsonPrimitive?.contentOrNull ?: return@map roomEl
            val override = overrides.overrides[roomId]

            if (override != null) {
                buildJsonObject {
                    put("id", roomId)
                    put("displayName", override.displayName
                        ?: obj["displayName"]?.jsonPrimitive?.contentOrNull
                        ?: "Unknown")
                    put("destinationNodeId", obj["destinationNodeId"]?.jsonPrimitive?.contentOrNull ?: "")
                    put("category", override.category
                        ?: obj["category"]?.jsonPrimitive?.contentOrNull
                        ?: "unknown")
                    put("description", override.description
                        ?: obj["description"]?.jsonPrimitive?.contentOrNull
                        ?: "")
                }
            } else {
                buildJsonObject {
                    put("id", roomId)
                    put("displayName", obj["displayName"]?.jsonPrimitive?.contentOrNull ?: "Unknown")
                    put("destinationNodeId", obj["destinationNodeId"]?.jsonPrimitive?.contentOrNull ?: "")
                    put("category", obj["category"]?.jsonPrimitive?.contentOrNull ?: "unknown")
                    put("description", obj["description"]?.jsonPrimitive?.contentOrNull ?: "")
                }
            }
        }

        val roomsDoc = buildJsonObject {
            put("rooms", JsonArray(mergedRooms))
        }
        File(exportDir, "rooms.json").writeText(json.encodeToString(JsonObject.serializer(), roomsDoc))
        files.add("rooms.json")

        // 3. nav_graph.json — copy nodes and edges from draft
        val nodes = configJson["nodes"]?.jsonArray ?: JsonArray(emptyList())
        val edges = configJson["edges"]?.jsonArray ?: JsonArray(emptyList())
        val navGraph = buildJsonObject {
            put("nodes", nodes)
            put("edges", edges)
        }
        File(exportDir, "nav_graph.json").writeText(json.encodeToString(JsonObject.serializer(), navGraph))
        files.add("nav_graph.json")

        if (nodes.isEmpty()) warnings.add("nav_graph.json exported with 0 nodes")
        if (edges.isEmpty()) warnings.add("nav_graph.json exported with 0 edges")

        // 4. entrance_markers.json — copy from draft
        val markers = configJson["entranceMarkers"]?.jsonArray ?: JsonArray(emptyList())
        val markersDoc = buildJsonObject {
            put("entranceMarkers", markers)
        }
        File(exportDir, "entrance_markers.json").writeText(json.encodeToString(JsonObject.serializer(), markersDoc))
        files.add("entrance_markers.json")

        if (markers.isEmpty()) warnings.add("entrance_markers.json exported with 0 markers — configure real markers before use")

        // 5. route_rendering.json — copy from draft or use defaults
        val routeRendering = configJson["routeRendering"]?.jsonObject
        if (routeRendering != null) {
            File(exportDir, "route_rendering.json").writeText(json.encodeToString(JsonObject.serializer(), routeRendering))
        } else {
            val defaults = buildJsonObject {
                put("arrowSpacingMeters", 1.5)
                put("lookaheadDistanceMeters", 8.0)
                put("destinationThresholdMeters", 1.5)
                put("turnMarkerThresholdDegrees", 30.0)
                put("arrowHeightOffsetMeters", 0.05)
            }
            File(exportDir, "route_rendering.json").writeText(json.encodeToString(JsonObject.serializer(), defaults))
            warnings.add("route_rendering.json used defaults — no routeRendering found in draft config")
        }
        files.add("route_rendering.json")

        return ExportResult(
            jobId = jobId,
            status = "succeeded",
            files = files,
            exportPath = "reviewed-package/",
            warnings = warnings,
        )
    }
}
