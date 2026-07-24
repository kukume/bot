package me.kuku.telegram.ktor

import io.github.dehuckakpyt.telegrambot.model.CallbackContents
import io.ktor.server.application.*
import me.kuku.common.entity.*
import me.kuku.common.ktor.plugins.Exposed
import me.kuku.telegram.handler.MessageTokenTable
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.migration.jdbc.MigrationUtils

fun Application.exposed() {

    install(Exposed)


    transaction {
        SchemaUtils.create(
            BaiduTable,
            BiliBiliTable,
            DouYuTable,
            ECloudTable,
            HostLocTable,
            HuYaTable,
            KuGouTable,
            LeiShenTable,
            MiHoYoTable,
            NodeSeekTable,
            SmZdmTable,
            StepTable,
            WeiboTable,
            SignLogTable,
            UserConfigTable,
            IdentityTable,
            NodeLocTable,
            MessageTokenTable
        )

        transaction {
            MigrationUtils.statementsRequiredForDatabaseMigration(
            )
        }
    }

}
