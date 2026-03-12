package me.kuku.telegram.ktor

import io.github.dehuckakpyt.telegrambot.TelegramBot
import io.github.dehuckakpyt.telegrambot.exception.handler.ExceptionHandlerImpl
import io.github.dehuckakpyt.telegrambot.ext.config.client
import io.github.dehuckakpyt.telegrambot.ext.source.inDatabase
import io.github.dehuckakpyt.telegrambot.model.telegram.Chat
import io.github.dehuckakpyt.telegrambot.plugin.TelegramBot
import io.github.dehuckakpyt.telegrambot.source.callback.CallbackContentSource
import io.github.dehuckakpyt.telegrambot.source.chain.ChainSource
import io.github.dehuckakpyt.telegrambot.template.MessageTemplate
import io.github.dehuckakpyt.telegrambot.template.Templater
import io.ktor.client.plugins.*
import io.ktor.server.application.*

fun Application.telegramBot() {
    val config = environment.config.config("telegram-bot")
    install(TelegramBot) {
        receiving {
            callbackContentSource = {
                CallbackContentSource.inDatabase(
                    maxCallbackContentsPerUser = 20
                )
            }
            chainSource = { ChainSource.inDatabase }
            exceptionHandler = { OwnExceptionHandler(telegramBot, receiving.messageTemplate, templater) }
        }
        client {
            defaultRequest {
                config.propertyOrNull("api")?.getString()?.takeIf { it.isNotEmpty() }?.let {
                    url("$it/bot$token/")
                }
            }
        }
    }
}


class OwnExceptionHandler(bot: TelegramBot, template: MessageTemplate, templater: Templater) : ExceptionHandlerImpl(bot, template, templater) {

    override suspend fun caught(chat: Chat, ex: Throwable) {
        when (ex) {
            is IllegalStateException -> {
                bot.sendMessage(chat.id, ex.toString())
            }
            else -> super.caught(chat, ex)
        }
    }


}
