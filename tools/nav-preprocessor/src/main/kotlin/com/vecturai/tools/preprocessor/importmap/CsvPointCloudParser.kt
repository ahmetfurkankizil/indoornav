package com.vecturai.tools.preprocessor.importmap

import com.vecturai.tools.preprocessor.ValidationException
import com.vecturai.tools.preprocessor.glb.Vec3
import java.io.File

/**
 * Parses CSV files containing point-cloud data.
 *
 * Auto-detects a header row by looking for columns named `x`, `y`, `z`
 * (case-insensitive). If found, uses those column indices. Otherwise,
 * assumes columns 0, 1, 2 are x, y, z respectively.
 *
 * Supports both comma (`,`) and semicolon (`;`) delimiters.
 */
class CsvPointCloudParser {

    /**
     * Parse a .csv file and return vertex positions.
     *
     * @throws ValidationException on invalid or unreadable files
     */
    fun parse(path: String): List<Vec3> {
        val file = File(path)
        if (!file.exists()) throw ValidationException("CSV file not found: $path")
        if (!file.canRead()) throw ValidationException("CSV file not readable: $path")

        val allLines = file.readLines().filter { it.isNotBlank() }
        if (allLines.isEmpty()) {
            throw ValidationException("CSV file is empty: $path")
        }

        // Detect delimiter: whichever of ',' or ';' appears more on the first line
        val delimiter = detectDelimiter(allLines[0])

        // Try to detect header
        val firstParts = allLines[0].split(delimiter).map { it.trim().lowercase().trim('"') }
        val hasHeader = firstParts.any { it == "x" } &&
            firstParts.any { it == "y" } &&
            firstParts.any { it == "z" }

        val xIndex: Int
        val yIndex: Int
        val zIndex: Int
        val dataStartIndex: Int

        if (hasHeader) {
            xIndex = firstParts.indexOfFirst { it == "x" }
            yIndex = firstParts.indexOfFirst { it == "y" }
            zIndex = firstParts.indexOfFirst { it == "z" }
            dataStartIndex = 1
        } else {
            // Assume columns 0, 1, 2
            xIndex = 0
            yIndex = 1
            zIndex = 2
            dataStartIndex = 0
        }

        val maxNeeded = maxOf(xIndex, yIndex, zIndex) + 1
        val vertices = mutableListOf<Vec3>()

        for (i in dataStartIndex until allLines.size) {
            val parts = allLines[i].split(delimiter).map { it.trim().trim('"') }
            if (parts.size < maxNeeded) continue

            val x = parts[xIndex].toFloatOrNull() ?: continue
            val y = parts[yIndex].toFloatOrNull() ?: continue
            val z = parts[zIndex].toFloatOrNull() ?: continue
            vertices.add(Vec3(x, y, z))
        }

        if (vertices.isEmpty()) {
            throw ValidationException("CSV file contains no valid x/y/z data: $path")
        }

        return vertices
    }

    private fun detectDelimiter(firstLine: String): Char {
        val commaCount = firstLine.count { it == ',' }
        val semicolonCount = firstLine.count { it == ';' }
        return if (semicolonCount > commaCount) ';' else ','
    }
}
