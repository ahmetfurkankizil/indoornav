package com.Vectura AI.tools.admin.db.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp

object Managers : UUIDTable("managers") {
    val email        = varchar("email", 320).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val fullName     = varchar("full_name", 255)
    val createdAt    = timestamp("created_at")
    val updatedAt    = timestamp("updated_at")
}
