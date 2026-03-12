package me.kuku.qqbot.context

import me.kuku.qqbot.event.GroupMessage

private typealias GroupMessageBody = suspend GroupMessage.() -> Unit

class GroupMessageContext {

    val map = mutableMapOf<String, GroupMessageBody>()

    fun handler(command: String, message: GroupMessageBody) {
        map[command] = message
    }


}