package me.kuku.telegram.handler

import io.github.dehuckakpyt.telegrambot.factory.keyboard.inlineKeyboard
import io.github.dehuckakpyt.telegrambot.handler.BotHandler
import io.github.dehuckakpyt.telegrambot.model.telegram.InlineKeyboardButton
import io.github.dehuckakpyt.telegrambot.model.telegram.input.ByteArrayContent
import io.ktor.client.request.*
import io.ktor.client.statement.*
import me.kuku.common.ktor.client
import me.kuku.common.logic.YgoLogic
import org.koin.core.annotation.Factory

@Factory
class YgoHandler: BotHandler({

    command("/ygo") {
        val cardList = YgoLogic.search(commandArgument ?: error("not args"))
        val list = mutableListOf<InlineKeyboardButton>()
        for (i in cardList.indices) {
            val card = cardList[i]
            list.add(callbackButton(card.chineseName, "ygoCard", card.cardPassword))
        }
        sendMessage("请选择查询的卡片", replyMarkup = inlineKeyboard(*list.toTypedArray()))
    }

    callback("ygoCard") {
        answerCallbackQuery(query.id, "获取成功")
        val id = transferred<String>()
        val card = YgoLogic.searchDetail(id.toLong())
        sendPhoto(ByteArrayContent(client.get(card.imageUrl).bodyAsBytes()),
            caption = "中文名：${card.chineseName}\n日文名：${card.japaneseName}\n英文名：${card.englishName}\n效果：\n${card.effect}\n链接：${card.url}")
    }

})