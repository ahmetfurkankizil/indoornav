package com.vecturai.tools.admin.service

import com.vecturai.tools.admin.db.tables.Buildings
import com.vecturai.tools.admin.db.tables.Floors
import com.vecturai.tools.admin.model.FloorBoundsRequest
import com.vecturai.tools.admin.model.FloorResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Path
import java.time.Instant
import java.util.UUID

class FloorService(private val uploadsDir: String) {

    suspend fun create(
        buildingId: String,
        managerId: String,
        floorNumber: Int,
        floorName: String,
        glbBytes: ByteArray,
    ): FloorResponse? = withContext(Dispatchers.IO) {
        transaction {
            // Verify manager owns this building
            val building = Buildings.selectAll()
                .where {
                    (Buildings.id eq UUID.fromString(buildingId)) and
                        (Buildings.managerId eq UUID.fromString(managerId))
                }
                .singleOrNull() ?: return@transaction null

            val now = Instant.now()
            val id = Floors.insertAndGetId {
                it[Floors.buildingId]   = UUID.fromString(buildingId)
                it[Floors.floorNumber]  = floorNumber
                it[Floors.floorName]    = floorName
                it[Floors.glbFilePath]  = "pending"
                it[Floors.uploadStatus] = "ready"
                it[Floors.createdAt]    = now
                it[Floors.updatedAt]    = now
            }

            val path = saveGlbFile(buildingId, id.value.toString(), glbBytes)
            Floors.update({ Floors.id eq id }) {
                it[Floors.glbFilePath] = path
            }

            Floors.selectAll().where { Floors.id eq id }.single().let { rowToResponse(it) }
        }
    }

    suspend fun list(buildingId: String, managerId: String): List<FloorResponse>? =
        withContext(Dispatchers.IO) {
            transaction {
                val exists = Buildings.selectAll()
                    .where {
                        (Buildings.id eq UUID.fromString(buildingId)) and
                            (Buildings.managerId eq UUID.fromString(managerId))
                    }
                    .singleOrNull() ?: return@transaction null

                Floors.selectAll()
                    .where { Floors.buildingId eq UUID.fromString(buildingId) }
                    .orderBy(Floors.floorNumber to SortOrder.ASC)
                    .map { rowToResponse(it) }
            }
        }

    suspend fun getById(floorId: String, managerId: String): FloorResponse? =
        withContext(Dispatchers.IO) {
            transaction {
                val row = Floors.innerJoin(Buildings)
                    .selectAll()
                    .where {
                        (Floors.id eq UUID.fromString(floorId)) and
                            (Buildings.managerId eq UUID.fromString(managerId))
                    }
                    .singleOrNull() ?: return@transaction null
                rowToResponse(row)
            }
        }

    suspend fun replaceGlb(
        floorId: String,
        managerId: String,
        glbBytes: ByteArray,
        floorName: String?,
        floorNumber: Int?,
    ): FloorResponse? = withContext(Dispatchers.IO) {
        transaction {
            val row = Floors.innerJoin(Buildings)
                .selectAll()
                .where {
                    (Floors.id eq UUID.fromString(floorId)) and
                        (Buildings.managerId eq UUID.fromString(managerId))
                }
                .singleOrNull() ?: return@transaction null

            val buildingId = row[Floors.buildingId].value.toString()
            val path = saveGlbFile(buildingId, floorId, glbBytes)

            Floors.update({ Floors.id eq UUID.fromString(floorId) }) {
                it[Floors.glbFilePath]  = path
                it[Floors.uploadStatus] = "ready"
                floorName?.let { n -> it[Floors.floorName] = n }
                floorNumber?.let { n -> it[Floors.floorNumber] = n }
                // Reset bounds since GLB changed — browser must re-extract
                it[Floors.boundsMinX] = null
                it[Floors.boundsMaxX] = null
                it[Floors.boundsMinZ] = null
                it[Floors.boundsMaxZ] = null
                it[Floors.floorY]     = null
                it[Floors.updatedAt]  = Instant.now()
            }

            Floors.selectAll().where { Floors.id eq UUID.fromString(floorId) }
                .single().let { rowToResponse(it) }
        }
    }

    suspend fun updateBounds(
        floorId: String,
        managerId: String,
        bounds: FloorBoundsRequest,
    ): FloorResponse? = withContext(Dispatchers.IO) {
        transaction {
            val exists = Floors.innerJoin(Buildings)
                .selectAll()
                .where {
                    (Floors.id eq UUID.fromString(floorId)) and
                        (Buildings.managerId eq UUID.fromString(managerId))
                }
                .singleOrNull() ?: return@transaction null

            Floors.update({ Floors.id eq UUID.fromString(floorId) }) {
                it[Floors.boundsMinX]  = bounds.minX
                it[Floors.boundsMaxX]  = bounds.maxX
                it[Floors.boundsMinZ]  = bounds.minZ
                it[Floors.boundsMaxZ]  = bounds.maxZ
                it[Floors.floorY]      = bounds.floorY
                it[Floors.updatedAt]   = Instant.now()
            }

            Floors.selectAll().where { Floors.id eq UUID.fromString(floorId) }
                .single().let { rowToResponse(it) }
        }
    }

    suspend fun delete(floorId: String, managerId: String): Boolean =
        withContext(Dispatchers.IO) {
            transaction {
                val row = Floors.innerJoin(Buildings)
                    .selectAll()
                    .where {
                        (Floors.id eq UUID.fromString(floorId)) and
                            (Buildings.managerId eq UUID.fromString(managerId))
                    }
                    .singleOrNull() ?: return@transaction false

                Floors.deleteWhere { Floors.id eq UUID.fromString(floorId) } > 0
            }
        }

    fun getGlbBytes(floorId: String): ByteArray? {
        val dir = Path.of(uploadsDir, "glb").toFile()
        // Try exact match by scanning sub-dirs
        dir.listFiles()?.forEach { buildingDir ->
            val file = buildingDir.resolve("$floorId.glb")
            if (file.exists()) return file.readBytes()
        }
        return null
    }

    private fun saveGlbFile(buildingId: String, floorId: String, bytes: ByteArray): String {
        val dir = Path.of(uploadsDir, "glb", buildingId).toFile().also { it.mkdirs() }
        dir.resolve("$floorId.glb").writeBytes(bytes)
        return "uploads/glb/$buildingId/$floorId.glb"
    }

    private fun rowToResponse(row: ResultRow): FloorResponse = FloorResponse(
        id           = row[Floors.id].value.toString(),
        buildingId   = row[Floors.buildingId].value.toString(),
        floorNumber  = row[Floors.floorNumber],
        floorName    = row[Floors.floorName],
        boundsMinX   = row[Floors.boundsMinX],
        boundsMaxX   = row[Floors.boundsMaxX],
        boundsMinZ   = row[Floors.boundsMinZ],
        boundsMaxZ   = row[Floors.boundsMaxZ],
        floorY       = row[Floors.floorY],
        uploadStatus = row[Floors.uploadStatus],
        createdAt    = row[Floors.createdAt].toString(),
        updatedAt    = row[Floors.updatedAt].toString(),
    )
}
