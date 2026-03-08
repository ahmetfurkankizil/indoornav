package com.vecturai.tools.preprocessor

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import kotlin.system.exitProcess

/**
 * VecturAI Navigation Preprocessor CLI v1.0.
 *
 * Validates a human-authored building config + Polycam .glb scan
 * and exports a production building package.
 */
class NavPreprocessorCommand : CliktCommand(
    name = "nav-preprocessor",
) {
    private val input by option("--input", "-i")
        .required()

    private val config by option("--config", "-c")
        .required()

    private val output by option("--output", "-o")
        .required()

    private val overwrite by option("--overwrite")
        .flag(default = false)

    override fun run() {
        echo("╔══════════════════════════════════════════════╗")
        echo("║   VecturAI Navigation Preprocessor v1.0     ║")
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