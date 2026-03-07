package com.vecturai.data.remote

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Ktor-based implementation of [RemoteBuildingDataSource].
 *
 * Uses Ktor's multiplatform HTTP client for network requests.
 * The actual HTTP engine is provided platform-specifically:
 * - Android: OkHttp
 * - iOS: Darwin (URLSession)
 *
 * TODO: Configure base URL from app configuration
 * TODO: Implement actual API calls
 * TODO: Add proper error handling and retry logic
 * TODO: Support request caching headers (ETag, If-None-Match)
 */
class KtorBuildingDataSource(
    private val baseUrl: String = "https://api.vecturai.com/v1",
) : RemoteBuildingDataSource {

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                prettyPrint = false
            })
        }
        // TODO: Add logging plugin for debugging
        // TODO: Add auth plugin if needed
        // TODO: Configure timeouts
    }

    override suspend fun fetchManifest(buildingId: String): String? {
        // TODO: GET $baseUrl/buildings/$buildingId/manifest.json
        return null
    }

    override suspend fun fetchBuildingData(buildingId: String, fileName: String): String? {
        // TODO: GET $baseUrl/buildings/$buildingId/data/$fileName
        return null
    }

    override suspend fun fetchBuildingPackage(buildingId: String): Map<String, String>? {
        // TODO: GET $baseUrl/buildings/$buildingId/package
        // Parse response into map of filename → content
        return null
    }

    override suspend fun getLatestVersion(buildingId: String): Int {
        // TODO: GET $baseUrl/buildings/$buildingId/version
        return -1
    }
}
