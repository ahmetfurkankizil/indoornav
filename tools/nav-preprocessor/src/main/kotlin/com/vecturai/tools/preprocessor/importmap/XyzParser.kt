package com.vecturai.tools.preprocessor.importmap

import com.vecturai.tools.preprocessor.ValidationException
import com.vecturai.tools.preprocessor.glb.Vec3
import java.io.File

/**
 * Parses XYZ point-cloud files.
 *
 * Format: one point per line, whitespace-delimited `x y z` values.
 * - Lines starting with `#` or `//` are treated as comments and skipped.
 * - Blank lines are skipped.
 * - Extra columns beyond column 2 (index 0-based) are ignored.
 */
class XyzParser {

    /**
     * Parse a .xyz file and return vertex positions.
     *
     * @throws ValidationException on invalid or unreadable files
     */
    fun parse(path: String): List<Vec3> {
        val file = File(path)
        if (!file.exists()) throw ValidationException("XYZ file not found: $path")
        if (!file.canRead()) throw ValidationException("XYZ file not readable: $path")

        val vertices = mutableListOf<Vec3>()
        var lineNumber = 0

        file.bufferedReader().useLines { lines ->
            for (line in lines) {
                lineNumber++
                val trimmed = line.trim()

                // Skip blanks and comments
                if (trimmed.isEmpty()) continue
                if (trimmed.startsWith("#") || trimmed.startsWith("//")) continue

                val parts = trimmed.split("\\s+".toRegex())
                if (parts.size < 3) {
                    // Skip malformed lines rather than failing the whole import
                    continue
                }

                val x = parts[0].toFloatOrNull()
                val y = parts[1].toFloatOrNull()
                val z = parts[2].toFloatOrNull()

                if (x != null && y != null && z != null) {
                    vertices.add(Vec3(x, y, z))
                }
                // else: skip non-numeric lines (e.g. header rows)
            }
        }

        if (vertices.isEmpty()) {
            throw ValidationException("XYZ file contains no valid vertices: $path")
        }

        return vertices
    }
}
