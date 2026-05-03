package com.Vectura AI.tools.admin.service

import com.Vectura AI.tools.admin.Env

import com.Vectura AI.tools.admin.model.AiNodeInput
import com.Vectura AI.tools.admin.model.AiSuggestResponse
import com.Vectura AI.tools.admin.model.SuggestedEdge
import com.Vectura AI.tools.admin.model.Waypoint
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*

class AiEdgeSuggester {
    private val ollamaBaseUrl = Env.get("OLLAMA_BASE_URL", "http://localhost:11434")
    private val model = Env.get("OLLAMA_MODEL", "llava:latest")

    private val json = Json { 
        ignoreUnknownKeys = true 
        coerceInputValues = true
    }

    private val httpClient = HttpClient(Java) {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            requestTimeoutMillis = 600000 // 10 minutes
            connectTimeoutMillis = 10000
            socketTimeoutMillis = 600000
        }
    }

    suspend fun suggest(
        floorPlanImageBase64: String,
        nodes: List<AiNodeInput>,
    ): AiSuggestResponse {
        // Local model doesn't need API key, but we check if base URL is set
        if (ollamaBaseUrl.isBlank()) return AiSuggestResponse(emptyList())

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
               - IMPORTANT: Use the EXACT SAME IDs for nodes in both 'nodes' and 'edges' arrays (e.g., if a node has id 'new_1', the edge should use 'fromNodeId': 'new_1').
            
            SCHEMA (STRICT NAMES):
            - nodes: Array of objects with fields: [id (string), label (string), nodeType (string), canvasX (float 0-1), canvasY (float 0-1)]
            - edges: Array of objects with fields: [fromNodeId (string), toNodeId (string), waypoints (empty array []), confidence (float 0-1)]
            
            NODE TYPES: 'room', 'entrance', 'elevator', 'stairs', 'turning point'
            
            CONSTRAINTS:
            - Respond ONLY with the JSON object.
            - DO NOT include any comments (// or #) inside the JSON.
            - DO NOT include any conversational text.
            - Ensure all coordinates are between 0.0 and 1.0 (relative to canvas).
            - Suggest at least one node for every visible room.
            
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
            put("stream", false)
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "system")
                    put("content", systemPrompt)
                }
                addJsonObject {
                    put("role", "user")
                    put("content", "Please analyze this floor plan and suggest a complete navigation graph. " + 
                                       (if (nodes.isNotEmpty()) "I have provided some existing nodes to start with." else "Start from scratch."))
                    putJsonArray("images") {
                        add(floorPlanImageBase64)
                    }
                }
            }
        }

        return try {
            println("AI Suggest: Sending request to Ollama (url=$ollamaBaseUrl, model=$model, nodes=${nodes.size}, imgLen=${floorPlanImageBase64.length})")
            val response = httpClient.post("$ollamaBaseUrl/api/chat") {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            if (response.status != HttpStatusCode.OK) {
                val errorBody = response.body<String>()
                println("AI Suggest: Ollama API Error (${response.status}): $errorBody")
                return AiSuggestResponse(emptyList())
            }

            val responseJson = response.body<JsonObject>()
            val text = responseJson["message"]
                ?.jsonObject
                ?.get("content")
                ?.jsonPrimitive
                ?.content
                ?: run {
                    println("AI Suggest: No message content in response: $responseJson")
                    return AiSuggestResponse(emptyList())
                }

            println("AI Suggest: Raw response text: $text")

            // Robust JSON extraction: look for all ```json ... ``` blocks and merge them
            val blocks = mutableListOf<String>()
            var remaining = text
            while (remaining.contains("```")) {
                val block = remaining.substringAfter("```json", "").substringBefore("```")
                    .ifBlank { remaining.substringAfter("```", "").substringBefore("```") }
                if (block.isNotBlank()) {
                    blocks.add(block.trim())
                    remaining = remaining.substringAfter("```").substringAfter("```")
                } else {
                    break
                }
            }

            val cleaned = if (blocks.isNotEmpty()) {
                // If multiple blocks, we try to merge them into one JSON object if they are partial
                if (blocks.size > 1) {
                    val allNodes = blocks.flatMap { b ->
                        val start = b.indexOf("\"nodes\":")
                        if (start == -1) return@flatMap emptyList<String>()
                        val end = b.lastIndexOf(']')
                        if (end == -1) return@flatMap emptyList<String>()
                        listOf(b.substring(start, end + 1))
                    }
                    val allEdges = blocks.flatMap { b ->
                        val start = b.indexOf("\"edges\":")
                        if (start == -1) return@flatMap emptyList<String>()
                        val end = b.lastIndexOf(']')
                        if (end == -1) return@flatMap emptyList<String>()
                        listOf(b.substring(start, end + 1))
                    }
                    "{ ${allNodes.joinToString(", ")}, ${allEdges.joinToString(", ")} }"
                } else {
                    blocks[0]
                }
            } else {
                // Fallback: find first { and last }
                val start = text.indexOf('{')
                val end = text.lastIndexOf('}')
                if (start != -1 && end != -1 && end > start) {
                    text.substring(start, end + 1).trim()
                } else {
                    text.trim()
                }
            }

            // Resilient mapping: fix common LLM field name drifts and case sensitivity
            val resilientJson = cleaned.lines().joinToString("\n") { line ->
                var l = line
                // Strip comments
                if (l.contains("//") && !l.contains("\": \"http")) l = l.substringBefore("//")
                if (l.contains("#")) l = l.substringBefore("#")
                
                // Rename fields only in node context (rough heuristic: look for "id", "label" etc)
                // We use a more careful replace to avoid breaking waypoints "x"/"y"
                l = l.replace("\"type\":", "\"nodeType\":").replace("\"Type\":", "\"nodeType\":")
                
                // Only replace x/y if they aren't followed by a comma or brace that looks like a waypoint list
                // Actually, let's just use regex for better accuracy
                l = l.replace(Regex("\"canvas_x\":"), "\"canvasX\":")
                l = l.replace(Regex("\"canvas_y\":"), "\"canvasY\":")
                // Only replace x/y if it's clearly a node definition (has label or nodeType)
                // This prevents breaking Waypoints which MUST use x/y
                if (l.contains("\"nodeType\"") || l.contains("\"label\"")) {
                    l = l.replace("\"x\":", "\"canvasX\":").replace("\"y\":", "\"canvasY\":")
                }
                
                // Map connections/Nodes/Edges to correct keys
                l = l.replace("\"Nodes\":", "\"nodes\":")
                l = l.replace("\"Edges\":", "\"edges\":")
                l = l.replace("\"connections\":", "\"edges\":")
                l
            }
            
            val result = json.decodeFromString<AiSuggestResponse>(resilientJson)
            println("AI Suggest: Parsed ${result.edges.size} edges and ${result.nodes.size} nodes")
            result
        } catch (e: Exception) {
            println("AI Suggest: Execution Error: ${e.message}")
            e.printStackTrace()
            AiSuggestResponse(emptyList())
        }
    }
}
