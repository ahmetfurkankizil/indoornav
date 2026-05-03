package com.Vectura AI.data.remote

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Ktor-based implementation of [RemoteBuildingDataSource].
 */
class KtorBuildingDataSource(
    private val baseUrl: String = "http://localhost:8080",
) : RemoteBuildingDataSource {

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                prettyPrint = false
            })
        }
    }

    override suspend fun fetchManifest(buildingId: String): String? = null
    override suspend fun fetchBuildingData(buildingId: String, fileName: String): String? = null
    override suspend fun fetchBuildingPackage(buildingId: String): Map<String, String>? = null
    override suspend fun getLatestVersion(buildingId: String): Int = -1

    override suspend fun fetchBuildingPackageByToken(token: String): String? {
        val url = "$baseUrl/mobile/buildings/$token/nav-package"
        println("Vectura AI_DEBUG: Requesting URL: $url")
        return try {
            val response = httpClient.get(url)
            println("Vectura AI_DEBUG: Status: ${response.status}")
            if (response.status == HttpStatusCode.OK) {
                response.bodyAsText()
            } else {
                println("Vectura AI_DEBUG: Failed with status ${response.status}")
                null
            }
        } catch (e: Exception) {
            println("Vectura AI_DEBUG: Network Error: ${e.message}")
            e.printStackTrace()
            null
        }
    }
}
