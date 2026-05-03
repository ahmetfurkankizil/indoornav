package com.Vectura AI.tools.admin.service

import com.Vectura AI.tools.admin.db.tables.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

@kotlinx.serialization.Serializable
data class RouteRenderingConfig(
    val arrowSpacingMeters: Double = 0.6,
    val lookaheadDistanceMeters: Double = 8.0,
    val destinationThresholdMeters: Double = 1.2,
    val turnMarkerThresholdDegrees: Double = 25.0,
    val arrowHeightOffsetMeters: Double = 0.1,
)

private val navJson = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
}

class NavPackageGenerator {

    suspend fun generate(buildingId: String): String = withContext(Dispatchers.IO) {
        transaction {
            val buildingRow = Buildings.selectAll()
                .where { Buildings.id eq UUID.fromString(buildingId) }
                .single()

            val floors = Floors.selectAll()
                .where { Floors.buildingId eq UUID.fromString(buildingId) }
                .orderBy(Floors.floorNumber to SortOrder.ASC)
                .map { floorRow ->
                    val floorId = floorRow[Floors.id].value.toString()

                    val nodes = Nodes.selectAll()
                        .where { Nodes.floorId eq UUID.fromString(floorId) }
                        .map { n ->
                            buildJsonObject {
                                put("id", n[Nodes.id].value.toString())
                                put("label", n[Nodes.label])
                                put("type", n[Nodes.nodeType])
                                put("x", n[Nodes.worldX] ?: 0.0)
                                put("y", n[Nodes.worldY] ?: 0.0)
                                put("z", n[Nodes.worldZ] ?: 0.0)
                            }
                        }

                    val edges = Edges.selectAll()
                        .where { Edges.floorId eq UUID.fromString(floorId) }
                        .map { e ->
                            val rawWaypoints = e[Edges.waypoints]
                            buildJsonObject {
                                put("id", e[Edges.id].value.toString())
                                put("from", e[Edges.fromNodeId].value.toString())
                                put("to", e[Edges.toNodeId].value.toString())
                                put("cost", e[Edges.distance] ?: 1.0)
                                put("bidirectional", e[Edges.isBidirectional])
                                put("type", e[Edges.edgeType])
                                // Store waypoints as 3D world-space points
                                put("waypoints", Json.parseToJsonElement(rawWaypoints))
                            }
                        }

                    buildJsonObject {
                        put("floorId", floorId)
                        put("floorNumber", floorRow[Floors.floorNumber])
                        put("floorName", floorRow[Floors.floorName])
                        put("floorY", floorRow[Floors.floorY] ?: 0.0)
                        put("nodes", JsonArray(nodes))
                        put("edges", JsonArray(edges))
                    }
                }

            val connections = FloorConnections.selectAll()
                .where { FloorConnections.buildingId eq UUID.fromString(buildingId) }
                .map { c ->
                    buildJsonObject {
                        put("id", c[FloorConnections.id].value.toString())
                        put("fromNodeId", c[FloorConnections.fromNodeId].value.toString())
                        put("toNodeId", c[FloorConnections.toNodeId].value.toString())
                        put("type", c[FloorConnections.connectionType])
                        put("bidirectional", c[FloorConnections.isBidirectional])
                    }
                }

            // Determine next version
            val lastVersion = NavigationPackages.selectAll()
                .where { NavigationPackages.buildingId eq UUID.fromString(buildingId) }
                .maxByOrNull { NavigationPackages.version }
                ?.get(NavigationPackages.version) ?: 0

            val markers = EntranceMarkers.selectAll()
                .where { EntranceMarkers.buildingId eq UUID.fromString(buildingId) }
                .map { m ->
                    buildJsonObject {
                        put("id", m[EntranceMarkers.id].value.toString())
                        put("displayName", m[EntranceMarkers.displayName])
                        put("startNodeId", m[EntranceMarkers.startNodeId].value.toString())
                        put("physicalWidthMeters", m[EntranceMarkers.physicalWidthMeters])
                        put("physicalHeightMeters", m[EntranceMarkers.physicalHeightMeters])
                        putJsonObject("position") {
                            put("x", m[EntranceMarkers.worldX] ?: 0.0)
                            put("y", m[EntranceMarkers.worldY] ?: 0.0)
                            put("z", m[EntranceMarkers.worldZ] ?: 0.0)
                        }
                        put("forwardBasis", m[EntranceMarkers.forwardBasis])
                        put("rotationYDegrees", m[EntranceMarkers.rotationYDegrees])
                        put("referenceImageName", m[EntranceMarkers.referenceImageName])
                    }
                }

            val packageJson = buildJsonObject {
                put("buildingId", buildingId)
                put("buildingName", buildingRow[Buildings.name])
                put("version", lastVersion + 1)
                put("floors", JsonArray(floors))
                put("crossFloorConnections", JsonArray(connections))
                put("entranceMarkers", JsonArray(markers))
                put("buildingWidthMeters", buildingRow[Buildings.widthMeters])
                put("routeRendering", navJson.encodeToJsonElement(RouteRenderingConfig()))
            }

            val jsonString = navJson.encodeToString(packageJson)
            val checksum = sha256(jsonString)

            // Mark all previous packages as not current
            NavigationPackages.update({
                NavigationPackages.buildingId eq UUID.fromString(buildingId)
            }) { it[NavigationPackages.isCurrent] = false }

            // Insert new current package
            NavigationPackages.insertAndGetId {
                it[NavigationPackages.buildingId]  = UUID.fromString(buildingId)
                it[NavigationPackages.version]     = lastVersion + 1
                it[NavigationPackages.checksum]    = checksum
                it[NavigationPackages.isCurrent]   = true
                it[NavigationPackages.generatedAt] = Instant.now()
                it[NavigationPackages.packageJson] = jsonString
            }

            jsonString
        }
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
