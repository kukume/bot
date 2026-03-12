package me.kuku.qqbot.subscribe

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.config.*
import io.ktor.util.*
import me.kuku.common.ktor.client
import me.kuku.qqbot.context.Subscribe
import me.kuku.common.logic.BiliBiliLogic
import me.kuku.common.logic.EpicLogic
import me.kuku.common.logic.ToolLogic
import org.koin.core.component.inject
import org.koin.core.qualifier.named

fun Subscribe.tool() {

    val rapidConfig by inject<ApplicationConfig>(named("rapidConfig"))

    group {
        handler("bv") {
            val sendMessage = sendMessage("获取哔哩哔哩视频时间很长，请耐心等待", msgSeq = 1)
            val file = BiliBiliLogic.videoByBvId(arg(1)).file
            try {
                sendVideo(fileData = file.readBytes().encodeBase64(), msgSeq = 2)
            } finally {
                file.delete()
            }
            sendMessage.delete()
        }

        handler("st") {
            val jsonNode = client.get("https://api.kukuqaq.com/lolicon/random").body<JsonNode>()
            val quickUrl = jsonNode[0]["quickUrl"].asText()
            sendImage(quickUrl)
        }

        handler("epic") {
            val sendMessage = sendMessage("获取epic免费游戏信息时间很长，请耐心等待", msgSeq = 1)
            val epics = EpicLogic.epic()
            var seq = 2
            for (epic in epics) {
                val content = epic.text().replace(".", "点")
                try {
                    sendImage(fileData = client.get(epic.imageUrl).bodyAsBytes().encodeBase64(), msgSeq = seq, url = null,
                        content = content)
                } catch (e: Exception) {
                    sendMessage(content, msgSeq = seq)
                }
                seq++
            }
            sendMessage.delete()
        }

        handler("dy") {
            val urlArg = arg(1)
            val sendMessage = sendMessage("获取抖音视频时间很长，请耐心等待", msgSeq = 1)
            val file = ToolLogic.dy(urlArg)
            sendVideo(fileData = file.readBytes().encodeBase64(), msgSeq = 2)
            sendMessage.delete()
        }

        handler("rate") {
            val one = arg(1)
            val two = arg(2)
            val three = arg(3)
            val from = one.takeLast(3).uppercase()
            val to = three.takeLast(3).uppercase()
            if (two != "to") error("format error")
            val amt = one.dropLast(3).toDoubleOrNull() ?: error("format error")
            val rate = ToolLogic.rate(from, to, amt)
            val text = """
                1${rate.transCurr} = ${rate.conversionRate}${rate.crdhldBillCurr}
                
                ${rate.transAmt}${rate.transCurr} = ${rate.crdhldBillAmt}${rate.crdhldBillCurr}
            """.trimIndent()
            sendMessage("\n" + text)
        }

        handler("bin") {
            val bin = arg(1)
            val key = rapidConfig.property("key").getString()
            val resultBin = ToolLogic.bin(bin, key)
            val text = """
                    🔢卡头：${resultBin.number}
                    💳品牌：${resultBin.scheme}
                    🔖类型：${resultBin.type}
                    💹等级：${resultBin.level}
                    
                    🗺地区：${resultBin.region}
                    💸货币：${resultBin.currency}
                    🏦银行：${resultBin.bank}
                """.trimIndent()
            sendMessage("\n" + text)
        }

        handler("my") {
            val fishing = ToolLogic.fishing()
            val encodeBase64 = client.get(fishing).bodyAsBytes().encodeBase64()
            sendImage(fileData = encodeBase64)
        }

        handler("world") {
            val imageUrl = ToolLogic.watchWorld()
            sendImage(imageUrl)
        }

        handler("ip") {
            val ip = arg(1)
            sendMessage(ToolLogic.meiTuanIp(ip))
        }
    }

}
