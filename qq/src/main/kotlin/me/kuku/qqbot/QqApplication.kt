package me.kuku.qqbot

import io.github.kloping.qqbot.Starter
import io.github.kloping.qqbot.api.v2.FriendMessageEvent
import io.github.kloping.qqbot.api.v2.GroupMessageEvent
import io.github.kloping.qqbot.api.v2.MessageV2Event
import io.github.kloping.qqbot.impl.ListenerHost
import kotlinx.coroutines.runBlocking
import me.kuku.qqbot.config.BaseCommand
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module
import org.koin.plugin.module.dsl.startKoin

fun main(args: Array<String>) {
    val qqId = System.getenv("QQ_ID")
    val qqSecret = System.getenv("QQ_SECRET")
    val token = System.getenv("QQ_TOKEN") ?: ""
    val port = System.getenv("PORT")?.toInt()

    val starter = Starter(qqId, token, qqSecret)
    starter.config.webhookport = port ?: 18080
    starter.config.webhookpath = "/webhook"
    starter.run()

    val koinApplication = startKoin<MyApp> {
        printLogger()
    }

    val koin = koinApplication.koin

    val baseCommands = koin.getAll<BaseCommand>()

    starter.registerListenerHost(object : ListenerHost() {

        @EventReceiver
        fun onMessage(event: GroupMessageEvent) {
            baseCommands.forEach {
                val args = event.check(it) ?: return@forEach
                runBlocking { it.executeGroup(event, args) }
            }
        }

        @EventReceiver
        fun onMessage(event: FriendMessageEvent) {
            baseCommands.forEach {
                val args = event.check(it) ?: return@forEach
                runBlocking { it.executePrivate(event, args) }
            }
        }

        @EventReceiver
        fun onMessage(event: MessageV2Event) {
            baseCommands.forEach {
                val args = event.check(it) ?: return@forEach
                runBlocking { it.executeAll(event, args) }
            }
        }

    })

}


@Module
@ComponentScan
class AppModule

@KoinApplication(modules = [AppModule::class])
class MyApp


private fun MessageV2Event. check(baseCommand: BaseCommand): List<String>? {
    val message = this.message.joinToString("")
    val atBot = "<@${bot.info.unionOpenid}>"
    val args = message.replace(atBot, "").trim().split(" ").toMutableList()
    val command = args.removeFirstOrNull()
    return if (baseCommand.commands.contains(command)) {
        args
    } else null
}
