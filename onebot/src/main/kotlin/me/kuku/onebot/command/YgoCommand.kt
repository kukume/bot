package me.kuku.onebot.command

import cn.rtast.rob.annotations.ExperimentalROneBotApi
import cn.rtast.rob.command.BaseCommand
import cn.rtast.rob.entity.toResource
import cn.rtast.rob.event.raw.message.GroupMessage
import cn.rtast.rob.event.raw.message.text
import cn.rtast.rob.onebot.dsl.image
import cn.rtast.rob.onebot.dsl.messageChain
import cn.rtast.rob.onebot.dsl.text
import cn.rtast.rob.session.accept
import cn.rtast.rob.session.reject
import me.kuku.common.logic.YgoLogic
import org.koin.core.annotation.Single

@Single
class YgoCommand: BaseCommand() {

    override val commandNames = listOf("ygo")

    @OptIn(ExperimentalROneBotApi::class)
    override suspend fun executeGroup(
        message: GroupMessage,
        args: List<String>
    ) {
        val name = args[0]
        val cardList = YgoLogic.search(name)
        val list = mutableListOf<String>()
        for ((index, card) in cardList.withIndex()) {
            list.add("${index + 1}、${card.chineseName}")
        }
        message.reply("请发送你需要查询的卡片，\n发送序号：\n${list.joinToString("\n")}")
        startGroupSession(message) {
            val index = it.message.text.toIntOrNull()
            if (index == null || index !in 1..cardList.size) {
                it.reject(messageChain {
                    text("请输入正确的序号")
                })
            } else {
                val card = cardList[index - 1]
                it.accept(messageChain {
                    image(card.imageUrl.toResource())
                    text("\n中文名：${card.chineseName}\n日文名：${card.japaneseName}\n英文名：${card.englishName}\n效果：\n${card.effect}\n链接：${card.url}")
                })
            }
        }
    }
}
