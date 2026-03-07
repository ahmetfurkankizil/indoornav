package com.vecturai.tools.preprocessor

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import kotlin.system.exitProcess

/**
 * VecturAI Navigation Preprocessor CLI v1.0.
 *
 * Validates a human-authored building config + Polycam .glb scan
 * and exports a production building package.
 *
 * Usage:
 *   nav-preprocessor --input scan.glb --config authoring_config.json --output ./package/
 *   nav-preprocessor -i scan.glb -c config.json -o ./out/ --overwrite
 *
 * Exit codes:
 *   0 = success
 *   1 = validation error
 *   2 = I/O or unexpected error
 */
class NavPreprocessorCommand : CliktCommand(
    name = "nav-preprocessor",
    help = "Validate authoring config and produce a VecturAI building package.",
) {
    private val input by option("--input", "-i", help = "Path to Polycam .glb file")
        .required()

    private val config by option("--config", "-c", help = "Path to authoring_config.json")
        .required()

    private val output by option("--output", "-o", help = "Output directory for the building package")
        .required()

    private val overwrite by option("--overwrite", help = "Overwrite output directory if it exists")
        .flag(default = false)

    override fun run() {
        echo("╔══════════════════════════════════════════════╗")
        echo("║   VecturAI Navigation Preprocessor v1.0      ║")
        echo("╚══════════════════════════════════════════════╝")
        echo("")

        val pipeline = Pipeline()
        val exitCode = pipeline.execute(
            glbPath = input,
            configPath = config,
            outputDir = output,
            overwrite = overwrite,
        )

        if (exitCode != 0) {
            exitProcess(exitCode)
        }
    }
}

fun main(args: Array<String>) = NavPreprocessorCommand().main(args)
