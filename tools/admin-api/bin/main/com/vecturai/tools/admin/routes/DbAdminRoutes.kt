package com.vecturai.tools.admin.routes

import com.vecturai.tools.admin.db.tables.*
import com.vecturai.tools.admin.model.NavPackageListItem
import com.vecturai.tools.admin.model.SystemStats
import com.vecturai.tools.admin.service.NavPackageGenerator
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

fun Route.dbAdminRoutes(navGen: NavPackageGenerator) {
    authenticate("superadmin-auth") {
        route("/api/db-admin") {

            get("/stats") {
                val stats = withContext(Dispatchers.IO) {
                    transaction {
                        SystemStats(
                            managerCount      = Managers.selectAll().count().toInt(),
                            buildingCount     = Buildings.selectAll().count().toInt(),
                            floorCount        = Floors.selectAll().count().toInt(),
                            nodeCount         = Nodes.selectAll().count().toInt(),
                            edgeCount         = Edges.selectAll().count().toInt(),
                            navPackageCount   = NavigationPackages.selectAll().count().toInt(),
                        )
                    }
                }
                call.respond(stats)
            }

            // ── Managers ──────────────────────────────────────────────
            get("/managers") {
                val managers = withContext(Dispatchers.IO) {
                    transaction {
                        Managers.selectAll().map { row ->
                            mapOf(
                                "id"        to row[Managers.id].value.toString(),
                                "email"     to row[Managers.email],
                                "fullName"  to row[Managers.fullName],
                                "createdAt" to row[Managers.createdAt].toString(),
                            )
                        }
                    }
                }
                call.respond(managers)
            }

            delete("/managers/{id}") {
                val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing id"))
                withContext(Dispatchers.IO) {
                    transaction { Managers.deleteWhere { Managers.id eq UUID.fromString(id) } }
                }
                call.respond(HttpStatusCode.NoContent)
            }

            // ── Buildings ─────────────────────────────────────────────
            get("/buildings") {
                val buildings = withContext(Dispatchers.IO) {
                    transaction {
                        Buildings.selectAll().map { row ->
                            mapOf(
                                "id"        to row[Buildings.id].value.toString(),
                                "managerId" to row[Buildings.managerId].value.toString(),
                                "name"      to row[Buildings.name],
                                "status"    to row[Buildings.status],
                                "createdAt" to row[Buildings.createdAt].toString(),
                            )
                        }
                    }
                }
                call.respond(buildings)
            }

            delete("/buildings/{id}") {
                val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing id"))
                withContext(Dispatchers.IO) {
                    transaction { Buildings.deleteWhere { Buildings.id eq UUID.fromString(id) } }
                }
                call.respond(HttpStatusCode.NoContent)
            }

            // ── Navigation packages ───────────────────────────────────
            get("/nav-packages") {
                val packages = withContext(Dispatchers.IO) {
                    transaction {
                        NavigationPackages
                            .innerJoin(Buildings)
                            .selectAll()
                            .map { row ->
                                NavPackageListItem(
                                    id           = row[NavigationPackages.id].value.toString(),
                                    buildingId   = row[NavigationPackages.buildingId].value.toString(),
                                    buildingName = row[Buildings.name],
                                    version      = row[NavigationPackages.version],
                                    checksum     = row[NavigationPackages.checksum],
                                    isCurrent    = row[NavigationPackages.isCurrent],
                                    generatedAt  = row[NavigationPackages.generatedAt].toString(),
                                )
                            }
                    }
                }
                call.respond(packages)
            }

            get("/nav-packages/{id}/json") {
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing id"))
                val json = withContext(Dispatchers.IO) {
                    transaction {
                        NavigationPackages.selectAll()
                            .where { NavigationPackages.id eq UUID.fromString(id) }
                            .singleOrNull()
                            ?.get(NavigationPackages.packageJson)
                    }
                } ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("Package not found"))

                call.respond(Json.parseToJsonElement(json))
            }

            delete("/nav-packages/{id}") {
                val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing id"))
                withContext(Dispatchers.IO) {
                    transaction { NavigationPackages.deleteWhere { NavigationPackages.id eq UUID.fromString(id) } }
                }
                call.respond(HttpStatusCode.NoContent)
            }

            post("/buildings/{id}/regenerate") {
                val buildingId = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing id"))
                navGen.generate(buildingId)
                call.respond(mapOf("status" to "regenerated"))
            }
        }
    }
}
