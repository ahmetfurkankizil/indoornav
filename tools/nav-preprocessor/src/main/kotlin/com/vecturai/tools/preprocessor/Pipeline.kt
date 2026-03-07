package com.vecturai.tools.preprocessor

import com.vecturai.tools.preprocessor.model.ProcessingContext
import com.vecturai.tools.preprocessor.steps.*

/**
 * Ordered pipeline that processes a .glb scan into navigation contracts.
 *
 * Each step receives a [ProcessingContext] and either modifies it
 * or fails with a descriptive error. Steps are executed sequentially.
 */
class Pipeline {

    private val steps: List<PipelineStep> = listOf(
        LoadAssetStep(),
        ValidateStep(),
        ExtractFloorStep(),
        BuildGraphStep(),
        ExportContractsStep(),
    )

    /**
     * Execute the full preprocessing pipeline.
     *
     * @param inputPath Path to the input .glb file
     * @param outputDir Path to the output directory
     */
    fun execute(inputPath: String, outputDir: String) {
        val context = ProcessingContext(
            inputPath = inputPath,
            outputDir = outputDir,
        )

        println("Starting pipeline with ${steps.size} steps...")
        println()

        for ((index, step) in steps.withIndex()) {
            val stepNum = index + 1
            print("[$stepNum/${steps.size}] ${step.name}... ")

            try {
                step.execute(context)
                println("✓")
            } catch (e: Exception) {
                println("✗")
                println("  Error: ${e.message}")
                println()
                println("Pipeline failed at step $stepNum: ${step.name}")
                return
            }
        }

        println()
        println("Pipeline completed successfully!")
        println("Output written to: $outputDir")
    }
}

/**
 * Interface for a single step in the preprocessing pipeline.
 */
interface PipelineStep {
    /** Human-readable name for progress display. */
    val name: String

    /**
     * Execute this pipeline step.
     *
     * @param context Mutable processing context
     * @throws Exception if the step fails
     */
    fun execute(context: ProcessingContext)
}
