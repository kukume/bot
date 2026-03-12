package me.kuku.onebot.command

import cn.rtast.rob.annotations.ExperimentalROneBotApi
import cn.rtast.rob.command.BaseCommand
import cn.rtast.rob.entity.toResource
import cn.rtast.rob.enums.SegmentType
import cn.rtast.rob.event.raw.message.GroupMessage
import cn.rtast.rob.event.raw.message.PrivateMessage
import cn.rtast.rob.event.raw.message.text
import cn.rtast.rob.onebot.dsl.image
import cn.rtast.rob.onebot.dsl.messageChain
import cn.rtast.rob.onebot.dsl.record
import cn.rtast.rob.onebot.dsl.text
import cn.rtast.rob.onebot.dsl.video
import cn.rtast.rob.session.accept
import cn.rtast.rob.session.reject
import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.kuku.common.ktor.client
import me.kuku.common.logic.BiliBiliLogic
import me.kuku.common.logic.EpicLogic
import me.kuku.common.logic.JmComicLogic
import me.kuku.common.logic.MusicLogic
import me.kuku.common.logic.ToolLogic
import me.kuku.common.utils.toJsonNode
import me.kuku.common.utils.S3Utils
import org.koin.core.annotation.Single

private val sendVideoMutex = Mutex()

@Single
class St: BaseCommand() {
    override val commandNames = listOf("st")

    override suspend fun executeGroup(
        message: GroupMessage,
        args: List<String>
    ) {
        val jsonNode = client.get("https://api.lolicon.app/setu/v2").body<JsonNode>()
        val quickUrl = jsonNode["data"][0]["urls"]["original"].asText()
        message.reply(messageChain {
            image(quickUrl.toResource())
        })
    }
}

@Single
class Bv: BaseCommand() {
    override val commandNames = listOf("bv")

    override suspend fun executeGroup(
        message: GroupMessage,
        args: List<String>
    ) {
        sendVideoMutex.withLock {
            val replyId = message.reply("发送视频中，请稍后")
            val video = BiliBiliLogic.videoByBvId(args.joinToString(" "))
            val file = video.file
            message.reply(messageChain {
                image(video.pic.toResource())
                text("${video.title}\n${video.desc}")
            })
            val key = "tmp/${file.name}"
            try {
                S3Utils.putObject(key, file)
                val url = S3Utils.presignedUrl(key)
                if (file.length() > 1024 * 1024 * 50) {
                    val remotePath = message.action.downloadFile(url).file
                    message.action.uploadGroupFile(message.groupId, remotePath, file.name)
                } else {
                    message.sendMessage(messageChain {
                        video(url)
                    })
                }
            } finally {
                file.deleteOnExit()
                message.action.revokeMessage(replyId!!)
            }
        }
    }
}

@Single
class Ip: BaseCommand() {
    override val commandNames = listOf("ip")

    override suspend fun executeGroup(
        message: GroupMessage,
        args: List<String>
    ) {
        message.reply(ToolLogic.meiTuanIp(args[0]))
    }
}

@Single
class Dy: BaseCommand() {
    override val commandNames = listOf("dy")

    override suspend fun executeGroup(
        message: GroupMessage,
        args: List<String>
    ) {
        sendVideoMutex.withLock {
            val messageReceipt = message.reply("发送视频中，请稍等...")
            val file = ToolLogic.dy(args.joinToString(" "))
            try {
                val key = "tmp/${file.name}"
                S3Utils.putObject(key, file)
                val url = S3Utils.presignedUrl(key)
                if (file.length() > 1024 * 1024 * 50) {
                    val remotePath = message.action.downloadFile(url).file
                    message.action.uploadGroupFile(message.groupId, remotePath, file.name)
                } else {
                    message.sendMessage(messageChain {
                        video(url)
                    })
                }
            } finally {
                message.action.revokeMessage(messageReceipt!!)
                file.deleteOnExit()
            }
        }
    }
}

@Single
class Epic: BaseCommand() {
    override val commandNames = listOf("epic")

    override suspend fun executeGroup(
        message: GroupMessage,
        args: List<String>
    ) {
        val epic = EpicLogic.epic()
        for (game in epic) {
            message.reply(messageChain {
                image(game.imageUrl.toResource())
                text(game.text())
            })
        }
    }
}

@Single
class Rate: BaseCommand() {
    override val commandNames = listOf("rate")

    override suspend fun executeGroup(
        message: GroupMessage,
        args: List<String>
    ) {
        val one = args[0]
        val two = args[1]
        val three = args[2]
        val from = one.takeLast(3).uppercase()
        val to = three.takeLast(3).uppercase()
        if (two != "to") error("format error")
        val amt = one.dropLast(3).toDoubleOrNull() ?: error("format error")
        val rate = ToolLogic.rateVisa(from, to, amt)
        val text = """
                1${rate.transCurr} = ${rate.conversionRate}${rate.crdhldBillCurr}
                
                ${rate.transAmt}${rate.transCurr} = ${rate.crdhldBillAmt}${rate.crdhldBillCurr}
            """.trimIndent()
        message.reply(text)
    }
}

@Single
class My: BaseCommand() {
    override val commandNames = listOf("my")

