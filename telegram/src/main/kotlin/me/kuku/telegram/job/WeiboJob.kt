package me.kuku.telegram.job

import io.github.dehuckakpyt.telegrambot.TelegramBot
import io.github.dehuckakpyt.telegrambot.model.telegram.InputMediaPhoto
import io.github.dehuckakpyt.telegrambot.model.telegram.input.ByteArrayContent
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.delay
import me.kuku.common.entity.*
import me.kuku.common.ktor.client
import me.kuku.common.logic.WeiboLogic
import me.kuku.common.logic.WeiboPojo
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.koin.core.component.inject
import org.quartz.JobExecutionContext

private object WeiboData {
    val userMap = mutableMapOf<Long, Long>()
}

class WeiboSignJob: CoroutineJob {
    override suspend fun coroutineExecute(context: JobExecutionContext) {
        suspendTransaction {
            WeiboEntity.find { WeiboTable.sign eq Status.ON }.toList()
        }.sign(LogType.Weibo) { entity ->
            WeiboLogic.superTalkSign(entity)
        }
    }
}


class WeiboDynamicJob: CoroutineJob {
    private val telegramBot by inject<TelegramBot>()
    override suspend fun coroutineExecute(context: JobExecutionContext) {
        suspendTransaction {
            WeiboEntity.find { WeiboTable.push eq Status.ON }.toList()
        }.push { entity ->
            val userMap = WeiboData.userMap
            val tgId = entity.identityId.toLong()
            delay(3000)
            val list = WeiboLogic.followWeibo(entity)
            val newList = mutableListOf<WeiboPojo>()
            if (userMap.containsKey(tgId)) {
                for (weiboPojo in list) {
                    if (weiboPojo.id <= userMap[tgId]!!) break
                    newList.add(weiboPojo)
                }
                for (weiboPojo in newList) {
                    val ownText = if (weiboPojo.longText) WeiboLogic.longText(entity, weiboPojo.bid) else weiboPojo.text
                    val forwardText = if (weiboPojo.forwardLongText) WeiboLogic.longText(entity, weiboPojo.forwardBid) else weiboPojo.forwardText
                    val text = "#微博动态推送\n${WeiboLogic.convert(weiboPojo, ownText, forwardText)}"
                    val videoUrl = if (weiboPojo.videoUrl.isNotEmpty()) weiboPojo.videoUrl
                    else if (weiboPojo.forwardVideoUrl.isNotEmpty()) weiboPojo.forwardVideoUrl
                    else ""
                    try {
                        if (videoUrl.isNotEmpty()) {
                            client.get(videoUrl).body<ByteArray>().let {
                                telegramBot.sendVideo(tgId, ByteArrayContent(it))
                            }
                        } else if (weiboPojo.imageUrl.isNotEmpty() || weiboPojo.forwardImageUrl.isNotEmpty()) {
                            val imageList = weiboPojo.imageUrl
                            imageList.addAll(weiboPojo.forwardImageUrl)
                            val mediaPhotos = imageList.map { InputMediaPhoto(it, caption = text) }
                            telegramBot.sendMediaGroup(tgId, mediaPhotos)
                        } else telegramBot.sendMessage(tgId, text)
                    } catch (e: Exception) {
                        telegramBot.sendMessage(tgId, text)
                    }
                }
            }
            userMap[tgId] = list[0].id
        }
    }
}
