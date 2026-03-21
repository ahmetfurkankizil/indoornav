package com.vecturai.tools.preprocessor.importmap

import com.vecturai.tools.preprocessor.DebugExporter
import com.vecturai.tools.preprocessor.ValidationException
import com.vecturai.tools.preprocessor.analysis.FloorPlaneEstimator
import com.vecturai.tools.preprocessor.analysis.OccupancyGridGenerator
import com.vecturai.tools.preprocessor.analysis.ZoneSuggester
import com.vecturai.tools.preprocessor.draft.DraftConfigGenerator
import com.vecturai.tools.preprocessor.draft.NavigationGraphDrafter
import com.vecturai.tools.preprocessor.glb.GlbGeometryExtractor
import com.vecturai.tools.preprocessor.glb.GlbParser
import com.vecturai.tools.preprocessor.glb.Vec3
import java.io.File

/**
 * Orchestrates import of floor-plan scans in multiple formats into the draft pipeline.
 *
 * Supported formats: `.glb`, `.gltf`, `.ply`, `.xyz`, `.pts`, `.csv`
 *
 * Flow:
 * 1. Detect format from file extension
 * 2. Parse the file into `List<Vec3>` (vertex positions)
 * 3. Run the same analysis chain as [DraftPipeline]:
 *    FloorPlaneEstimator → OccupancyGridGenerator → ZoneSuggester →
 *    NavigationGraphDrafter → DraftConfigGenerator
 * 4. Export debug artifacts
 */
class MapImportPipeline {

    companion object {
        /** File extensions recognised by this pipeline. */
        val SUPPORTED_EXTENSIONS = setOf("glb", "gltf", "ply", "xyz", "pts", "csv")
    }

    private val plyParser = PlyParser()
    private val xyzParser = XyzParser()
    private val ptsParser = PtsParser()
    private val csvParser = CsvPointCloudParser()
    private val glbParser = GlbParser()
    private val geometryExtractor = GlbGeometryExtractor()

    private val floorEstimator = FloorPlaneEstimator()
    private val gridGenerator = OccupancyGridGenerator()
    private val zoneSuggester = ZoneSuggester()
    private val graphDrafter = NavigationGraphDrafter()
    private val configGenerator = DraftConfigGenerator()
    private val debugExporter = DebugExporter()