    override suspend fun executeGroup(
        message: GroupMessage,
        args: List<String>
    ) {
        val fishing = ToolLogic.fishing()
        message.reply(messageChain {
            image(fishing.toResource())
        })
    }
}

@Single
class World: BaseCommand() {
    override val commandNames = listOf("world")

    override suspend fun executeGroup(
        message: GroupMessage,
        args: List<String>
    ) {
        val world = ToolLogic.watchWorld()
        message.reply(messageChain {
            image(world.toResource())
        })
    }
}

@Single
class Bin: BaseCommand() {
    override val commandNames = listOf("bin")

    override suspend fun executeGroup(
        message: GroupMessage,
        args: List<String>
    ) {
        val bin = args[0]
        val resultBin = ToolLogic.bin(bin)
        val text = """
                    🔢卡头：${resultBin.number}
                    💳品牌：${resultBin.scheme}
                    🔖类型：${resultBin.type}
                    💹等级：${resultBin.level}
                    
                    🗺地区：${resultBin.region}
                    💸货币：${resultBin.currency}
                    🏦银行：${resultBin.bank}
                """.trimIndent()
        message.reply(text)
    }
}

@Single
class JmComic: BaseCommand() {
    override val commandNames = listOf("jmcomic", "jm")

    private val mutex = Mutex()
    private val cache = mutableMapOf<Long, String>()

    override suspend fun executeGroup(message: GroupMessage, args: List<String>) {
        val id = args[0]
        try {
            mutex.withLock(id) {
                val messageId = message.reply("正在构建消息，请等待")
                val groupId = message.groupId
                val path = JmComicLogic.pdf("p$id")
                val action = message.action
                val url = S3Utils.presignedUrl("tmp/jmcomic/$id.pdf", path)
                val file = action.callApi("download_file", mapOf("url" to url)).toJsonNode()["data"]["file"].asText()
                val folderName = "jmcomic"
                var folderId = cache[groupId]
                if (folderId == null) {
                    var folder = action.getGroupRootFiles(groupId).folders.find { it.folderName == folderName }
                    if (folder == null) {
                        action.createGroupFileFolder(groupId, folderName)
                    }
                    folder = action.getGroupRootFiles(groupId).folders.find { it.folderName == folderName }
                    if (folder == null) error("无法创建群文件夹 jmcomic")
                    folderId = folder.folderId
                    cache[groupId] = folderId
                }
                action.uploadGroupFile(groupId, file, "$id.pdf", folderId)
                action.revokeMessage(messageId!!)
            }
        } catch (e : IllegalStateException) {
            e.message?.let {
                message.reply(it)
            }
        }
    }

}

@Single
class Bgp: BaseCommand() {
    override val commandNames = listOf("bgp")

    override suspend fun executeGroup(
        message: GroupMessage,
        args: List<String>
    ) {
        val keyword = args[0]
        val file = ToolLogic.bgp(keyword)
        message.reply(messageChain {
            image(file.readBytes().toResource())
        })
        file.deleteOnExit()
    }
}

@Single
class Whois: BaseCommand() {
    override val commandNames = listOf("whois", "whoisf")

    override suspend fun executeGroup(
        message: GroupMessage,
        args: List<String>
    ) {
        val domain = args[0]
        kotlin.runCatching {
            val whois = ToolLogic.whois(domain, message.text.startsWith("whoisf"))
            message.reply("""
                域名：${whois.domainName}
                注册人：${whois.registrant}
                注册邮箱：${whois.registrantEmail}
                注册机构：${whois.registrar}
                状态：${whois.status}
                NS：${whois.nameserver}
                创建日期：${whois.creationDate}
                过期日期：${whois.expiryDate}
            """.trimIndent())
        }.onFailure {
            it.message?.let { m ->
                message.reply(m)
            }
        }
    }
}

@Single
class FacePackage: BaseCommand() {
    override val commandNames = listOf("表情")

    override suspend fun executeGroup(
        message: GroupMessage,
        args: List<String>
    ) {
        val messageList = message.message.filter { it.type == SegmentType.image }
        val str = messageList.joinToString("\n") { it.data.url.toString() }
        message.reply(str)
    }

    override suspend fun executePrivate(
        message: PrivateMessage,
        args: List<String>
    ) {
        val messageList = message.message.filter { it.type == SegmentType.image }
        val str = messageList.joinToString("\n") { it.data.url.toString() }
        message.reply(str)
    }
}

@Single
class Music: BaseCommand() {
    override val commandNames = listOf("点歌", "music")

    @OptIn(ExperimentalROneBotApi::class)
    override suspend fun executeGroup(
        message: GroupMessage,
        args: List<String>
    ) {
        val name = args[0]
        val songList = MusicLogic.search(name).subList(0, 10)
        val list = mutableListOf<String>()
        for ((index, song) in songList.withIndex()) {
            list.add("${index + 1}、${song.name}")
        }
        message.reply("发送序号：\n${list.joinToString("\n")}")
        startGroupSession(message) {
            val index = it.message.text.toIntOrNull()
            if (index == null || index !in 1..songList.size) {
                it.reject(messageChain {
                    text("请输入正确的序号")
                })
            } else {
                val song = songList[index - 1]
                message.sendMessage(messageChain {
                    record {
                        file = song.url
                    }
                })
                it.accept()
            }
        }
    }
}