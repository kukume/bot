package me.kuku.telegram.job

import io.github.dehuckakpyt.telegrambot.TelegramBot
import io.github.dehuckakpyt.telegrambot.model.telegram.InputMediaPhoto
import io.github.dehuckakpyt.telegrambot.model.telegram.input.ByteArrayContent
import io.github.dehuckakpyt.telegrambot.model.telegram.input.NamedFileContent
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.delay
import me.kuku.common.entity.*
import me.kuku.common.ktor.client
import me.kuku.common.logic.BiliBiliLogic
import me.kuku.common.logic.BiliBiliPojo
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.koin.core.component.inject
import org.quartz.JobExecutionContext
import java.io.File

private object BiliBiliData {
    val liveMap = mutableMapOf<Long, MutableMap<Long, Boolean>>()
    val userMap = mutableMapOf<Long, Long>()
}

class BiliBiliSignJob: CoroutineJob {
    // 0 23 3 * * ?
    override suspend fun coroutineExecute(context: JobExecutionContext) {
        suspendTransaction {
            BiliBiliEntity.find { BiliBiliTable.sign eq Status.ON }.toList()
        }.sign(LogType.BiliBili) { entity ->
            val firstRank = BiliBiliLogic.ranking(entity)[0]
            delay(5000)
            BiliBiliLogic.watchVideo(entity, firstRank)
            delay(5000)
            BiliBiliLogic.share(entity, firstRank.aid)
        }
    }
}


class BiliBiliLiveJob: CoroutineJob {
    private val telegrambot by inject<TelegramBot>()
    // 2 min
    override suspend fun coroutineExecute(context: JobExecutionContext) {
        suspendTransaction {
            BiliBiliEntity.find { BiliBiliTable.live eq Status.ON }.toList()
        }.push { entity ->
            val tgId = entity.identityId.toLong()
            val liveMap = BiliBiliData.liveMap
            if (!liveMap.containsKey(tgId)) liveMap[tgId] = mutableMapOf()
            val map = liveMap[tgId]!!
            val liveList = BiliBiliLogic.live(entity)
            for (live in liveList) {
                val userid = live.id.toLong()
                val b = live.status
                val name = live.uname
                if (map.containsKey(userid)) {
                    if (map[userid] != b) {
                        map[userid] = b
                        val msg = if (b) "直播啦！！" else "下播了！！"
                        val text = "#哔哩哔哩开播提醒\n#$name $msg\n标题：${live.title}\n链接：${live.url}"
                        val imageUrl = live.imageUrl
                        if (imageUrl.isEmpty())
                            telegrambot.sendMessage(tgId, text)
                        else {
                            client.get(imageUrl).body<ByteArray>().let {
                                telegrambot.sendPhoto(tgId, ByteArrayContent(it), caption = text)
                            }
                        }
                    }
                } else map[userid] = live.status
            }
        }
    }
}

class BiliBiliDynamicJob: CoroutineJob {

    private val telegramBot by inject<TelegramBot>()
    // 2 min
    override suspend fun coroutineExecute(context: JobExecutionContext) {
        suspendTransaction {
            BiliBiliEntity.find { BiliBiliTable.push eq Status.ON }.toList()
        }.push { entity ->
            val tgId = entity.identityId.toLong()
            val userMap = BiliBiliData.userMap
            delay(3000)
            val list = BiliBiliLogic.friendDynamic(entity)
            val newList = mutableListOf<BiliBiliPojo>()
            if (userMap.containsKey(tgId)) {
                val oldId = userMap[tgId]!!
                for (biliBiliPojo in list) {
                    if (biliBiliPojo.id.toLong() <= oldId) break
                    newList.add(biliBiliPojo)
                }
                for (biliBiliPojo in newList) {
                    val text = "#哔哩哔哩动态推送\n${BiliBiliLogic.convertStr(biliBiliPojo)}"
                    val bvId = biliBiliPojo.bvId.ifEmpty { biliBiliPojo.forwardBvId.ifEmpty { "" } }
                    try {
                        if (bvId.isNotEmpty()) {
                            var file: File? = null
                            try {
                                delay(3000)
                                file = BiliBiliLogic.videoByBvId(biliBiliPojo.bvId).file
                                telegramBot.sendVideo(tgId, NamedFileContent(file), caption = text)
                            } finally {
                                file?.delete()
                            }
                        } else if (biliBiliPojo.picList.isNotEmpty() || biliBiliPojo.forwardPicList.isNotEmpty()) {
                            val picList = biliBiliPojo.picList
                            picList.addAll(biliBiliPojo.forwardPicList)
                            val inputMedia = picList.map { InputMediaPhoto(it, caption = text) }
                            telegramBot.sendMediaGroup(tgId, inputMedia)
                        } else telegramBot.sendMessage(tgId, text)
                    } catch (e: Exception) {
                        telegramBot.sendMessage(tgId, text)
                    }
                }
            }
            userMap[tgId] = list[0].id.toLong()
        }
    }
}
