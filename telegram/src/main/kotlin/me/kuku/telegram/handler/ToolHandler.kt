package me.kuku.telegram.handler

import io.github.dehuckakpyt.telegrambot.handler.BotHandler
import io.github.dehuckakpyt.telegrambot.model.telegram.input.NamedFileContent
import me.kuku.common.logic.BiliBiliLogic
import org.koin.core.annotation.Factory

@Factory
class ToolHandler: BotHandler({

    command("/bv") {
        val videoFile = BiliBiliLogic.videoByBvId(commandArgument ?: error("not args")).file
        try {
            sendVideo(NamedFileContent(videoFile))
        } finally {
            videoFile.delete()
        }
    }

})