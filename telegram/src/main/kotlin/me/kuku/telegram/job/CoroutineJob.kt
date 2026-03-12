package me.kuku.telegram.job

import io.github.dehuckakpyt.telegrambot.TelegramBot
import io.github.dehuckakpyt.telegrambot.exception.api.TelegramBotApiException
import kotlinx.coroutines.runBlocking
import me.kuku.common.entity.*
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.koin.core.component.KoinComponent
import org.koin.mp.KoinPlatform.getKoin
import org.quartz.Job
import org.quartz.JobExecutionContext
import kotlin.reflect.full.declaredMembers

interface CoroutineJob: Job, KoinComponent {

    override fun execute(context: JobExecutionContext) {
        runBlocking { coroutineExecute(context) }
    }

    suspend fun coroutineExecute(context: JobExecutionContext)
}

suspend inline fun <reified T: IntEntity> Iterable<T>.sign(type: LogType, block: (T) -> Any?) {
    this.forEach { entity ->
        val clazz = entity::class.declaredMembers
        val identityId = clazz.find { members -> members.name == "identityId" }?.call(entity)?.toString() ?: error("identityId not found")
        val identityName = clazz.find { members -> members.name == "identityName" }?.call(entity)?.toString() ?: error("identityName not found")
        try {
            val remark = block(entity)
            suspendTransaction {
                SignLogTable.insert {
                    it[SignLogTable.identityId] = identityId
                    it[SignLogTable.identityName] = identityName
                    it[SignLogTable.type] = type
                    it[success] = true
                    if (remark is String) {
                        it[SignLogTable.remark] = remark
                    }
                }
            }
        } catch (e: Exception) {
            suspendTransaction {
                SignLogTable.insert {
                    it[SignLogTable.identityId] = identityId
                    it[SignLogTable.identityName] = identityName
                    it[SignLogTable.type] = type
                    it[success] = false
                    it[errReason] = e.message
                    it[exceptionStack] = e.stackTraceToString()
                }
            }
            getKoin().get<TelegramBot>().sendMessage(identityId,
                "#自动签到失败提醒\n${type.value}执行失败，${e.message ?: "未知异常原因，请重新执行指令以查看原因"}")
        }
    }
}

suspend inline fun <reified T: IntEntity> Iterable<T>.push(block: (T) -> Unit) {
    this.forEach { entity ->
        try {
            block(entity)
        } catch (e: TelegramBotApiException) {
            val message = e.message
            if (message != null && message.contains("bot was blocked by the user")) {
                // clearAll
                val clazz = entity::class.declaredMembers
                val identityId = clazz.find { members -> members.name == "identityId" }?.call(entity)?.toString()
                if (identityId != null) {
                    suspendTransaction {
                        BaiduTable.deleteWhere { BaiduTable.identityId eq identityId }
                        BiliBiliTable.deleteWhere { BiliBiliTable.identityId eq identityId }
                        DouYuTable.deleteWhere { DouYuTable.identityId eq identityId }
                        ECloudTable.deleteWhere { ECloudTable.identityId eq identityId }
                        HostLocTable.deleteWhere { HostLocTable.identityId eq identityId }
                        HuYaTable.deleteWhere { HuYaTable.identityId eq identityId }
                        IdentityTable.deleteWhere { IdentityTable.identityId eq identityId }
                        KuGouTable.deleteWhere { KuGouTable.identityId eq identityId }
                        LeiShenTable.deleteWhere { LeiShenTable.identityId eq identityId }
                        MiHoYoTable.deleteWhere { MiHoYoTable.identityId eq identityId }
                        NodeSeekTable.deleteWhere { NodeSeekTable.identityId eq identityId }
                        SignLogTable.deleteWhere { SignLogTable.identityId eq identityId }
                        SmZdmTable.deleteWhere { SmZdmTable.identityId eq identityId }
                        StepTable.deleteWhere { StepTable.identityId eq identityId }
                        UserConfigTable.deleteWhere { UserConfigTable.identityId eq identityId }
                        WeiboTable.deleteWhere { WeiboTable.identityId eq identityId }
                    }
                }

            }
            e.printStackTrace()
        }
    }
}
