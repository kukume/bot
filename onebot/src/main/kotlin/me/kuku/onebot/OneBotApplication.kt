package me.kuku.onebot

import cn.rtast.klogging.LogLevel
import cn.rtast.rob.OneBotFactory
import cn.rtast.rob.command.BaseCommand
import cn.rtast.rob.event.onEvent
import cn.rtast.rob.event.packed.GroupMessageErrorEvent
import me.kuku.onebot.config.ROneBot
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module
import org.koin.dsl.module
import org.koin.plugin.module.dsl.startKoin

suspend fun main() {
    val address = System.getenv("BOT_ADDRESS")
    val accessToken = System.getenv("BOT_ACCESS_TOKEN")
    val botInstance = OneBotFactory.createClient(address, accessToken, logLevel = LogLevel.DEBUG)
    botInstance.onEvent<GroupMessageErrorEvent> {
        it.exception.printStackTrace()
        it.message.reply("exception，异常原因：${it.exception.message}")
    }
    val koinApplication = startKoin<MyApp> {
        printLogger()
        module {
            single { botInstance }
        }

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

@Module
@ComponentScan
class AppModule

@KoinApplication(modules = [AppModule::class])
class MyApp