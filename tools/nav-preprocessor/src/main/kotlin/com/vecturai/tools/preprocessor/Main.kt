package com.vecturai.tools.preprocessor

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required

/**
 * VecturAI Navigation Preprocessor CLI.
 *
 * Converts a Polycam .glb scan into the navigation data contracts
 * required by the VecturAI app (nav_graph.json, rooms.json, etc.).
 *
 * Usage:
 *   nav-preprocessor --input scan.glb --output ./output/
 *
 * Pipeline steps:
 *   1. Load .glb asset
 *   2. Validate geometry
 *   3. Extract floor candidate
 *   4. Build navigation graph
 *   5. Export data contracts
 */
class NavPreprocessorCommand : CliktCommand(
    name = "nav-preprocessor",
    help = "Convert Polycam .glb scans to VecturAI navigation data contracts.",
) {
    private val input by option("--input", "-i", help = "Path to input Polycam .glb file")
        .required()

    private val output by option("--output", "-o", help = "Output directory for generated contracts")
        .required()

    override fun run() {
        echo("╔══════════════════════════════════════════╗")
        echo("║   VecturAI Navigation Preprocessor       ║")
        echo("║   v0.1.0                                  ║")
        echo("╚══════════════════════════════════════════╝")
        echo("")
        echo("Input:  $input")
        echo("Output: $output")
        echo("")

        val pipeline = Pipeline()
        pipeline.execute(input, output)
    }
}

fun main(args: Array<String>) = NavPreprocessorCommand().main(args)
