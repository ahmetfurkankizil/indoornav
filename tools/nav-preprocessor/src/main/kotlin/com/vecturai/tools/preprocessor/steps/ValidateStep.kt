package com.vecturai.tools.preprocessor.steps

import com.vecturai.tools.preprocessor.PipelineStep
import com.vecturai.tools.preprocessor.model.ProcessingContext

/**
 * Step 2: Validate the loaded asset geometry.
 *
 * TODO: Check mesh is watertight (no holes)
 * TODO: Verify mesh is reasonably sized (building-scale)
 * TODO: Check for degenerate triangles
 * TODO: Validate coordinate system orientation (Y-up or Z-up)
 * TODO: Warn on extremely high poly count (performance)
 */
class ValidateStep : PipelineStep {
    override val name = "Validate Geometry"

    override fun execute(context: ProcessingContext) {
        if (!context.assetLoaded) {
            throw IllegalStateException("Asset not loaded — cannot validate")
        }

        val errors = mutableListOf<String>()

        // TODO: Implement geometry validation checks
        // - Check vertex count > minimum threshold
        // - Check for NaN/Inf coordinates
        // - Verify bounding box is building-scale (1m–200m range)
        // - Check for flipped normals

        context.validationErrors = errors
        context.isValid = errors.isEmpty()

        if (errors.isNotEmpty()) {
            throw IllegalStateException(
                "Validation failed with ${errors.size} error(s):\n" +
                    errors.joinToString("\n") { "  • $it" }
            )
        }
    }
}
