package com.Vectura AI.tools.admin.routes

import com.Vectura AI.tools.admin.db.tables.Buildings
import com.Vectura AI.tools.admin.db.tables.NavigationPackages
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import com.Vectura AI.tools.admin.Env
import java.util.UUID

fun Route.mobileApiRoutes() {
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
            val qrService = com.Vectura AI.tools.admin.service.QRCodeService(uploadsDir)
            
            val bytes = qrService.getQrImageBytes(buildingId)
                ?: qrService.generate(qrToken)

            call.respondBytes(bytes, ContentType.Image.PNG)
        }
    }
}
