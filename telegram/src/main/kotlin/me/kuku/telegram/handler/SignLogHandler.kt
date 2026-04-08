package me.kuku.telegram.handler

import io.github.dehuckakpyt.telegrambot.TelegramBot
import io.github.dehuckakpyt.telegrambot.ext.container.chatId
import io.github.dehuckakpyt.telegrambot.ext.container.fromId
import io.github.dehuckakpyt.telegrambot.factory.keyboard.button.ButtonFactory
import io.github.dehuckakpyt.telegrambot.factory.keyboard.inlineKeyboard
import io.github.dehuckakpyt.telegrambot.handler.BotHandler
import io.github.dehuckakpyt.telegrambot.model.telegram.InlineKeyboardButton
import io.github.dehuckakpyt.telegrambot.model.telegram.InlineKeyboardMarkup
import me.kuku.common.entity.IdentityEntity
import me.kuku.common.entity.IdentityTable
import me.kuku.common.entity.SignLogEntity
import me.kuku.common.entity.SignLogTable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.koin.core.annotation.Factory
import org.koin.java.KoinJavaComponent.getKoin
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Factory
class SignLogHandler(
    private val buttonFactory: ButtonFactory
): BotHandler({

    data class SignTransfer(val date: LocalDate, val id: Int)

    data class DateAndIdentityName(val date: LocalDate?, val identityName: String?, val redirect: Boolean = false)

    suspend fun replyMarkup(now: LocalDate, chatId: Long, fromId: Long, identityName: String = "", returnButton: Boolean = false): InlineKeyboardMarkup {
        val before = now.atTime(0, 0)
        val after = now.atTime(23, 59)
        val queryList = suspendTransaction {
            SignLogEntity.find(
                SignLogTable.logTime greaterEq before and (SignLogTable.logTime lessEq after)
                and (SignLogTable.identityId eq fromId.toString()) and (SignLogTable.identityName eq identityName)
            ).toList()
        }
        val list = mutableListOf<List<InlineKeyboardButton>>()
        for (signLogEntity in queryList) {
            val button = buttonFactory.callbackButton(chatId, fromId,
                "${signLogEntity.type.value} - ${if (signLogEntity.success) "成功" else "失败"}", "signLogDetail", SignTransfer(now, signLogEntity.id.value))
            list.add(listOf(button))
        }
        list.add(listOf(
            buttonFactory.callbackButton(chatId, fromId, "前一天", "signLog", DateAndIdentityName(now.minusDays(1), identityName)),
            buttonFactory.callbackButton(chatId, fromId, "后一天", "signLog", DateAndIdentityName(now.plusDays(1), identityName))
        ))
        if (returnButton) {
            list.add(listOf(buttonFactory.callbackButton(chatId, fromId,
                "返回", "signLog", DateAndIdentityName(null, null, true))))
        }
        return InlineKeyboardMarkup(list)
    }

    suspend fun signLog(now: LocalDate, chatId: Long, fromId: Long, bot: TelegramBot, messageId: Long? = null, identityName: String = "", needRedirect: Boolean = true) {
        val queryList = suspendTransaction {
            IdentityEntity.find { IdentityTable.identityId eq fromId.toString() }.toList()
        }
        if (needRedirect) {
            if (queryList.isNotEmpty()) {
                val buttonList = mutableListOf<InlineKeyboardButton>()
                buttonList.add(buttonFactory.callbackButton("默认", "signLogSelect"))
                for (identityEntity in queryList) {
                    buttonList.add(buttonFactory.callbackButton(chatId, fromId,
                        identityEntity.identityName, "signLogSelect", identityEntity.id.value))
                }
                val text = "请选择身份"
                if (messageId == null) {
                    bot.sendMessage(chatId, text, replyMarkup = inlineKeyboard(*buttonList.toTypedArray()))
                } else {
                    bot.editMessageText(chatId, messageId, text,
                        replyMarkup =  inlineKeyboard(*buttonList.toTypedArray()))
                }
                return
            }
        }
        val returnButton = queryList.isNotEmpty()
        val text = "${now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))} 签到日志，点击可查看详情"
        if (messageId == null) {
            bot.sendMessage(
                chatId,
                text,
                replyMarkup = replyMarkup(now, chatId, fromId, identityName, returnButton)
            )
        } else {
            bot.editMessageText(chatId, messageId, text,
                replyMarkup = replyMarkup(now, chatId, fromId, identityName, returnButton))
        }
    }

    callback("signLogSelect") {
        val id = transferredOrNull<Int>()
        val identityName = if (id != null) {
            suspendTransaction {
                IdentityEntity.findById(id)!!.identityName
            }
        } else ""
        signLog(LocalDate.now(), chatId, fromId, bot, message.messageId, identityName, false)
    }

    command("/signlog") {
        val now = LocalDate.now()
        signLog(now, chatId, fromId, bot)
    }

    callback("signLog") {
        val any = transferredOrNull<DateAndIdentityName>()
        val now = any?.date ?: LocalDate.now()
        signLog(now, chatId, fromId, bot, message.messageId, any?.identityName ?: "", any?.redirect ?: false)
    }

    callback("signLogDetail") {
        val transfer = transferred<SignTransfer>()
        val id = transfer.id
        val key = transfer.date
        val entity = suspendTransaction {
            SignLogEntity.findById(id)!!
        }
        if (entity.success) {
            if (entity.remark != null) {
                answerCallbackQuery(query.id, entity.remark, true)
            } else {
                answerCallbackQuery(query.id, "没有详细信息", true)
            }
        } else {
            editMessageText(message.messageId, entity.exceptionStack.toString(),
                replyMarkup = inlineKeyboard(callbackButton("返回", "signLog", DateAndIdentityName(key, entity.identityName)))
            )
        }
    }

})
