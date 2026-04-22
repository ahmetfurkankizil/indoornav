# File Dossier: GlbParserTest.kt

## Path
`tools\nav-preprocessor\src\test\kotlin\com\vecturai\tools\preprocessor\GlbParserTest.kt`

## Type
Unit/Integration Test

## Role
Unit/Integration Test for the tools component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
package com.vecturai.tools.preprocessor

import com.vecturai.tools.preprocessor.glb.GlbParser
import com.vecturai.tools.preprocessor.glb.GlbGeometryExtractor
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.*

/**
 * Tests for [GlbParser] — GLB binary format parsing.
 */
class GlbParserTest {

    private val parser = GlbParser()

    private fun createMinimalGlb(): ByteArray {
        // Minimal valid GLB: header + JSON chunk with empty meshes
        val jsonContent = """{"meshes":[],"accessors":[],"bufferViews":[],"buffers":[]}"""
        val jsonBytes = jsonContent.toByteArray(Charsets.UTF_8)
        // Pad JSON to 4-byte alignment
        val jsonPadded = jsonBytes.size.let { size ->
            val padded = (size + 3) and 3.inv()
            jsonBytes + ByteArray(padded - size) { 0x20 }
        }

        val totalLength = 12 + 8 + jsonPadded.size
        val buf = ByteBuffer.allocate(totalLength).order(ByteOrder.LITTLE_ENDIAN)

        
```

## Status
Mapped (Pass 3 Normalization)
