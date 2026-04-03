package me.kuku.telegram.handler

import io.github.dehuckakpyt.telegrambot.ext.container.chatId
import io.github.dehuckakpyt.telegrambot.ext.container.fromId
import io.github.dehuckakpyt.telegrambot.factory.keyboard.button.ButtonFactory
import io.github.dehuckakpyt.telegrambot.factory.keyboard.inlineKeyboard
import io.github.dehuckakpyt.telegrambot.handler.BotHandler
import io.github.dehuckakpyt.telegrambot.model.telegram.InlineKeyboardMarkup
import io.github.dehuckakpyt.telegrambot.TelegramBot
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.koin.core.annotation.Factory
import org.koin.ktor.ext.getKoin
import org.koin.mp.KoinPlatform.getKoin
import java.util.UUID

object MessageTokenTable : IntIdTable("message_token") {
    val userId = long("user_id").uniqueIndex()
    val chatId = long("chat_id")
    val token = varchar("token", 64).uniqueIndex()
}

class MessageTokenEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<MessageTokenEntity>(MessageTokenTable)

    var userId by MessageTokenTable.userId
    var chatId by MessageTokenTable.chatId
    var token by MessageTokenTable.token
}

data class MessageTokenRequest(
    val token: String,
    val text: String
)

data class MessageTokenResponse(
    val success: Boolean
)

private data class MessageTokenTransfer(
    val token: String
)

private fun newMessageToken(): String = UUID.randomUUID().toString().replace("-", "")

private suspend fun ensureMessageToken(userId: Long, chatId: Long): MessageTokenEntity = suspendTransaction {
    MessageTokenEntity.find { MessageTokenTable.userId eq userId }.firstOrNull()?.also {
        if (it.chatId != chatId) {
            it.chatId = chatId
        }
    } ?: MessageTokenEntity.new {
        this.userId = userId
        this.chatId = chatId
        this.token = newMessageToken()
    }
}

private suspend fun refreshMessageToken(userId: Long, chatId: Long): MessageTokenEntity = suspendTransaction {
    val entity = MessageTokenEntity.find { MessageTokenTable.userId eq userId }.firstOrNull()
        ?: MessageTokenEntity.new {
            this.userId = userId
            this.chatId = chatId
            this.token = newMessageToken()
        }
    entity.chatId = chatId
    entity.token = newMessageToken()
    entity
}

private suspend fun tokenMarkup(chatId: Long, userId: Long, token: String): InlineKeyboardMarkup {
    val buttonFactory = getKoin().get<ButtonFactory>()
    return inlineKeyboard(
        buttonFactory.callbackButton(chatId, userId, "刷新token", "refreshMessageToken", MessageTokenTransfer(token))
    )
}

@Factory
class MessageTokenHandler : BotHandler({

    command("/msgtoken") {
        val entity = ensureMessageToken(fromId, chatId)
        sendMessage(
            """
            当前消息token：
            ${entity.token}
            
            可通过接口按 token 给你发送消息。
            """.trimIndent(),
            replyMarkup = tokenMarkup(chatId, fromId, entity.token)
        )
    }

    callback("refreshMessageToken") {
        val entity = refreshMessageToken(fromId, chatId)
        editMessageText(
            message.messageId,
            """
            当前消息token：
            ${entity.token}
            
            已刷新，可通过接口按 token 给你发送消息。
            """.trimIndent(),
            replyMarkup = tokenMarkup(chatId, fromId, entity.token)
        )
    }
})

fun Application.messageTokenApi() {
    routing {
        post("/send-message") {
            val body = call.receive<MessageTokenRequest>()
            require(body.token.isNotBlank()) { "token is blank" }
            require(body.text.isNotBlank()) { "text is blank" }
            val entity = suspendTransaction {
                MessageTokenEntity.find { MessageTokenTable.token eq body.token }.firstOrNull()
            } ?: return@post call.respond(HttpStatusCode.NotFound, MessageTokenResponse(success = false))
            getKoin().get<TelegramBot>().sendMessage(entity.chatId, "#自定义消息提醒\n${body.text}")
            call.respond(HttpStatusCode.OK, MessageTokenResponse(success = true))
        }
    }
}
