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
    private val model  = "claude-3-5-sonnet-20241022"

    private val json = Json { 
        ignoreUnknownKeys = true 
        coerceInputValues = true
    }

    private val httpClient = HttpClient(Java) {
        install(ContentNegotiation) { json(json) }
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

        val systemPrompt = """
            You are a professional indoor navigation architect. Your task is to analyze a floor plan image and suggest a logical navigation graph (nodes and edges).
            
            WORKFLOW:
            1. First, analyze the image to find all rooms, hallways, and entrance/exit points.
            2. Place nodes:
               - 'room': At the entrance of every room (just inside the door).
               - 'entrance': At the main entry point to the floor.
               - 'elevator'/'stairs': At vertical transport points.
               - 'turning point': Every time a corridor bends or branches.
            3. Connect nodes:
               - Create logical paths between rooms and hallways.
            
            CONSTRAINTS:
            - Respond ONLY with the JSON object.
            - Do not be lazy. Suggest at least one node for every visible room.
            
            Response Format:
            {
              "nodes": [
                { "id": "new_1", "label": "Room 101", "nodeType": "room", "canvasX": 0.5, "canvasY": 0.5 }
              ],
              "edges": [
                { "fromNodeId": "new_1", "toNodeId": "new_2", "waypoints": [], "confidence": 0.9 }
              ]
            }
        """.trimIndent()

        val requestBody = buildJsonObject {
            put("model", model)
            put("max_tokens", 4096)
            put("system", systemPrompt)
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "user")
                    putJsonArray("content") {
                        addJsonObject {
                            put("type", "text")
                            put("text", "Please analyze this floor plan and suggest a complete navigation graph. " + 
                                       (if (nodes.isNotEmpty()) "I have provided some existing nodes to start with." else "Start from scratch."))
                        }
                        addJsonObject {
                            put("type", "image")
                            putJsonObject("source") {
                                put("type", "base64")
                                put("media_type", "image/png")
                                put("data", floorPlanImageBase64)
                            }
                        }
                    }
                }
            }
        }

        return try {
            println("AI Suggest: Sending request (model=$model, nodes=${nodes.size}, imgLen=${floorPlanImageBase64.length}, hasKey=${apiKey.isNotBlank()})")
            val response = httpClient.post("https://api.anthropic.com/v1/messages") {
                header("x-api-key", apiKey)
                header("anthropic-version", "2023-06-01")
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            if (response.status != HttpStatusCode.OK) {
                val errorBody = response.body<String>()
                println("AI Suggest: Anthropic API Error (${response.status}): $errorBody")
                return AiSuggestResponse(emptyList())
            }

            val responseJson = response.body<JsonObject>()
            val text = responseJson["content"]
                ?.jsonArray
                ?.firstOrNull()
                ?.jsonObject
                ?.get("text")
                ?.jsonPrimitive
                ?.content
                ?: run {
                    println("AI Suggest: No text content in response: $responseJson")
                    return AiSuggestResponse(emptyList())
                }

            println("AI Suggest: Raw response text: $text")

            // Strip markdown code fences if present
            val cleaned = text.trim()
                .removePrefix("```json").removePrefix("```")
                .removeSuffix("```").trim()

            val result = json.decodeFromString<AiSuggestResponse>(cleaned)
            println("AI Suggest: Parsed ${result.edges.size} edges")
            result
        } catch (e: Exception) {
            println("AI Suggest: Execution Error: ${e.message}")
            e.printStackTrace()
            AiSuggestResponse(emptyList())
        }
    }
}
