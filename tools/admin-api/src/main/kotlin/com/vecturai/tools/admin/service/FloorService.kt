package com.Vectura AI.tools.admin.service

import com.Vectura AI.tools.admin.db.tables.Buildings
import com.Vectura AI.tools.admin.db.tables.Floors
import com.Vectura AI.tools.admin.model.FloorBoundsRequest
import com.Vectura AI.tools.admin.model.FloorResponse
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
        fileExtension: String = "glb",
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

            val path = saveMapFile(buildingId, id.value.toString(), fileExtension, glbBytes)
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
        fileExtension: String = "glb",
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
            val path = saveMapFile(buildingId, floorId, fileExtension, glbBytes)

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

    fun getGlbBytes(floorId: String): ByteArray? = getMapFile(floorId)?.first

    fun getMapFile(floorId: String): Pair<ByteArray, String>? {
        val dir = Path.of(uploadsDir, "glb").toFile()
        val extensions = listOf("glb", "svg", "png", "dxf")
        dir.listFiles()?.forEach { buildingDir ->
            for (ext in extensions) {
                val file = buildingDir.resolve("$floorId.$ext")
                if (file.exists()) return Pair(file.readBytes(), ext)
            }
        }
        return null
    }

    private fun saveMapFile(buildingId: String, floorId: String, extension: String, bytes: ByteArray): String {
        val dir = Path.of(uploadsDir, "glb", buildingId).toFile().also { it.mkdirs() }
        // Remove any previously stored file with a different extension before saving
        listOf("glb", "svg", "png", "dxf").forEach { ext ->
            dir.resolve("$floorId.$ext").takeIf { it.exists() }?.delete()
        }
        dir.resolve("$floorId.$extension").writeBytes(bytes)
        return "uploads/glb/$buildingId/$floorId.$extension"
    }

    private fun rowToResponse(row: ResultRow): FloorResponse {
        val storedPath = row[Floors.glbFilePath]
        val mapFileType = storedPath.substringAfterLast('.', "glb")
            .lowercase()
            .let { if (it in setOf("glb", "svg", "png", "dxf")) it else "glb" }
        return FloorResponse(
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
            mapFileType  = mapFileType,
            createdAt    = row[Floors.createdAt].toString(),
            updatedAt    = row[Floors.updatedAt].toString(),
        )
    }
}
