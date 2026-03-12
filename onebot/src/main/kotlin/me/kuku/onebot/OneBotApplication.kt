package me.kuku.onebot

import cn.rtast.klogging.LogLevel
import cn.rtast.rob.OneBotFactory
import cn.rtast.rob.command.BaseCommand
import cn.rtast.rob.event.onEvent
import cn.rtast.rob.event.packed.GroupMessageErrorEvent
import me.kuku.onebot.config.ROneBot
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.ksp.generated.defaultModule

suspend fun main() {
    val address = System.getenv("BOT_ADDRESS")
    val accessToken = System.getenv("BOT_ACCESS_TOKEN")
    val botInstance = OneBotFactory.createClient(address, accessToken, logLevel = LogLevel.DEBUG)
    botInstance.onEvent<GroupMessageErrorEvent> {
        it.exception.printStackTrace()
        it.message.reply("exception，异常原因：${it.exception.message}")
    }
    val koinApplication = startKoin {
        printLogger()
        module {
            single { botInstance }
        }
        modules(
            defaultModule
        )
    }
    val instances = koinApplication.koin.getAll<ROneBot>()
    instances.forEach { instance ->
        with(instance) {
            botInstance.execute()
        }
    }
    val command = koinApplication.koin.getAll<BaseCommand>()
    command.forEach { command ->
        OneBotFactory.commandManager.register(command)
    }

}
