# File Dossier: JsonFileHistoryRepository.kt

## Path
`shared\core\src\commonMain\kotlin\com\vecturai\core\repository\JsonFileHistoryRepository.kt`

## Type
Authored Source

## Role
Authored Source for the shared component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
package com.vecturai.core.repository

import com.vecturai.core.domain.VisitRecord
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * JSON-file-backed history repository.
 *
 * Persists visit records to a JSON file on the local filesystem
 * so history survives app restarts. Uses a platform-provided
 * file path for storage.
 *
 * Thread safety: single-writer assumption (MVP, single UI thread).
 *
 * @param readFile Function to read the JSON file contents (empty string if not found)
 * @param writeFile Function to write JSON string to file
 */
class JsonFileHistoryRepository(
    private val readFile: () -> String,
    private val writeFile: (String) -> Unit,
) : HistoryRepository {

    private val json = Json {
        prettyPrint = false
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    private var cache: MutableList<VisitRecord>? = null

    private fun loadCache(): MutableList<VisitRecord> {
        if (cache != null) 
```

## Status
Mapped (Pass 3 Normalization)
