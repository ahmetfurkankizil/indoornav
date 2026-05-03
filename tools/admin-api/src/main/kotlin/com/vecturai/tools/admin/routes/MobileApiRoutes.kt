package com.vecturai.tools.admin.routes

import com.vecturai.tools.admin.db.tables.Buildings
import com.vecturai.tools.admin.db.tables.NavigationPackages
import com.vecturai.tools.admin.service.AiRouteIntentService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonNull
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import com.vecturai.tools.admin.Env
import java.util.UUID

fun Route.mobileApiRoutes() {
    val aiRouteIntentService = AiRouteIntentService()

    route("/mobile/buildings/{token}/nav-package") {
        get {
            val token = call.parameters["token"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, buildJsonObject { put("error", "Missing token") })

            println("DEBUG: Mobile request for token: $token")

            val (packageJson, checksum) = withContext(Dispatchers.IO) {
                transaction {
                    val building = Buildings.selectAll()
                        .where { Buildings.qrToken eq token }
                        .singleOrNull()
                    
                    if (building == null) {
                        println("DEBUG: No building found for token: $token")
                        return@transaction null
                    }

                    println("DEBUG: Found building: ${building[Buildings.name]} (${building[Buildings.id]})")

                    val pkg = NavigationPackages.selectAll()
                        .where {
                            (NavigationPackages.buildingId eq building[Buildings.id]) and
                                (NavigationPackages.isCurrent eq true)
                        }
                        .singleOrNull()
                    
                    if (pkg == null) {
                        println("DEBUG: No current package found for building: ${building[Buildings.id]}")
                        return@transaction null
                    }

                    println("DEBUG: Found published package version: ${pkg[NavigationPackages.version]}")
                    Pair(
                        pkg[NavigationPackages.packageJson],
                        pkg[NavigationPackages.checksum],
                    )
                }
            } ?: return@get call.respond(HttpStatusCode.NotFound, buildJsonObject { put("error", "Building not found or not published") })

            // Parse and inject checksum into the response
            val parsed = Json.parseToJsonElement(packageJson) as? JsonObject
                ?: return@get call.respond(HttpStatusCode.InternalServerError, buildJsonObject { put("error", "Invalid package data") })

            val withChecksum = buildJsonObject {
                parsed.forEach { (k, v) -> put(k, v) }
                put("checksum", checksum)
            }

            call.respond(withChecksum)
        }
    }

    // POST /mobile/assistant/route-intent
    //
    // LLM-backed intent classifier for the iOS BackendGptRouteAgent. The
    // device sends the raw user request plus the catalog vocabulary it knows
    // about; we return a structured intent so the device-side deterministic
    // tool layer can run the actual search and route. API keys live ONLY on
    // the server. On any failure we still return HTTP 200 with intent="unknown"
    // so the mobile fallback path triggers cleanly.
    route("/mobile/assistant/route-intent") {
        post {
            val body = runCatching { call.receive<JsonObject>() }.getOrElse {
                return@post call.respond(HttpStatusCode.BadRequest, buildJsonObject { put("error", "Invalid JSON body") })
            }

            val rawText = body["rawText"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            if (rawText.isEmpty()) {
                return@post call.respond(HttpStatusCode.BadRequest, buildJsonObject { put("error", "rawText is required") })
            }

            val categories = body["availableCategories"]?.jsonArray
                ?.mapNotNull { runCatching { it.jsonPrimitive.contentOrNull }.getOrNull() }
                ?.filter { it.isNotBlank() }
                ?: emptyList()
            val aliases = body["availableRoomAliases"]?.jsonArray
                ?.mapNotNull { runCatching { it.jsonPrimitive.contentOrNull }.getOrNull() }
                ?.filter { it.isNotBlank() }
                ?: emptyList()

            val intent = withContext(Dispatchers.IO) {
                aiRouteIntentService.classify(rawText, categories, aliases)
            }

            if (intent == null) {
                call.respond(buildJsonObject {
                    put("intent", "unknown")
                    put("category", JsonNull)
                    put("roomToken", JsonNull)
                    put("assistantMessage", "I couldn't understand that. Try again with a place or category.")
                    put("confidence", 0.0)
                })
                return@post
            }

            call.respond(buildJsonObject {
                put("intent", intent.intent)
                put("category", intent.category ?: "")
                put("roomToken", intent.roomToken ?: "")
                put("assistantMessage", intent.assistantMessage ?: "")
                put("confidence", intent.confidence ?: 0.5)
            })
        }
    }

    route("/mobile/buildings/{token}/qr") {
        get {
            val token = call.parameters["token"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, buildJsonObject { put("error", "Missing token") })

            val (buildingId, qrToken) = withContext(Dispatchers.IO) {
                transaction {
                    val building = Buildings.selectAll()
                        .where { Buildings.qrToken eq token }
                        .singleOrNull()
                        ?: return@transaction null
                    
                    building[Buildings.id].value.toString() to building[Buildings.qrToken]
                }
            } ?: return@get call.respond(HttpStatusCode.NotFound, buildJsonObject { put("error", "Building not found") })

            val uploadsDir = Env.get("UPLOADS_DIR") ?: "uploads"
            val qrService = com.vecturai.tools.admin.service.QRCodeService(uploadsDir)
            
            val bytes = qrService.getQrImageBytes(buildingId)
                ?: qrService.generate(qrToken)

            call.respondBytes(bytes, ContentType.Image.PNG)
        }
    }
}
