package com.Vectura AI.tools.admin.service

import com.Vectura AI.tools.admin.db.tables.Buildings
import com.Vectura AI.tools.admin.db.tables.Edges
import com.Vectura AI.tools.admin.db.tables.Floors
import com.Vectura AI.tools.admin.db.tables.Nodes
import com.Vectura AI.tools.admin.model.EdgeResponse
import com.Vectura AI.tools.admin.model.Waypoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID
import kotlin.math.sqrt

class EdgeService {

    suspend fun create(
        floorId: String,
        managerId: String,
        fromNodeId: String,
        toNodeId: String,
        edgeType: String,
        isBidirectional: Boolean,
        waypoints: List<Waypoint>,
        createdBy: String,
    ): EdgeResponse? = withContext(Dispatchers.IO) {
        transaction {
            val floorRow = Floors.innerJoin(Buildings)
                .selectAll()
                .where {
                    (Floors.id eq UUID.fromString(floorId)) and
                        (Buildings.managerId eq UUID.fromString(managerId))
                }
                .singleOrNull() ?: return@transaction null

            val buildingId = floorRow[Floors.buildingId].value.toString()
            val dist = computeDistance(fromNodeId, toNodeId)

            val now = Instant.now()
            val id = Edges.insertAndGetId {
                it[Edges.floorId]         = UUID.fromString(floorId)
                it[Edges.buildingId]      = UUID.fromString(buildingId)
                it[Edges.fromNodeId]      = UUID.fromString(fromNodeId)
                it[Edges.toNodeId]        = UUID.fromString(toNodeId)
                it[Edges.isBidirectional] = isBidirectional
                it[Edges.distance]        = dist
                it[Edges.edgeType]        = edgeType
                it[Edges.waypoints]       = Json.encodeToString(waypoints)
                it[Edges.createdBy]       = createdBy
                it[Edges.createdAt]       = now
                it[Edges.updatedAt]       = now
            }

            Edges.selectAll().where { Edges.id eq id }.single().let { rowToResponse(it) }
        }
    }

    suspend fun list(floorId: String, managerId: String): List<EdgeResponse>? =
        withContext(Dispatchers.IO) {
            transaction {
                Floors.innerJoin(Buildings)
                    .selectAll()
                    .where {
                        (Floors.id eq UUID.fromString(floorId)) and
                            (Buildings.managerId eq UUID.fromString(managerId))
                    }
                    .singleOrNull() ?: return@transaction null

                Edges.selectAll()
                    .where { Edges.floorId eq UUID.fromString(floorId) }
                    .map { rowToResponse(it) }
            }
        }

    suspend fun update(
        edgeId: String,
        managerId: String,
        edgeType: String?,
        isBidirectional: Boolean?,
        waypoints: List<Waypoint>?,
    ): EdgeResponse? = withContext(Dispatchers.IO) {
        transaction {
            Edges.innerJoin(Floors, { Edges.floorId }, { Floors.id })
                .innerJoin(Buildings, { Floors.buildingId }, { Buildings.id })
                .selectAll()
                .where {
                    (Edges.id eq UUID.fromString(edgeId)) and
                        (Buildings.managerId eq UUID.fromString(managerId))
                }
                .singleOrNull() ?: return@transaction null

            Edges.update({ Edges.id eq UUID.fromString(edgeId) }) {
                edgeType?.let { v -> it[Edges.edgeType] = v }
                isBidirectional?.let { v -> it[Edges.isBidirectional] = v }
                waypoints?.let { v -> it[Edges.waypoints] = Json.encodeToString(v) }
                it[Edges.updatedAt] = Instant.now()
            }

            Edges.selectAll().where { Edges.id eq UUID.fromString(edgeId) }
                .single().let { rowToResponse(it) }
        }
    }

    suspend fun delete(edgeId: String, managerId: String): Boolean =
        withContext(Dispatchers.IO) {
            transaction {
                Edges.innerJoin(Floors, { Edges.floorId }, { Floors.id })
                    .innerJoin(Buildings, { Floors.buildingId }, { Buildings.id })
                    .selectAll()
                    .where {
                        (Edges.id eq UUID.fromString(edgeId)) and
                            (Buildings.managerId eq UUID.fromString(managerId))
                    }
                    .singleOrNull() ?: return@transaction false

                Edges.deleteWhere { Edges.id eq UUID.fromString(edgeId) } > 0
            }
        }

    private fun computeDistance(fromId: String, toId: String): Double? {
        val from = Nodes.selectAll().where { Nodes.id eq UUID.fromString(fromId) }.singleOrNull()
        val to   = Nodes.selectAll().where { Nodes.id eq UUID.fromString(toId) }.singleOrNull()
        if (from == null || to == null) return null
        val wx1 = from[Nodes.worldX] ?: return null
        val wy1 = from[Nodes.worldY] ?: 0.0
        val wz1 = from[Nodes.worldZ] ?: return null
        val wx2 = to[Nodes.worldX]   ?: return null
        val wy2 = to[Nodes.worldY]   ?: 0.0
        val wz2 = to[Nodes.worldZ]   ?: return null
        val dx = wx2 - wx1; val dy = wy2 - wy1; val dz = wz2 - wz1
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    private fun rowToResponse(row: ResultRow): EdgeResponse = EdgeResponse(
        id              = row[Edges.id].value.toString(),
        floorId         = row[Edges.floorId].value.toString(),
        buildingId      = row[Edges.buildingId].value.toString(),
        fromNodeId      = row[Edges.fromNodeId].value.toString(),
        toNodeId        = row[Edges.toNodeId].value.toString(),
        isBidirectional = row[Edges.isBidirectional],
        distance        = row[Edges.distance],
        edgeType        = row[Edges.edgeType],
        waypoints       = Json.decodeFromString(row[Edges.waypoints]),
        createdBy       = row[Edges.createdBy],
        createdAt       = row[Edges.createdAt].toString(),
        updatedAt       = row[Edges.updatedAt].toString(),
    )
}
