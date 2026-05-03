package com.Vectura AI.tools.admin.service

import com.Vectura AI.tools.admin.db.tables.Buildings
import com.Vectura AI.tools.admin.db.tables.Floors
import com.Vectura AI.tools.admin.db.tables.Nodes
import com.Vectura AI.tools.admin.model.NodeResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID
import kotlin.math.sqrt

class NodeService {

    suspend fun create(
        floorId: String,
        managerId: String,
        label: String,
        nodeType: String,
        canvasX: Double,
        canvasY: Double,
        metadata: String,
    ): NodeResponse? = withContext(Dispatchers.IO) {
        transaction {
            val floorRow = Floors.innerJoin(Buildings)
                .selectAll()
                .where {
                    (Floors.id eq UUID.fromString(floorId)) and
                        (Buildings.managerId eq UUID.fromString(managerId))
                }
                .singleOrNull() ?: return@transaction null

            val buildingId = floorRow[Floors.buildingId].value.toString()
            val (wx, wy, wz) = computeWorldCoords(canvasX, canvasY, floorRow)

            val now = Instant.now()
            val id = Nodes.insertAndGetId {
                it[Nodes.floorId]    = UUID.fromString(floorId)
                it[Nodes.buildingId] = UUID.fromString(buildingId)
                it[Nodes.label]      = label
                it[Nodes.nodeType]   = nodeType
                it[Nodes.canvasX]    = canvasX
                it[Nodes.canvasY]    = canvasY
                it[Nodes.worldX]     = wx
                it[Nodes.worldY]     = wy
                it[Nodes.worldZ]     = wz
                it[Nodes.metadata]   = metadata
                it[Nodes.createdAt]  = now
                it[Nodes.updatedAt]  = now
            }

            Nodes.selectAll().where { Nodes.id eq id }.single().let { rowToResponse(it) }
        }
    }

    suspend fun listByBuilding(buildingId: String, managerId: String): List<NodeResponse>? =
        withContext(Dispatchers.IO) {
            transaction {
                val exists = Buildings.selectAll()
                    .where {
                        (Buildings.id eq UUID.fromString(buildingId)) and
                            (Buildings.managerId eq UUID.fromString(managerId))
                    }
                    .singleOrNull() ?: return@transaction null

                Nodes.selectAll()
                    .where { Nodes.buildingId eq UUID.fromString(buildingId) }
                    .map { rowToResponse(it) }
            }
        }

    suspend fun list(floorId: String, managerId: String): List<NodeResponse>? =
        withContext(Dispatchers.IO) {
            transaction {
                val exists = Floors.innerJoin(Buildings)
                    .selectAll()
                    .where {
                        (Floors.id eq UUID.fromString(floorId)) and
                            (Buildings.managerId eq UUID.fromString(managerId))
                    }
                    .singleOrNull() ?: return@transaction null

                Nodes.selectAll()
                    .where { Nodes.floorId eq UUID.fromString(floorId) }
                    .map { rowToResponse(it) }
            }
        }

    suspend fun update(
        nodeId: String,
        managerId: String,
        label: String?,
        nodeType: String?,
        canvasX: Double?,
        canvasY: Double?,
        metadata: String?,
    ): NodeResponse? = withContext(Dispatchers.IO) {
        transaction {
            val nodeRow = Nodes
                .innerJoin(Floors, { Nodes.floorId }, { Floors.id })
                .innerJoin(Buildings, { Floors.buildingId }, { Buildings.id })
                .selectAll()
                .where {
                    (Nodes.id eq UUID.fromString(nodeId)) and
                        (Buildings.managerId eq UUID.fromString(managerId))
                }
                .singleOrNull() ?: return@transaction null

            val newCanvasX = canvasX ?: nodeRow[Nodes.canvasX]
            val newCanvasY = canvasY ?: nodeRow[Nodes.canvasY]

            val floorRow = Floors.selectAll()
                .where { Floors.id eq nodeRow[Nodes.floorId] }
                .single()

            val (wx, wy, wz) = computeWorldCoords(newCanvasX, newCanvasY, floorRow)

            Nodes.update({ Nodes.id eq UUID.fromString(nodeId) }) {
                label?.let { v -> it[Nodes.label]    = v }
                nodeType?.let { v -> it[Nodes.nodeType] = v }
                it[Nodes.canvasX]  = newCanvasX
                it[Nodes.canvasY]  = newCanvasY
                it[Nodes.worldX]   = wx
                it[Nodes.worldY]   = wy
                it[Nodes.worldZ]   = wz
                metadata?.let { v -> it[Nodes.metadata] = v }
                it[Nodes.updatedAt] = Instant.now()
            }

            Nodes.selectAll().where { Nodes.id eq UUID.fromString(nodeId) }
                .single().let { rowToResponse(it) }
        }
    }

    suspend fun delete(nodeId: String, managerId: String): Boolean =
        withContext(Dispatchers.IO) {
            transaction {
                val exists = Nodes
                    .innerJoin(Floors, { Nodes.floorId }, { Floors.id })
                    .innerJoin(Buildings, { Floors.buildingId }, { Buildings.id })
                    .selectAll()
                    .where {
                        (Nodes.id eq UUID.fromString(nodeId)) and
                            (Buildings.managerId eq UUID.fromString(managerId))
                    }
                    .singleOrNull() ?: return@transaction false

                Nodes.deleteWhere { Nodes.id eq UUID.fromString(nodeId) } > 0
            }
        }

    private fun computeWorldCoords(
        cx: Double,
        cy: Double,
        floorRow: ResultRow,
    ): Triple<Double?, Double?, Double?> {
        val minX = floorRow[Floors.boundsMinX] ?: return Triple(null, null, null)
        val maxX = floorRow[Floors.boundsMaxX] ?: return Triple(null, null, null)
        val minZ = floorRow[Floors.boundsMinZ] ?: return Triple(null, null, null)
        val maxZ = floorRow[Floors.boundsMaxZ] ?: return Triple(null, null, null)
        val fy   = floorRow[Floors.floorY] ?: 0.0
        return Triple(
            minX + cx * (maxX - minX),
            fy,
            minZ + cy * (maxZ - minZ),
        )
    }

    private fun rowToResponse(row: ResultRow): NodeResponse = NodeResponse(
        id         = row[Nodes.id].value.toString(),
        floorId    = row[Nodes.floorId].value.toString(),
        buildingId = row[Nodes.buildingId].value.toString(),
        label      = row[Nodes.label],
        nodeType   = row[Nodes.nodeType],
        canvasX    = row[Nodes.canvasX],
        canvasY    = row[Nodes.canvasY],
        worldX     = row[Nodes.worldX],
        worldY     = row[Nodes.worldY],
        worldZ     = row[Nodes.worldZ],
        metadata   = row[Nodes.metadata],
        createdAt  = row[Nodes.createdAt].toString(),
        updatedAt  = row[Nodes.updatedAt].toString(),
    )
}
