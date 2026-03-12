package me.kuku.qqbot.context

import me.kuku.qqbot.event.PrivateMessage


private typealias PrivateMessageBody = suspend PrivateMessage.() -> Unit

class PrivateMessageContext {

    val map = mutableMapOf<String, PrivateMessageBody>()

    fun handler(command: String, message: PrivateMessageBody) {
        map[command] = message
    }


}