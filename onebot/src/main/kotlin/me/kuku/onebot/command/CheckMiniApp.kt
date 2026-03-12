package me.kuku.onebot.command

import cn.rtast.rob.BotInstance
import cn.rtast.rob.enums.SegmentType
import cn.rtast.rob.event.onEvent
import cn.rtast.rob.event.packed.GroupMessageEvent
import me.kuku.common.utils.toJsonNode
import me.kuku.onebot.config.ROneBot
import org.koin.core.annotation.Single

@Single
class CheckMiniApp: ROneBot {

    override fun BotInstance.execute() {
        onEvent<GroupMessageEvent> { event ->
            val jsonNode = event.message.message.find { it.type == SegmentType.json }?.data?.data?.toJsonNode() ?: return@onEvent
            val url = jsonNode["meta"]?.get("detail_1")?.get("qqdocurl")?.asText() ?: return@onEvent
            val list = listOf("""
                网页明明能直达，偏偏甩你小程序。
                点开广告一大把，关都关不掉俩俩。
                界面模糊乱七八，跳来跳去像傻瓜。
                装作分享讲精致，其实懒癌犯大发。
                劝君别做麻烦鬼，链接复制才优雅。
            """.trimIndent(), """
                小程遮正道，乱点似迷宫。
                网页明明在，何须绕重重？
            """.trimIndent(), """
                人间本有好链接，却爱小程惹人嫌。
                一跳三层开广告，半屏乱码丧心田。
                转来转去无归处，点了才知被卖钱。
                劝君莫做麻烦客，清清爽爽最堪怜。
            """.trimIndent())
            event.reply("${list.random()}\n$url")
        }
    }
}
