package me.kuku.onebot

import cn.rtast.klogging.LogLevel
import cn.rtast.rob.OneBotFactory
import cn.rtast.rob.command.BaseCommand
import cn.rtast.rob.event.onEvent
import cn.rtast.rob.event.packed.GroupMessageErrorEvent
import me.kuku.common.logic.OpenaiLogic
import me.kuku.common.mcp.McpClientManager
import me.kuku.common.mcp.McpConfigLoader
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
    initMcp()
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

private suspend fun initMcp() {
    val mcpManager = McpClientManager()

    val config = McpConfigLoader.load()
    if (config != null) {
        mcpManager.connectFromConfig(config)
    } else {
        var index = 0
        while (true) {
            val prefix = "MCP_SERVER_${index}_"
            val name = System.getenv("${prefix}NAME") ?: break
            val type = System.getenv("${prefix}TYPE") ?: "stdio"
            when (type.lowercase()) {
                "stdio" -> {
                    val command = System.getenv("${prefix}COMMAND") ?: continue
                    mcpManager.connectStdio(name, command.split(","))
                }
                "sse" -> {
                    val url = System.getenv("${prefix}URL") ?: continue
                    mcpManager.connectSse(name, url)
                }
            }
            index++
        }
    }

    if (mcpManager.isNotEmpty()) {
        OpenaiLogic.mcpManager = mcpManager
    }
}

@Module
@ComponentScan
class AppModule

@KoinApplication(modules = [AppModule::class])
class MyApp