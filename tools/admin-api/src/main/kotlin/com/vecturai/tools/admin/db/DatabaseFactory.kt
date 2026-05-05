package com.vecturai.tools.admin.db

import com.vecturai.tools.admin.Env
import com.vecturai.tools.admin.db.tables.*
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {

    fun init() {
        val host     = Env.get("POSTGRES_HOST")     ?: "localhost"
        val port     = Env.get("POSTGRES_PORT")     ?: "5432"
        val db       = Env.get("POSTGRES_DB")       ?: "VecturAI"
        val user     = Env.get("POSTGRES_USER")     ?: "VecturAI"
        val password = Env.get("POSTGRES_PASSWORD") ?: "VecturAI"

        val config = HikariConfig().apply {
            jdbcUrl         = "jdbc:postgresql://$host:$port/$db"
            driverClassName = "org.postgresql.Driver"
            username        = user
            this.password   = password
            maximumPoolSize = 10
            isAutoCommit    = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }

        Database.connect(HikariDataSource(config))

        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                Managers,
                Buildings,
                Floors,
                Nodes,
                Edges,
                FloorConnections,
                NavigationPackages,
                EntranceMarkers,
                NavMeshAreas,
            )
        }
    }
}
