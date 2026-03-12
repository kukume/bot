package me.kuku.qqbot.ktor

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import me.kuku.qqbot.entity.MessageTable
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.core.StdOutSqlLogger
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun Application.exposed() {

    val databaseConfig = environment.config.config("database")
    val hikariConfig = HikariConfig().apply {
        driverClassName = databaseConfig.property("driver").getString()
        jdbcUrl = databaseConfig.property("url").getString()
        username = databaseConfig.property("user").getString()
        password = databaseConfig.property("password").getString()
        maximumPoolSize = 64
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_READ_COMMITTED"
        validate()
    }

    Database.connect(
        HikariDataSource(hikariConfig),
        databaseConfig = DatabaseConfig.invoke {
            sqlLogger = StdOutSqlLogger
        }
    )

    transaction {
        SchemaUtils.create(MessageTable)
    }
}
