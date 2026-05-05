package com.vecturai.tools.admin.db.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp

object Floors : UUIDTable("floors") {
    val buildingId    = reference("building_id", Buildings)
    val floorNumber   = integer("floor_number")
    val floorName     = varchar("floor_name", 100)
    val glbFilePath   = varchar("glb_file_path", 512)
    val boundsMinX    = double("bounds_min_x").nullable()
    val boundsMaxX    = double("bounds_max_x").nullable()
    val boundsMinZ    = double("bounds_min_z").nullable()
    val boundsMaxZ    = double("bounds_max_z").nullable()
    val floorY        = double("floor_y").nullable()
    val uploadStatus  = varchar("upload_status", 20).default("ready")  // ready | replacing
    val createdAt     = timestamp("created_at")
    val updatedAt     = timestamp("updated_at")

    init {
        uniqueIndex(buildingId, floorNumber)
    }
}
