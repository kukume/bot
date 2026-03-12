package me.kuku.telegram.job

import io.github.dehuckakpyt.telegrambot.TelegramBot
import io.github.dehuckakpyt.telegrambot.model.telegram.input.ByteArrayContent
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.delay
import me.kuku.common.entity.Status
import me.kuku.common.entity.UserConfigEntity
import me.kuku.common.entity.UserConfigTable
import me.kuku.common.ktor.client
import me.kuku.common.logic.EpicLogic
import me.kuku.common.logic.ToolLogic
import me.kuku.common.logic.XianBao
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.koin.core.component.inject
import org.quartz.JobExecutionContext
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

private object PushData {
    var xianBaoId = 0
}

class EpicJob: CoroutineJob {
    private val telegramBot by inject<TelegramBot>()
    override suspend fun coroutineExecute(context: JobExecutionContext) {
        val queryList = suspendTransaction {
            UserConfigEntity.find {
                (UserConfigTable.epicFreeGamePush eq Status.ON)
            }.toList()
        }
        if (queryList.isEmpty()) return
        val epicList = EpicLogic.epic().filter { it.diff < 1.hours.inWholeMilliseconds }
        for (epicFreeGame in epicList) {
            queryList.push { entity ->
                telegramBot.sendPhoto(entity.identityId,
                    ByteArrayContent(client.get(epicFreeGame.imageUrl).bodyAsBytes()), caption = epicFreeGame.text())
            }
        }


    }
}

class XianBaoJob: CoroutineJob {
    private val telegramBot by inject<TelegramBot>()
    override suspend fun coroutineExecute(context: JobExecutionContext) {
        val pushList = suspendTransaction {
            UserConfigEntity.find {
                (UserConfigTable.xianBaoPush eq Status.ON)
            }.toList()
        }
        if (pushList.isEmpty()) return
        val list = ToolLogic.xianBao()
        val newList = mutableListOf<XianBao>()
        if (PushData.xianBaoId != 0) {
            for (xianBao in list) {
                if (xianBao.id <= PushData.xianBaoId) break
                newList.add(xianBao)
            }
        }
        PushData.xianBaoId = list[0].id
        for (xianBao in newList) {
            delay(3.seconds)
            pushList.push { entity ->
                val str = """
                    #线报酷推送
                    标题：${xianBao.title}
                    时间：${xianBao.datetime}
                    线报酷链接：${xianBao.urlIncludeDomain()}
                """.trimIndent()
                telegramBot.sendMessage(entity.identityId, str)
            }
        }
    }
}
