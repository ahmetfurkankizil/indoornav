package com.vecturai.tools.admin.service

import com.vecturai.tools.admin.Env

import com.vecturai.tools.admin.model.AiNodeInput
import com.vecturai.tools.admin.model.AiSuggestResponse
import com.vecturai.tools.admin.model.SuggestedEdge
import com.vecturai.tools.admin.model.Waypoint
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*

class AiEdgeSuggester {
    private val apiKey = Env.get("ANTHROPIC_API_KEY") ?: ""
    private val model  = "claude-haiku-4-5"

    private val httpClient = HttpClient(Java) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    suspend fun suggest(
        floorPlanImageBase64: String,
        nodes: List<AiNodeInput>,
    ): AiSuggestResponse {
        if (apiKey.isBlank()) return AiSuggestResponse(emptyList())

        val nodeListJson = Json.encodeToString(
            JsonArray.serializer(),
            JsonArray(nodes.map { n ->
                buildJsonObject {
                    put("id", n.id)
                    put("label", n.label)
                    put("nodeType", n.nodeType)
                    put("canvasX", n.canvasX)
                    put("canvasY", n.canvasY)
                }
            })
        )

        val prompt = """
You are an indoor navigation path planner. You are given:
1. A 2D top-down floor plan image of a building floor
2. A list of labeled nodes with their normalized positions (0.0=left/top, 1.0=right/bottom)

Your task: suggest walkable edges connecting these nodes.

Rules:
- Edges must follow visible corridors and open floor space — never pass through walls
- Prefer paths a person would naturally walk (corridors, not diagonal room-cuts)
- Add waypoints where the path curves around an obstacle or wall corner
- Every 'room' node must be reachable from an 'entrance' or 'corridor' node
- Do not connect nodes separated by a solid wall with no visible door

Respond ONLY with valid JSON, no explanation or markdown:
{"edges":[{"fromNodeId":"...","toNodeId":"...","waypoints":[{"x":0.0,"y":0.0}],"confidence":0.85}]}

Nodes:
$nodeListJson
""".trimIndent()

        val requestBody = buildJsonObject {
            put("model", model)
            put("max_tokens", 2048)
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "user")
                    putJsonArray("content") {
                        addJsonObject {
                            put("type", "image")
                            putJsonObject("source") {
                                put("type", "base64")
                                put("media_type", "image/png")
                                put("data", floorPlanImageBase64)
                            }
                        }
                        addJsonObject {
                            put("type", "text")
                            put("text", prompt)
                        }
                    }
                }
            }
        }

        return try {
            val response = httpClient.post("https://api.anthropic.com/v1/messages") {
                header("x-api-key", apiKey)
                header("anthropic-version", "2023-06-01")
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            val responseJson = response.body<JsonObject>()
            val text = responseJson["content"]
                ?.jsonArray
                ?.firstOrNull()
                ?.jsonObject
                ?.get("text")
                ?.jsonPrimitive
                ?.content
                ?: return AiSuggestResponse(emptyList())

            // Strip markdown code fences if present
            val cleaned = text.trim()
                .removePrefix("```json").removePrefix("```")
                .removeSuffix("```").trim()

            Json.decodeFromString<AiSuggestResponse>(cleaned)
        } catch (_: Exception) {
            AiSuggestResponse(emptyList())
        }
    }
}
