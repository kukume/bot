package me.kuku.qqbot.context

import me.kuku.qqbot.event.Message

private typealias MessageBody = suspend Message.() -> Unit

class MessageContext {

    val map = mutableMapOf<String, MessageBody>()

    fun handler(command: String, message: MessageBody) {
        map[command] = message
    }

}