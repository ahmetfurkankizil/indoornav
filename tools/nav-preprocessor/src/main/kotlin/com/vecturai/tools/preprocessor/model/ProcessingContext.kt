package com.vecturai.tools.preprocessor.model

/**
 * Mutable context bag passed through all pipeline steps.
 *
 * Each step reads from and writes to this context, building up
 * the processed data incrementally.
 *
 * @property inputPath Path to the input .glb file
 * @property outputDir Path to the output directory
 */
data class ProcessingContext(
    val inputPath: String,
    val outputDir: String,

    // Populated by LoadAssetStep
    var assetLoaded: Boolean = false,
    var vertexCount: Int = 0,
    var faceCount: Int = 0,

    // Populated by ValidateStep
    var isValid: Boolean = false,
    var validationErrors: List<String> = emptyList(),

    // Populated by ExtractFloorStep
    var floorExtracted: Boolean = false,
    var floorBoundsMinX: Double = 0.0,
    var floorBoundsMinY: Double = 0.0,
    var floorBoundsMaxX: Double = 0.0,
    var floorBoundsMaxY: Double = 0.0,

    // Populated by BuildGraphStep
    var graphBuilt: Boolean = false,
    var nodeCount: Int = 0,
    var edgeCount: Int = 0,

    // Populated by ExportContractsStep
    var contractsExported: Boolean = false,
    var exportedFiles: List<String> = emptyList(),
)
