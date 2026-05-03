package com.vecturai.tools.admin.service

import com.vecturai.tools.admin.Env
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*

/**
 * Thin LLM proxy used by the iOS BackendGptRouteAgent.
 *
 * Takes a free-text user request from the mobile route assistant plus a list
 * of available POI categories / room aliases, and asks an Ollama-served model
 * (the same one already used by [AiEdgeSuggester]) to classify the intent.
 *
 * The response is intentionally tiny — only enough information for the
 * device-side deterministic tool layer to look up real candidates and route
 * them. The LLM never sees node coordinates and never produces a route.
 *
 * On any error (no model, parse failure, empty response, ...) [classify]
 * returns `null` so the caller can fall back to the mock agent.
 */
class AiRouteIntentService {

    private val ollamaBaseUrl = Env.get("OLLAMA_BASE_URL", "http://localhost:11434")
    private val model         = Env.get("OLLAMA_MODEL",   "gpt-oss:latest")

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults    = true
    }

    private val httpClient = HttpClient(Java) {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 5_000
            socketTimeoutMillis  = 15_000
        }
    }

    data class IntentResult(
        val intent: String,
        val category: String?,
        val roomToken: String?,
        val assistantMessage: String?,
        val confidence: Double?,
    )

    suspend fun classify(
        rawText: String,
        availableCategories: List<String>,
        availableRoomAliases: List<String>,
    ): IntentResult? {
        if (ollamaBaseUrl.isBlank()) return null

        val systemPrompt = SYSTEM_PROMPT
        val userPrompt = buildJsonObject {
            put("rawText", rawText)
            putJsonArray("availableCategories") { availableCategories.distinct().forEach { add(it) } }
            putJsonArray("availableRoomAliases") { availableRoomAliases.distinct().forEach { add(it) } }
        }.toString()

        val requestBody = buildJsonObject {
            put("model", model)
            put("stream", false)
            put("format", "json")
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "system")
                    put("content", systemPrompt)
                }
                addJsonObject {
                    put("role", "user")
                    put("content", userPrompt)
                }
            }
        }

        return try {
            val response = httpClient.post("$ollamaBaseUrl/api/chat") {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
            if (response.status != HttpStatusCode.OK) return null

            val responseJson = response.body<JsonObject>()
            val text = responseJson["message"]
                ?.jsonObject
                ?.get("content")
                ?.jsonPrimitive
                ?.content
                ?: return null

            val obj = parseJsonObject(text) ?: return null
            val intent = obj["intent"]?.jsonPrimitive?.contentOrNull
                ?.let(::normalizeIntent)
                ?: "unknown"

            IntentResult(
                intent = intent,
                category = obj["category"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() },
                roomToken = obj["roomToken"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() },
                assistantMessage = obj["assistantMessage"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() },
                confidence = obj["confidence"]?.jsonPrimitive?.doubleOrNull,
            )
        } catch (e: Exception) {
            println("AiRouteIntent: ${e.message}")
            null
        }
    }

    private fun parseJsonObject(text: String): JsonObject? {
        val cleaned = text.trim().let {
            // Strip ```json fences if the model leaks them despite format=json.
            if (it.startsWith("```")) {
                it.substringAfter("```json", it.substringAfter("```"))
                    .substringBeforeLast("```")
            } else {
                it
            }
        }
        val start = cleaned.indexOf('{')
        val end   = cleaned.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return runCatching { Json.parseToJsonElement(cleaned.substring(start, end + 1)).jsonObject }.getOrNull()
    }

    private fun normalizeIntent(raw: String): String = when (raw.lowercase().trim()) {
        "roomidentifier", "room", "room_id", "room-id" -> "roomIdentifier"
        "categorysearch", "category", "categorical"     -> "categorySearch"
        "freetext", "free_text", "fuzzy"                -> "freeText"
        "ambiguous"                                     -> "ambiguous"
        else                                            -> "unknown"
    }

    companion object {
        private val SYSTEM_PROMPT = """
            You are the route-intent interpreter of an indoor navigation assistant.
            
            HARD RULES:
            - You MUST NOT invent destinations, coordinates or room names.
            - You MUST NOT claim to know facts about the building.
            - You MUST NOT use personal data; the user has no profile.
            - You MUST output ONLY valid JSON matching the schema below.
            - You MUST ask the device to confirm before any navigation starts (the
              device handles confirmation; you only return data).
            
            INPUT (JSON object):
            {
              "rawText": string,
              "availableCategories": string[],
              "availableRoomAliases": string[]
            }
            
            TASK:
            Classify the user request into ONE of these intents:
            - "roomIdentifier"  – the request mentions a specific room (e.g. EA-Z04, ea101).
            - "categorySearch"  – the request is about a category (restroom, coffee, lab, ...).
            - "freeText"        – fuzzy / synonym match against an alias.
            - "ambiguous"       – the request could mean several different things.
            - "unknown"         – nothing in the building matches.
            
            For "roomIdentifier" set "roomToken" to the room id you found in rawText
            (verbatim). For "categorySearch" set "category" to one element of
            availableCategories. Always include a short conversational
            "assistantMessage" that asks the user to confirm.
            
            OUTPUT (JSON object, no extra text):
            {
              "intent": "roomIdentifier" | "categorySearch" | "freeText" | "ambiguous" | "unknown",
              "category": string | null,
              "roomToken": string | null,
              "assistantMessage": string,
              "confidence": number  (0.0 - 1.0)
            }
        """.trimIndent()
    }
}
