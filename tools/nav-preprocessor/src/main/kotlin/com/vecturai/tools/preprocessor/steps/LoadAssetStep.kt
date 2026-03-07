package com.vecturai.tools.preprocessor.steps

import com.vecturai.tools.preprocessor.PipelineStep
import com.vecturai.tools.preprocessor.model.ProcessingContext
import java.io.File

/**
 * Step 1: Load the .glb asset file.
 *
 * TODO: Integrate a glTF/GLB parser library (e.g., JglTF)
 * TODO: Parse binary GLB container format
 * TODO: Extract mesh data (vertices, faces, normals)
 * TODO: Extract material information for floor detection
 */
class LoadAssetStep : PipelineStep {
    override val name = "Load Asset"

    override fun execute(context: ProcessingContext) {
        val file = File(context.inputPath)
        if (!file.exists()) {
            throw IllegalArgumentException("Input file not found: ${context.inputPath}")
        }
        if (!file.name.endsWith(".glb", ignoreCase = true)) {
            throw IllegalArgumentException("Expected .glb file, got: ${file.name}")
        }

        // TODO: Parse GLB file and extract mesh data
        // For now, just verify the file exists and record basic info
        context.assetLoaded = true
        context.vertexCount = 0  // TODO: Read from parsed GLB
        context.faceCount = 0    // TODO: Read from parsed GLB

        println("(${file.length() / 1024} KB)")
    }
}
