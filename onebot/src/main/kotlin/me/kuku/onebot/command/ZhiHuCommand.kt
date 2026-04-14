package me.kuku.onebot.command

import cn.rtast.rob.BotInstance
import cn.rtast.rob.command.BaseCommand
import cn.rtast.rob.entity.toResource
import cn.rtast.rob.enums.SegmentType
import cn.rtast.rob.event.onEvent
import cn.rtast.rob.event.packed.GroupMessageEvent
import cn.rtast.rob.event.raw.message.GroupMessage
import cn.rtast.rob.onebot.dsl.image
import cn.rtast.rob.onebot.dsl.messageChain
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.parameters
import me.kuku.common.ktor.client
import me.kuku.common.utils.toJsonNode
import me.kuku.onebot.config.ROneBot
import org.koin.core.annotation.Single

@Single
class ZhiHuCommand: BaseCommand() {

    override val commandNames = listOf("zh")

    override suspend fun executeGroup(
        message: GroupMessage,
        args: List<String>
    ) {
        val all = args.joinToString(" ")
        val regex = """https://www\.zhihu\.com/question/\d+/answer/\d+""".toRegex()
        val url = regex.find(all)?.value ?: return
        val ba = zhiHuPic(url)
        message.reply(messageChain {
            image(ba.toResource())
        })
    }
}

@Single
class NsCommand: BaseCommand() {
    override val commandNames = listOf("ns")

    override suspend fun executeGroup(
        message: GroupMessage,
        args: List<String>
    ) {
        val all = args.joinToString(" ")
        val regex = """https://www\.nodeseek\.com/post-\d+-1""".toRegex()
        val url = regex.find(all)?.value ?: return
        val ba = zhiHuPic(url)
        message.reply(messageChain {
            image(ba.toResource())
        })
    }
}

@Single
class LinuxDoCommand: BaseCommand() {
    override val commandNames = listOf("ld")

    override suspend fun executeGroup(
        message: GroupMessage,
        args: List<String>
    ) {
        val all = args.joinToString(" ")
        val regex = """https://linux\.do/t/topic/\d+""".toRegex()
        val url = regex.find(all)?.value ?: return
        val ba = zhiHuPic(url)
        message.reply(messageChain {
            image(ba.toResource())
        })
    }
}

suspend fun zhiHuPic(url: String): ByteArray {
    return client.submitForm(
        url = "http://localhost:38127/render",
        parameters { append("url", url) }
    ).bodyAsBytes()
}

@Single
class CheckZhiHu: ROneBot {
    override fun BotInstance.execute() {
        onEvent<GroupMessageEvent> { event ->
            val jsonNode = event.message.message.find { it.type == SegmentType.json }?.data?.data?.toJsonNode() ?: return@onEvent
            val url = jsonNode["meta"]?.get("detail_1")?.get("qqdocurl")?.asText()
                ?.takeIf { it.contains("www.zhihu.com/question") } ?: return@onEvent
            val ba = zhiHuPic(url)
            event.reply(messageChain {
                image(ba.toResource())
            })
        }
    }
}