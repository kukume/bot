package me.kuku.telegram.handler

import io.github.dehuckakpyt.telegrambot.handler.BotHandler
import org.koin.core.annotation.Factory

@Factory
class StartHandler : BotHandler({
    command("/start") {
        sendMessage("Hello, my name is ${bot.username} :-)")
    }

    command("/help") {
        sendMessage("""
            /sign
            /signlog
            /userconfig
            /ygo
        """.trimIndent())
    }

})