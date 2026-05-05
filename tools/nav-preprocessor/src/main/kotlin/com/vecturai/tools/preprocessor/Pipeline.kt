package com.vecturai.tools.preprocessor

/**
 * Orchestrates the v1 preprocessing pipeline.
 *
 * Steps:
 * 1. Inspect .glb asset (presence, basic metadata)
 * 2. Load and structurally validate authoring config
 * 3. Validate graph integrity (references, connectivity)
 * 4. Export package files
 * 5. Export debug artifacts
 */
class Pipeline {

    private val assetInspector = AssetInspector()
    private val configLoader = AuthoringConfigLoader()
    private val graphValidator = GraphValidator()
    private val packageExporter = PackageExporter()
    private val debugExporter = DebugExporter()

    /**
     * Execute the full pipeline.
     *
     * @return Exit code: 0 = success, 1 = validation error, 2 = unexpected error
     */
    fun execute(
        glbPath: String,
        configPath: String,
        outputDir: String,
        overwrite: Boolean = false,
    ): Int {
        try {
            // ── Step 1: Inspect .glb asset ──
            print("[1/5] Inspecting asset... ")
            val assetInfo = assetInspector.inspect(glbPath)
            println("✓ (${formatSize(assetInfo.fileSizeBytes)}, GLB v${assetInfo.glbVersion ?: "?"})")
            assetInfo.warnings.forEach { println("  ⚠ $it") }

            // ── Step 2: Load authoring config ──
            print("[2/5] Loading authoring config... ")
            val config = configLoader.load(configPath)
            val structErrors = configLoader.validateStructure(config)
            if (structErrors.isNotEmpty()) {
                println("✗")
                structErrors.forEach { println("  • $it") }
                return 1
            }
            println("✓ (${config.nodes.size} nodes, ${config.edges.size} edges, ${config.rooms.size} rooms)")

            // ── Step 3: Validate graph integrity ──
            print("[3/5] Validating graph... ")
            val graphResult = graphValidator.validate(config)
            if (!graphResult.isValid) {
                println("✗")
                graphResult.errors.forEach { println("  • $it") }
                return 1
            }
            println("✓")
            graphResult.warnings.forEach { println("  ⚠ $it") }

            // ── Step 4: Export package ──
            print("[4/5] Exporting package... ")
            val exportResult = packageExporter.export(config, glbPath, outputDir, overwrite)
            println("✓ (${exportResult.files.size} files)")
            exportResult.files.forEach { println("  → $it") }

            // ── Step 5: Export debug artifacts ──
            print("[5/5] Exporting debug artifacts... ")
            debugExporter.export(config, outputDir)
            println("✓")
            println("  → graph_debug.json")
            println("  → plan_view_debug.svg")

            println()
            println("══════════════════════════════════════════════")
            println("Package exported successfully to: ${exportResult.outputDir}")
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
