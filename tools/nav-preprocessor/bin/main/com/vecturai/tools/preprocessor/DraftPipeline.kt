package com.VecturAI.tools.preprocessor

import com.VecturAI.tools.preprocessor.analysis.FloorPlaneEstimator
import com.VecturAI.tools.preprocessor.analysis.OccupancyGridGenerator
import com.VecturAI.tools.preprocessor.analysis.ZoneSuggester
import com.VecturAI.tools.preprocessor.draft.DraftConfigGenerator
import com.VecturAI.tools.preprocessor.draft.NavigationGraphDrafter
import com.VecturAI.tools.preprocessor.glb.GlbGeometryExtractor
import com.VecturAI.tools.preprocessor.glb.GlbParser
import java.io.File

/**
 * Orchestrates the draft-generation pipeline.
 *
 * Steps:
 * 1. Parse GLB binary
 * 2. Extract mesh geometry
 * 3. Estimate floor plane
 * 4. Generate occupancy grid
 * 5. Discover zones
 * 6. Draft navigation graph
 * 7. Generate draft config
 * 8. Export debug artifacts
 */
class DraftPipelineV2 {

    private val glbParser = GlbParser()
    private val geometryExtractor = GlbGeometryExtractor()
    private val floorEstimator = FloorPlaneEstimator()
    private val gridGenerator = OccupancyGridGenerator()
    private val zoneSuggester = ZoneSuggester()
    private val graphDrafter = NavigationGraphDrafter()
    private val configGenerator = DraftConfigGenerator()
    private val debugExporter = DebugExporter()

    /**
     * Execute the full draft pipeline.
     *
     * @return Exit code: 0 = success, 1 = validation/data error, 2 = unexpected error
     */
    fun execute(glbPath: String, outputDir: String): Int {
        try {
            // ── Step 1: Parse GLB ──
            print("[1/8] Parsing GLB binary... ")
            val glbData = glbParser.parse(glbPath)
            val meshCount = glbData.json.meshes.size
            println("✓ ($meshCount meshes, ${formatSize(glbData.binChunk.size.toLong())} binary data)")

            // ── Step 2: Extract geometry ──
            print("[2/8] Extracting geometry... ")
            val geometry = geometryExtractor.extract(glbData)
            if (geometry.vertices.isEmpty()) {
                println("✗")
                println("  No vertex data found in GLB file")
                return 1
            }
            println("✓ (${geometry.vertices.size} vertices, ${geometry.primitiveCount} primitives)")
            geometry.boundingBox?.let { bb ->
                println("  Bounds: X[%.2f..%.2f] Y[%.2f..%.2f] Z[%.2f..%.2f]".format(
                    bb.minX, bb.maxX, bb.minY, bb.maxY, bb.minZ, bb.maxZ))
                println("  Extent: %.2fm × %.2fm × %.2fm".format(bb.extentX, bb.extentY, bb.extentZ))
            }

            // ── Step 3: Estimate floor plane ──
            print("[3/8] Estimating floor plane... ")
            val floorEstimate = floorEstimator.estimate(geometry.vertices)
            if (floorEstimate == null) {
                println("✗")
                println("  Insufficient vertex data for floor estimation")
                return 1
            }
            println("✓ (floorY=%.3f, confidence=%.1f%%, %d floor vertices)".format(
                floorEstimate.floorY, floorEstimate.confidence * 100, floorEstimate.floorVertexCount))

            // ── Step 4: Generate occupancy grid ──
            print("[4/8] Generating occupancy grid... ")
            val grid = gridGenerator.generate(floorEstimate.floorVertices)
            if (grid == null) {
                println("✗")
                println("  Could not generate occupancy grid")
                return 1
            }
            println("✓ (${grid.width}×${grid.height} cells, ${grid.occupiedCount} occupied, cell=${grid.cellSize}m)")

            // ── Step 5: Discover zones ──
            print("[5/8] Discovering zones... ")
            val zones = zoneSuggester.suggest(grid)
            if (zones.isEmpty()) {
                println("✗")
                println("  No walkable zones found in occupancy grid")
                return 1
            }
            println("✓ (${zones.size} zones)")
            zones.forEach { zone ->
                println("  • ${zone.label}: ${zone.cellCount} cells, centroid=(%.2f, %.2f)".format(
                    zone.centroidX, zone.centroidZ))
            }

            // ── Step 6: Draft navigation graph ──
            print("[6/8] Drafting navigation graph... ")
            val draftGraph = graphDrafter.draft(zones, grid, floorEstimate.floorY)
            println("✓ (${draftGraph.nodes.size} nodes, ${draftGraph.edges.size} edges)")

            // ── Step 7: Generate draft config ──
            print("[7/8] Generating draft config... ")
            val glbFileName = File(glbPath).name
            val configPath = configGenerator.generate(
                glbFileName = glbFileName,
                draftGraph = draftGraph,
                zones = zones,
                floorEstimate = floorEstimate,
                boundingBox = geometry.boundingBox,
                outputDir = outputDir,
            )
            println("✓")
            println("  → authoring_config.generated.json")
            println("  → generation_metadata.json")

            // ── Step 8: Export debug artifacts ──
            print("[8/8] Exporting debug artifacts... ")
            debugExporter.exportDraftDebug(grid, zones, draftGraph, floorEstimate, geometry, outputDir)
            println("✓")
            println("  → occupancy_debug.svg")
            println("  → draft_graph_debug.svg")
            println("  → geometry_stats.json")

            println()
            println("══════════════════════════════════════════════")
            println("Draft config generated: $configPath")
            println("⚠  This is a DRAFT — review and edit before use!")
            println("══════════════════════════════════════════════")
            return 0

        } catch (e: ValidationException) {
            println("✗")
            println("  Validation error: ${e.message}")
            return 1
        } catch (e: Exception) {
            println("✗")
            println("  Unexpected error: ${e.message}")
            e.printStackTrace()
            return 2
        }
    }

    private fun formatSize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${"%.1f".format(bytes.toDouble() / (1024 * 1024))} MB"
    }
}
