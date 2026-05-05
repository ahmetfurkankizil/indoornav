package com.vecturai.tools.preprocessor.draft

import com.vecturai.tools.preprocessor.analysis.FloorPlaneEstimator
import com.vecturai.tools.preprocessor.analysis.ZoneSuggester
import com.vecturai.tools.preprocessor.glb.BoundingBox3D
import com.vecturai.tools.preprocessor.model.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant

/**
 * Assembles a complete [AuthoringConfig] from draft pipeline results
 * and writes `authoring_config.generated.json`.
 *
 * Uses neutral zone labels, placeholder marker, and default rendering config.
 * All outputs are marked with low confidence and `editRequired: true`.
 */
class DraftConfigGenerator {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    @Serializable
    data class GeneratedMetadata(
        val generatedBy: String = "nav-preprocessor",
        val timestamp: String,
        val confidence: String = "low",
        val editRequired: Boolean = true,
        val floorY: Double,
        val floorConfidence: Double,
        val zoneCount: Int,
        val nodeCount: Int,
        val edgeCount: Int,
        val notes: String = "Auto-generated draft — review and edit zone labels, node positions, and room details before use.",
    )

    /**
     * Generate the draft config and write it to disk.
     *
     * @return The path to the generated config file
     */
    fun generate(
        glbFileName: String,
        draftGraph: NavigationGraphDrafter.DraftNavGraph,
        zones: List<ZoneSuggester.Zone>,
        floorEstimate: FloorPlaneEstimator.FloorEstimate,
        boundingBox: BoundingBox3D?,
        outputDir: String,
    ): String {
        val outDir = File(outputDir)
        outDir.mkdirs()

        // Build AuthoringConfig from draft data
        val nodes = draftGraph.nodes.map { n ->
            AuthoringNode(
                id = n.id,
                x = n.x,
                y = n.y,
                z = n.z,
                type = n.type,
                label = n.label,
            )
        }

        val edges = draftGraph.edges.map { e ->
            AuthoringEdge(
                id = e.id,
                from = e.from,
                to = e.to,
                cost = e.cost,
                bidirectional = e.bidirectional,
            )
        }

        // Create rooms from non-largest zones (zones that have room_entry nodes)
        val rooms = zones.drop(1).mapIndexed { index, zone ->
            val entryNode = draftGraph.nodes.find { it.zoneId == zone.id }
            AuthoringRoom(
                id = zone.id,
                displayName = zone.label,
                destinationNodeId = entryNode?.id ?: draftGraph.nodes.first().id,
                category = "unknown",
                keywords = emptyList(),
                aliases = emptyList(),
                description = "Auto-generated zone — rename and categorize manually",
            )
        }

        // Placeholder entrance marker at the first node
        val firstNode = draftGraph.nodes.firstOrNull()
        val markers = if (firstNode != null) {
            listOf(
                AuthoringMarker(
                    id = "marker-draft",
                    startNodeId = firstNode.id,
                    physicalWidthMeters = 0.21,
                    physicalHeightMeters = 0.21,
                    position = Position3D(firstNode.x, firstNode.y + 1.2, firstNode.z),
                    forwardBasis = "-z",
                    notes = "PLACEHOLDER — position and configure a real marker",
                )
            )
        } else emptyList()

        val config = AuthoringConfig(
            buildingId = "draft-${System.currentTimeMillis() % 10000}",
            buildingName = "Draft Building (edit me)",
            floorId = "ground",
            asset = AssetReference(glbFile = glbFileName),
            tags = listOf("draft", "auto-generated"),
            entranceMarkers = markers,
            nodes = nodes,
            edges = edges,
            rooms = rooms,
            routeRendering = AuthoringRouteRendering(),
            graphMetadata = GraphMetadata(
                authorName = "nav-preprocessor (auto)",
                authoredDate = Instant.now().toString().substringBefore('T'),
                notes = "Auto-generated draft config. Zones are labeled neutrally. " +
                    "Review node positions, rename zones, add real entrance markers.",
            ),
        )

        val configPath = File(outDir, "authoring_config.generated.json")
        configPath.writeText(json.encodeToString(config))

        // Also write generation metadata
        val metadata = GeneratedMetadata(
            timestamp = Instant.now().toString(),
            floorY = floorEstimate.floorY,
            floorConfidence = floorEstimate.confidence,
            zoneCount = zones.size,
            nodeCount = nodes.size,
            edgeCount = edges.size,
        )
        File(outDir, "generation_metadata.json").writeText(json.encodeToString(metadata))

        return configPath.absolutePath
    }
}
