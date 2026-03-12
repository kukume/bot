package me.kuku.telegram.job

import io.github.dehuckakpyt.telegrambot.TelegramBot
import io.github.dehuckakpyt.telegrambot.model.telegram.input.ByteArrayContent
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.delay
import me.kuku.common.entity.HuYaEntity
import me.kuku.common.entity.HuYaTable
import me.kuku.common.entity.Status
import me.kuku.common.ktor.client
import me.kuku.common.logic.HuYaLogic
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.koin.core.component.inject
import org.quartz.JobExecutionContext

private object HuYaData {
    val huYaLiveMap = mutableMapOf<Long, MutableMap<Long, Boolean>>()
}

class HuYaJob: CoroutineJob {
    private val telegramBot by inject<TelegramBot>()
    override suspend fun coroutineExecute(context: JobExecutionContext) {
        suspendTransaction {
            HuYaEntity.find { HuYaTable.push eq Status.ON }.toList()
        }.push { entity ->
            delay(3000)
            val huYaLiveMap = HuYaData.huYaLiveMap
            val lives = HuYaLogic.live(entity)
            val tgId = entity.identityId.toLong()
            if (!huYaLiveMap.containsKey(tgId)) huYaLiveMap[tgId] = mutableMapOf()
            val map = huYaLiveMap[tgId]!!
            for (room in lives) {
                val id = room.roomId
                val b = room.isLive
                if (map.containsKey(id)) {
                    if (map[id] != b) {
                        map[id] = b
                        val msg = if (b) "直播啦！！" else "下播啦"
                        val text = "#虎牙开播提醒\n#${room.nick} $msg\n标题：${room.liveDesc}\n分类：${room.gameName}\n链接：${room.url}"
                        val videoCaptureUrl = room.videoCaptureUrl
                        if (videoCaptureUrl.isEmpty()) telegramBot.sendMessage(tgId, text)
                        else {
                            client.get(videoCaptureUrl).body<ByteArray>().let {
                                telegramBot.sendPhoto(tgId, ByteArrayContent(it), caption = text)
                            }
                        }
                    }
                } else map[id] = b
            }
        }
    }
}
