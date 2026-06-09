package me.kuku.qqbot.command

import io.github.kloping.qqbot.api.v2.GroupMessageEvent
import io.github.kloping.qqbot.entities.ex.Image
import io.github.kloping.qqbot.entities.ex.Markdown
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import me.kuku.common.ktor.client
import me.kuku.common.logic.EpicLogic
import me.kuku.common.logic.ToolLogic
import me.kuku.qqbot.config.BaseCommand
import org.koin.core.annotation.Single

@Single
class Epic: BaseCommand("/epic") {
    override suspend fun executeGroup(
        event: GroupMessageEvent,
        args: List<String>
    ) {
        val epics = EpicLogic.epic()
        epics.forEach {
            val text = it.text()
            event.sendMessage(Markdown().setContent("![text #208px #320px](${it.imageUrl})\n$text"))
        }
    }
}

@Single
class Rate: BaseCommand("/rate") {
    override suspend fun executeGroup(
        event: GroupMessageEvent,
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
        event.sendMessage(text)
    }
}

@Single
class Bin: BaseCommand("/bin") {
    override suspend fun executeGroup(
        event: GroupMessageEvent,
        args: List<String>
    ) {
        val resultBin = ToolLogic.bin(args[0])
        val text = """
            🔢卡头：${resultBin.number}
            💳品牌：${resultBin.scheme}
            🔖类型：${resultBin.type}
            💹等级：${resultBin.level}
            
            🗺地区：${resultBin.region}
            💸货币：${resultBin.currency}
            🏦银行：${resultBin.bank}
        """.trimIndent()
        event.sendMessage(text)
    }
}

@Single
class My: BaseCommand("/my") {
    override suspend fun executeGroup(
        event: GroupMessageEvent,
        args: List<String>
    ) {
        val fishing = ToolLogic.fishing()
        val bytes = client.get(fishing).bodyAsBytes()
        event.sendMessage(Image(bytes))
    }
}

@Single
class World: BaseCommand("/world") {
    override suspend fun executeGroup(
        event: GroupMessageEvent,
        args: List<String>
    ) {
        val imageUrl = ToolLogic.watchWorld()
        event.sendMessage(Image(imageUrl))
    }
}