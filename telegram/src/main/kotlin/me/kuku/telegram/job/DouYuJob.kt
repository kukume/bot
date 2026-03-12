package me.kuku.telegram.job

import io.github.dehuckakpyt.telegrambot.TelegramBot
import io.github.dehuckakpyt.telegrambot.model.telegram.InputMediaPhoto
import io.github.dehuckakpyt.telegrambot.model.telegram.input.ByteArrayContent
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.delay
import me.kuku.common.entity.*
import me.kuku.common.ktor.client
import me.kuku.common.logic.DouYuFish
import me.kuku.common.logic.DouYuLogic
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.koin.core.component.inject
import org.quartz.JobExecutionContext

private object DouYuData {
    val douYuLiveMap = mutableMapOf<Long, MutableMap<Long, Boolean>>()
    val douYuTitleMap = mutableMapOf<Long, MutableMap<Long, String>>()
    val douYuPushMap = mutableMapOf<Long, Long>()
}

class DouYuLiveJob: CoroutineJob {
    private val telegramBot by inject<TelegramBot>()
    override suspend fun coroutineExecute(context: JobExecutionContext) {
        suspendTransaction {
            DouYuEntity.find { DouYuTable.live eq Status.ON }.toList()
        }.push { entity ->
            delay(3000)
            val douYuLiveMap = DouYuData.douYuLiveMap
            val rooms = DouYuLogic.room(entity)
            val tgId = entity.identityId.toLong()
            if (!douYuLiveMap.containsKey(tgId)) douYuLiveMap[tgId] = mutableMapOf()
            val map = douYuLiveMap[tgId]!!
            for (room in rooms) {
                val id = room.roomId
                val b = room.showStatus
                if (map.containsKey(id)) {
                    if (map[id] != b) {
                        map[id] = b
                        val msg = if (b) "直播啦！！" else "下播啦"
                        val text = "#斗鱼开播提醒\n#${room.nickName} $msg\n标题：${room.name}\n分类：${room.gameName}\n在线：${room.online}\n链接：${room.url}"
                        val imageUrl = room.imageUrl
                        if (imageUrl.isNotEmpty()) {
                            client.get(imageUrl).body<ByteArray>().let {
                                telegramBot.sendPhoto(tgId, ByteArrayContent(it))
                            }
                        } else telegramBot.sendMessage(tgId, text)
                    }
                } else map[id] = b
            }
        }
    }
}

class DouYuTitleChangeJob: CoroutineJob {
    private val telegramBot by inject<TelegramBot>()
    override suspend fun coroutineExecute(context: JobExecutionContext) {
        suspendTransaction {
            DouYuEntity.find { DouYuTable.titleChange eq Status.ON }.toList()
        }.push { entity ->
            val douYuTitleMap = DouYuData.douYuTitleMap
            val data = DouYuLogic.room(entity)
            delay(3000)
            val tgId = entity.identityId.toLong()
            if (!douYuTitleMap.containsKey(tgId)) douYuTitleMap[tgId] = mutableMapOf()
            val map = douYuTitleMap[tgId]!!
            for (room in data) {
                val name = room.name
                val roomId = room.roomId
                val value = map[roomId]
                if (value != null && value != name) {
                    val text = "#斗鱼标题更新提醒\n${room.nickName}\n旧标题：${value}\n新标题：${name}\n链接：${room.url}"
                    val imageUrl = room.imageUrl
                    if (imageUrl.isNotEmpty()) {
                        client.get(imageUrl).body<ByteArray>().let {
                            telegramBot.sendPhoto(tgId, ByteArrayContent(it), caption = text)
                        }
                    } else telegramBot.sendMessage(tgId, text)
                }
                map[roomId] = name
            }
        }
    }
}

class DouYuPushJob: CoroutineJob {
    private val telegramBot by inject<TelegramBot>()
    override suspend fun coroutineExecute(context: JobExecutionContext) {
        suspendTransaction {
            DouYuEntity.find { DouYuTable.push eq Status.ON }.toList()
        }.push { entity ->
            val douYuPushMap = DouYuData.douYuPushMap
            val tgId = entity.identityId.toLong()
            val list = DouYuLogic.focusFishGroup(entity)
            val newList = mutableListOf<DouYuFish>()
            if (douYuPushMap.containsKey(tgId)) {
                val oldId = douYuPushMap[tgId]!!
                for (biliBiliPojo in list) {
                    if (biliBiliPojo.id <= oldId) break
                    newList.add(biliBiliPojo)
                }
                for (douYuFish in newList) {
                    val text = "#斗鱼鱼吧动态推送\n#${douYuFish.nickname}\n标题：${douYuFish.title}\n内容：${douYuFish.content}\n链接：${douYuFish.url}"
                    if (douYuFish.image.isNotEmpty()) {
                        val inputMedia = douYuFish.image.map { InputMediaPhoto(it, caption = text) }
                        telegramBot.sendMediaGroup(tgId, inputMedia)
                    } else {
                        telegramBot.sendMessage(tgId, text)
                    }
                }
            }
            douYuPushMap[tgId] = list[0].id
        }
    }
}

class DouYuSignJob: CoroutineJob {
    override suspend fun coroutineExecute(context: JobExecutionContext) {
        suspendTransaction { DouYuEntity.find { DouYuTable.fishGroup eq Status.ON }.toList() }
            .sign(LogType.YuBa) { entity ->
                delay(3000)
                DouYuLogic.fishGroup(entity)
            }
    }
}
