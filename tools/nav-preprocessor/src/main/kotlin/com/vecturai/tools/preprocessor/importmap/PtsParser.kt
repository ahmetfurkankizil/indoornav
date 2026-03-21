package com.vecturai.tools.preprocessor.importmap

import com.vecturai.tools.preprocessor.ValidationException
import com.vecturai.tools.preprocessor.glb.Vec3
import java.io.File

/**
 * Parses Leica PTS point-cloud files.
 *
 * Format:
 * - Line 1: total point count (integer)
 * - Subsequent lines: `x y z [intensity] [r g b]`
 *
 * Only x/y/z columns (indices 0–2 on data lines) are extracted;
 * intensity and colour columns are ignored.
 */
class PtsParser {

    /**
     * Parse a .pts file and return vertex positions.
     *
     * @throws ValidationException on invalid or unreadable files
     */
    fun parse(path: String): List<Vec3> {
        val file = File(path)
        if (!file.exists()) throw ValidationException("PTS file not found: $path")
        if (!file.canRead()) throw ValidationException("PTS file not readable: $path")

        val allLines = file.readLines()
        if (allLines.isEmpty()) {
            throw ValidationException("PTS file is empty: $path")
        }

        // First line should be the point count
        val expectedCount = allLines[0].trim().toIntOrNull()
            ?: throw ValidationException("PTS file first line is not a valid point count: '${allLines[0].trim()}'")

        val vertices = mutableListOf<Vec3>()

        for (i in 1..minOf(expectedCount, allLines.size - 1)) {
            val trimmed = allLines[i].trim()
            if (trimmed.isEmpty()) continue

            val parts = trimmed.split("\\s+".toRegex())
            if (parts.size < 3) continue

            val x = parts[0].toFloatOrNull() ?: continue
            val y = parts[1].toFloatOrNull() ?: continue
            val z = parts[2].toFloatOrNull() ?: continue
            vertices.add(Vec3(x, y, z))
        }

        if (vertices.isEmpty()) {
            throw ValidationException("PTS file contains no valid vertices: $path")
        }

        return vertices
    }
}
