package com.vecturai.tools.preprocessor

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.VecturAI.tools.preprocessor.glb.GlbGeometryExtractor
import com.VecturAI.tools.preprocessor.glb.GlbParser
import kotlin.system.exitProcess

/**
 * VecturAI Navigation Preprocessor CLI v2.0.
 *
 * Subcommands:
 * - inspect:         Inspect a .glb file and print geometry stats
 * - generate-draft:  Parse .glb geometry and generate a draft authoring config
 * - export-package:  Validate config + .glb and export a production package
 */
class NavPreprocessorCommand : CliktCommand(
    name = "nav-preprocessor",
) {
    override fun run() {
        echo("╔══════════════════════════════════════════════╗")
        echo("║   VecturAI Navigation Preprocessor v2.0     ║")
        echo("╚══════════════════════════════════════════════╝")
        echo("")
    }
}

// ── inspect ──────────────────────────────────────────────

class InspectCommand : CliktCommand(
    name = "inspect",
) {
    override fun help(context: com.github.ajalt.clikt.core.Context) =
        "Inspect a .glb file: validate header, extract geometry stats, estimate floor plane."
    private val input by option("--input", "-i", help = "Path to .glb file")
        .required()

    override fun run() {
        echo("─── GLB Inspection ───")
        echo("")

        try {
            // Parse GLB
            echo("[1/3] Parsing GLB binary... ")
            val parser = GlbParser()
            val glbData = parser.parse(input)
            echo("  ✓ ${glbData.json.meshes.size} meshes, ${glbData.json.accessors.size} accessors")
            echo("  Binary chunk: ${formatSize(glbData.binChunk.size.toLong())}")

            // Extract geometry
            echo("[2/3] Extracting geometry... ")
            val extractor = GlbGeometryExtractor()
            val geometry = extractor.extract(glbData)
            echo("  ✓ ${geometry.vertices.size} vertices from ${geometry.primitiveCount} primitives")

            geometry.boundingBox?.let { bb ->
                echo("  Bounding box:")
                echo("    X: [%.3f .. %.3f] (%.3fm)".format(bb.minX, bb.maxX, bb.extentX))
                echo("    Y: [%.3f .. %.3f] (%.3fm)".format(bb.minY, bb.maxY, bb.extentY))
                echo("    Z: [%.3f .. %.3f] (%.3fm)".format(bb.minZ, bb.maxZ, bb.extentZ))
            }

            // Floor estimate
            echo("[3/3] Estimating floor plane... ")
            val floorEstimator = com.VecturAI.tools.preprocessor.analysis.FloorPlaneEstimator()
            val floorEstimate = floorEstimator.estimate(geometry.vertices)
            if (floorEstimate != null) {
                echo("  ✓ Floor Y = %.3f (confidence: %.1f%%, %d vertices)".format(
                    floorEstimate.floorY, floorEstimate.confidence * 100, floorEstimate.floorVertexCount))
            } else {
                echo("  ⚠ Could not estimate floor plane (insufficient data)")
            }

            echo("")
            echo("─── Inspection complete ───")

        } catch (e: ValidationException) {
            echo("✗ ${e.message}")
            throw SystemExitException(1)
        } catch (e: Exception) {
            echo("✗ Unexpected error: ${e.message}")
            throw SystemExitException(2)
        }
    }
}

// ── generate-draft ───────────────────────────────────────

class GenerateDraftCommand : CliktCommand(
    name = "generate-draft",
) {
    override fun help(context: com.github.ajalt.clikt.core.Context) =
        "Parse .glb geometry and generate a draft authoring config with navigation graph."
    private val input by option("--input", "-i", help = "Path to .glb file")
        .required()

    private val output by option("--output", "-o", help = "Output directory for draft config and debug artifacts")
        .required()

    override fun run() {
        val pipeline = DraftPipeline()
        val exitCode = pipeline.execute(
            glbPath = input,
            outputDir = output,
        )

        if (exitCode != 0) {
            throw SystemExitException(exitCode)
        }
    }
}

// ── export-package ───────────────────────────────────────

class ExportPackageCommand : CliktCommand(
    name = "export-package",
) {
    override fun help(context: com.github.ajalt.clikt.core.Context) =
        "Validate an authoring config + .glb and export a production building package."
    private val input by option("--input", "-i", help = "Path to .glb file")
        .required()

    private val config by option("--config", "-c", help = "Path to authoring config JSON")
        .required()

    private val output by option("--output", "-o", help = "Output directory for the package")
        .required()

    private val overwrite by option("--overwrite", help = "Overwrite existing output directory")
        .flag(default = false)

    override fun run() {
        val pipeline = Pipeline()
        val exitCode = pipeline.execute(
            glbPath = input,
            configPath = config,
            outputDir = output,
            overwrite = overwrite,
        )

        if (exitCode != 0) {
            throw SystemExitException(exitCode)
        }
    }
}

// ── Utilities ────────────────────────────────────────────

class SystemExitException(val code: Int) : RuntimeException()

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "${"%.1f".format(bytes.toDouble() / (1024 * 1024))} MB"
}

fun main(args: Array<String>) {
    try {
        NavPreprocessorCommand()
            .subcommands(InspectCommand(), GenerateDraftCommand(), ExportPackageCommand())
            .main(args)
    } catch (e: SystemExitException) {
        exitProcess(e.code)
    }
}