package me.kuku.telegram.handler

import io.github.dehuckakpyt.telegrambot.ext.container.chatId
import io.github.dehuckakpyt.telegrambot.ext.container.fromId
import io.github.dehuckakpyt.telegrambot.factory.keyboard.button.ButtonFactory
import io.github.dehuckakpyt.telegrambot.factory.keyboard.inlineKeyboard
import io.github.dehuckakpyt.telegrambot.handler.BotHandler
import io.github.dehuckakpyt.telegrambot.model.telegram.InlineKeyboardButton
import io.github.dehuckakpyt.telegrambot.model.telegram.InlineKeyboardMarkup
import me.kuku.common.entity.Status
import me.kuku.common.entity.UserConfigEntity
import me.kuku.common.entity.UserConfigTable
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.update
import org.koin.core.annotation.Factory
import org.koin.mp.KoinPlatform.getKoin

@Factory
class UserConfigHandler: BotHandler({

    val buttonFactory = getKoin().get<ButtonFactory>()

    val configList = listOf(
        UserConfigButton("epic免费游戏推送", UserConfigTable.epicFreeGamePush),
        UserConfigButton("线报酷推送", UserConfigTable.xianBaoPush)
    )

    suspend fun markup(fromId: Long, chatId: Long): InlineKeyboardMarkup {
        val row = suspendTransaction {
            val entity = UserConfigEntity.find { UserConfigTable.identityId eq fromId.toString() }.firstOrNull()
                ?: UserConfigEntity.new { identityId = fromId.toString() }
            entity.readValues
        }
        val buttons = mutableListOf<InlineKeyboardButton>()
        for (config in configList) {
            val text = config.text
            val status = row[config.column]
            buttons.add(buttonFactory.callbackButton(chatId, fromId,
                "$text $status", "userConfigChange", UserConfigTransfer(text, status)))
        }
        return inlineKeyboard(*buttons.toTypedArray())
    }

    command("/userconfig") {
        sendMessage("一些配置，点击按钮可以切换", replyMarkup = markup(fromId, chatId))
    }

    callback("userConfigChange") {
        val transfer = transferred<UserConfigTransfer>()
        val text = transfer.text
        val status = transfer.status
        val config = configList.find { it.text == text }!!
        suspendTransaction {
            UserConfigTable.update({ UserConfigTable.identityId eq fromId.toString() }) {
                it[config.column] = !status
            }
        }
        editMessageText(message.messageId, "一些配置，点击按钮可以切换", replyMarkup = markup(fromId, chatId))
    }


})

private data class UserConfigButton(
    val text: String,
    val column: Column<Status>
)

private data class UserConfigTransfer(
    val text: String,
    val status: Status
)
