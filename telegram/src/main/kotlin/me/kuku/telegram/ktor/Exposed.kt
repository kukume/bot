package me.kuku.telegram.ktor

import io.ktor.server.application.*
import me.kuku.common.entity.*
import me.kuku.common.ktor.plugins.Exposed
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

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
            NodeLocTable
        )
    }

}
