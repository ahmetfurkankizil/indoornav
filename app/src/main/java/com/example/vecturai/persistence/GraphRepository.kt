package com.example.vecturai.persistence

import android.content.Context
import com.example.vecturai.graph.MapGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class GraphRepository(context: Context) {
    private val appContext = context.applicationContext
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val directory: File
        get() = File(appContext.filesDir, "graphs").apply { mkdirs() }

    suspend fun save(graph: MapGraph) = withContext(Dispatchers.IO) {
        File(directory, "${graph.buildingName.toFileName()}.json")
            .writeText(json.encodeToString(graph))
    }

    suspend fun load(buildingName: String): MapGraph? = withContext(Dispatchers.IO) {
        val file = File(directory, "${buildingName.toFileName()}.json")
        if (file.exists()) json.decodeFromString<MapGraph>(file.readText()) else null
    }

    suspend fun listBuildings(): List<String> = withContext(Dispatchers.IO) {
        directory
            .listFiles { file -> file.isFile && file.extension == "json" }
            ?.map { it.nameWithoutExtension }
            ?.sorted()
            .orEmpty()
    }

    private fun String.toFileName(): String =
        trim()
            .ifBlank { "demo" }
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
}
