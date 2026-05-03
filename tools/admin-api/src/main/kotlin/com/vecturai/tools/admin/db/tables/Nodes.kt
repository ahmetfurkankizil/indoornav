package com.vecturai.tools.admin.db.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp

object Nodes : UUIDTable("nodes") {
    val floorId    = reference("floor_id", Floors)
    val buildingId = reference("building_id", Buildings)
    val label      = varchar("label", 255)
    val nodeType   = varchar("node_type", 30).default("room")
    // room | corridor | entrance | elevator | stairs | exit | destination
    val canvasX    = double("canvas_x")
    val canvasY    = double("canvas_y")
    val worldX     = double("world_x").nullable()
    val worldY     = double("world_y").nullable()
    val worldZ     = double("world_z").nullable()
    val metadata   = text("metadata").default("{}")  // JSON object
    val createdAt  = timestamp("created_at")
    val updatedAt  = timestamp("updated_at")
}
