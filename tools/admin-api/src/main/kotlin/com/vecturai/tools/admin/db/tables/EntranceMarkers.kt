package com.Vectura AI.tools.admin.db.tables

import org.jetbrains.exposed.dao.id.UUIDTable

object EntranceMarkers : UUIDTable("entrance_markers") {
    val buildingId          = reference("building_id", Buildings)
    val displayName         = varchar("display_name", 255).nullable()
    val startNodeId         = reference("start_node_id", Nodes)
    val physicalWidthMeters  = double("physical_width_meters").default(0.2)
    val physicalHeightMeters = double("physical_height_meters").default(0.2)
    val worldX              = double("world_x").nullable()
    val worldY              = double("world_y").nullable()
    val worldZ              = double("world_z").nullable()
    val forwardBasis        = varchar("forward_basis", 20).default("X_PLUS")
    val rotationYDegrees    = double("rotation_y_degrees").nullable()
    val referenceImageName  = varchar("reference_image_name", 255).nullable()
}
