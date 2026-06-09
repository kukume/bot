package me.kuku.qqbot.command

import io.github.kloping.qqbot.api.v2.GroupMessageEvent
import io.github.kloping.qqbot.entities.ex.Keyboard
import io.github.kloping.qqbot.entities.ex.Markdown
import me.kuku.common.logic.YgoLogic
import me.kuku.qqbot.config.BaseCommand
import org.koin.core.annotation.Single

@Single
class YgoCommand: BaseCommand("/ygo") {

    override suspend fun executeGroup(
        event: GroupMessageEvent,
        args: List<String>
    ) {
        val cardList = YgoLogic.search(args[0]).take(5)
        val keyboardBuilder = Keyboard.KeyboardBuilder.create()
        cardList.forEach {
            val rowBuilder = keyboardBuilder.addRow()
            rowBuilder.addButton().setLabel(it.chineseName).setActionData("/ygoCardDetail ${it.cardPassword}").build()
            rowBuilder.build()
        }
        event.sendMessage(Markdown(null).setContent("请选择查询的卡片").setKeyboard(keyboardBuilder.build()))
    }
}


@Single
class YgoCardDetailCommand: BaseCommand("/ygoCardDetail") {
    override suspend fun executeGroup(
        event: GroupMessageEvent,
        args: List<String>
    ) {
        val card = YgoLogic.searchDetail(args[0].toLong())
        event.sendMessage(Markdown()
            .setContent("![text #208px #320px](${card.imageUrl})\n中文名：${card.chineseName}\n日文名：${card.japaneseName}\n英文名：${card.englishName}\n效果：\n${card.effect}\n链接：${card.url}"))
    }
}