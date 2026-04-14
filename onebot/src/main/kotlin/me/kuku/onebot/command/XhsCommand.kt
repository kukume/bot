package me.kuku.onebot.command

import cn.rtast.rob.BotInstance
import cn.rtast.rob.command.BaseCommand
import cn.rtast.rob.entity.toResource
import cn.rtast.rob.enums.SegmentType
import cn.rtast.rob.event.onEvent
import cn.rtast.rob.event.packed.GroupMessageEvent
import cn.rtast.rob.event.raw.message.GroupMessage
import cn.rtast.rob.onebot.NodeMessageChain
import cn.rtast.rob.onebot.dsl.image
import cn.rtast.rob.onebot.dsl.messageChain
import cn.rtast.rob.onebot.dsl.nodeMessageChain
import cn.rtast.rob.onebot.dsl.text
import cn.rtast.rob.onebot.dsl.video
import me.kuku.common.logic.XhsDetail
import me.kuku.common.logic.XhsLogic
import me.kuku.common.utils.toJsonNode
import me.kuku.onebot.config.ROneBot
import org.koin.core.annotation.Single

@Single
class XhsCommand: BaseCommand() {
    override val commandNames = listOf("xhs")

    override suspend fun executeGroup(
        message: GroupMessage,
        args: List<String>
    ) {
        val url = args[0]
        val detail = XhsLogic.detail(url)
        message.reply(buildMessage(detail, message.action.getLoginInfo().userId))
        val ba = zhiHuPic(args[0])
        message.reply(messageChain {
            image(ba.toResource())
        })
    }
}

private fun buildMessage(detail: XhsDetail, userId: Long): NodeMessageChain {
    val text = "标题：${detail.title}\n描述：${detail.description}\n最后更新时间：${detail.updateTime}\n作者：${detail.username}"
    val downloadUrls = detail.downloadUrls
    val images = downloadUrls.filter { !it.endsWith(".mp4") }
    val videos = downloadUrls.filter { it.endsWith(".mp4") }
    return nodeMessageChain  {
        messageChain(userId) {
            text(text)
        }
        videos.forEach { video ->
            messageChain(userId) {
                video(video)
            }
        }
        images.forEach { image ->
            messageChain(userId) {
                image(image.toResource())
            }
        }
    }
}

@Single
class CheckXhs: ROneBot {
    override fun BotInstance.execute() {
        onEvent<GroupMessageEvent> { event ->
            val jsonNode = event.message.message.find { it.type == SegmentType.json }?.data?.data?.toJsonNode() ?: return@onEvent
            val url = jsonNode["meta"]?.get("news")?.get("jumpUrl")?.asText()
                ?.takeIf { it.contains("www.xiaohongshu.com") || it.contains("xhslink.com") } ?: return@onEvent
            val detail = XhsLogic.detail(url)
            event.reply(buildMessage(detail, event.action.getLoginInfo().userId))
            val ba = zhiHuPic(url)
            event.reply(messageChain {
                image(ba.toResource())
            })
        }
    }
}