    /**
     * Execute the full import pipeline.
     *
     * @return Exit code: 0 = success, 1 = validation/data error, 2 = unexpected error
     */
    fun execute(inputPath: String, outputDir: String): Int {
        try {
            val file = File(inputPath)
            if (!file.exists()) {
                println("✗ File not found: $inputPath")
                return 1
            }

            val ext = file.extension.lowercase()
            if (ext !in SUPPORTED_EXTENSIONS) {
                println("✗ Unsupported format: .$ext")
                println("  Supported: ${SUPPORTED_EXTENSIONS.joinToString(", ") { ".$it" }}")
                return 1
            }

            println("╔══════════════════════════════════════════════╗")
            println("║   VecturAI Map Import Pipeline              ║")
            println("╚══════════════════════════════════════════════╝")
            println()
            println("  Input:  $inputPath")
            println("  Format: .$ext")
            println("  Output: $outputDir")
            println()

            // ── Step 1: Parse input into vertices ──
            print("[1/7] Parsing .$ext file... ")
            val vertices = parseInput(inputPath, ext)
            if (vertices.isEmpty()) {
                println("✗")
                println("  No vertex data found in the file")
                return 1
            }
            println("✓ (${vertices.size} vertices)")

            // ── Step 2: Compute bounding box ──
            print("[2/7] Computing bounding box... ")
            val boundingBox = computeBoundingBox(vertices)
            println("✓")
            if (boundingBox != null) {
                println("  Bounds: X[%.2f..%.2f] Y[%.2f..%.2f] Z[%.2f..%.2f]".format(
                    boundingBox.minX, boundingBox.maxX,
                    boundingBox.minY, boundingBox.maxY,
                    boundingBox.minZ, boundingBox.maxZ))
                println("  Extent: %.2fm × %.2fm × %.2fm".format(
                    boundingBox.extentX, boundingBox.extentY, boundingBox.extentZ))
            }

            // ── Step 3: Estimate floor plane ──
            print("[3/7] Estimating floor plane... ")
            val floorEstimate = floorEstimator.estimate(vertices)
            if (floorEstimate == null) {
                println("✗")
                println("  Insufficient vertex data for floor estimation")
                return 1
            }
            println("✓ (floorY=%.3f, confidence=%.1f%%, %d floor vertices)".format(
                floorEstimate.floorY, floorEstimate.confidence * 100, floorEstimate.floorVertexCount))

            // ── Step 4: Generate occupancy grid ──
            print("[4/7] Generating occupancy grid... ")
            val grid = gridGenerator.generate(floorEstimate.floorVertices)
            if (grid == null) {
                println("✗")
                println("  Could not generate occupancy grid")
                return 1
            }
            println("✓ (${grid.width}×${grid.height} cells, ${grid.occupiedCount} occupied, cell=${grid.cellSize}m)")

            // ── Step 5: Discover zones ──
            print("[5/7] Discovering zones... ")
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
            print("[6/7] Drafting navigation graph... ")
            val draftGraph = graphDrafter.draft(zones, grid, floorEstimate.floorY)
            println("✓ (${draftGraph.nodes.size} nodes, ${draftGraph.edges.size} edges)")

            // ── Step 7: Generate draft config + debug artifacts ──
            print("[7/7] Generating draft config... ")
            val sourceFileName = file.name
            val configPath = configGenerator.generate(
                glbFileName = sourceFileName,
                draftGraph = draftGraph,
                zones = zones,
                floorEstimate = floorEstimate,
                boundingBox = boundingBox,
                outputDir = outputDir,
            )
            println("✓")
            println("  → authoring_config.generated.json")
            println("  → generation_metadata.json")

            // Build a dummy GeometryResult for the debug exporter
            val dummyGeometry = GlbGeometryExtractor.GeometryResult(
                vertices = vertices,
                boundingBox = boundingBox,
                meshCount = 0,
                primitiveCount = 0,
            )
            debugExporter.exportDraftDebug(grid, zones, draftGraph, floorEstimate, dummyGeometry, outputDir)
            println("  → occupancy_debug.svg")
            println("  → draft_graph_debug.svg")
            println("  → geometry_stats.json")

            println()
            println("══════════════════════════════════════════════")
            println("Draft config generated: $configPath")
            println("Source format: .$ext (${vertices.size} vertices imported)")
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

    /**
     * Parse the input file into a list of vertex positions.
     */
    internal fun parseInput(path: String, extension: String): List<Vec3> {
        return when (extension.lowercase()) {
            "glb", "gltf" -> {
                val glbData = glbParser.parse(path)
                val geometry = geometryExtractor.extract(glbData)
                geometry.vertices
            }
            "ply" -> plyParser.parse(path)
            "xyz" -> xyzParser.parse(path)
            "pts" -> ptsParser.parse(path)
            "csv" -> csvParser.parse(path)
            else -> throw ValidationException("Unsupported format: .$extension")
        }
    }

    private fun computeBoundingBox(vertices: List<Vec3>): com.vecturai.tools.preprocessor.glb.BoundingBox3D? {
        if (vertices.isEmpty()) return null

        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE; var minZ = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE

        for (v in vertices) {
            if (v.x < minX) minX = v.x; if (v.x > maxX) maxX = v.x
            if (v.y < minY) minY = v.y; if (v.y > maxY) maxY = v.y
            if (v.z < minZ) minZ = v.z; if (v.z > maxZ) maxZ = v.z
        }

        return com.vecturai.tools.preprocessor.glb.BoundingBox3D(minX, minY, minZ, maxX, maxY, maxZ)
    }
}
