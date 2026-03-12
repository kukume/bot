package me.kuku.common.ktor.plugins

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.createApplicationPlugin
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.core.StdOutSqlLogger
import org.jetbrains.exposed.v1.jdbc.Database

val Exposed = createApplicationPlugin("Exposed", createConfiguration = { ExposedConfig() }) {
    val hikariConfig = pluginConfig.hikariConfig
    hikariConfig.apply {
        if (driverClassName == null) driverClassName = System.getenv("DATABASE_DRIVER")
        if (jdbcUrl == null) jdbcUrl = System.getenv("DATABASE_URL")
        if (username == null) username = System.getenv("DATABASE_USER")
        if (password == null) password = System.getenv("DATABASE_PASSWORD")
        validate()
    }

    Database.connect(
        HikariDataSource(hikariConfig),
        databaseConfig = DatabaseConfig.invoke {
            sqlLogger = StdOutSqlLogger
        }
    )
}


class ExposedConfig internal constructor() {

    internal val hikariConfig = HikariConfig()
    internal var databaseConfig = DatabaseConfig()

    fun hikariConfig(setupHikari: HikariConfig.() -> Unit) {
        hikariConfig.apply {
            maximumPoolSize = 64
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_READ_COMMITTED"
        }
        setupHikari.invoke(hikariConfig)
    }

    fun databaseConfig(setupDatabase: DatabaseConfig.Builder.() -> Unit) {
        databaseConfig = DatabaseConfig.invoke {
            sqlLogger = StdOutSqlLogger
            setupDatabase.invoke(this)
        }
    }


}
