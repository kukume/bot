package me.kuku.qqbot.config

import io.github.kloping.qqbot.api.v2.FriendMessageEvent
import io.github.kloping.qqbot.api.v2.GroupMessageEvent
import io.github.kloping.qqbot.api.v2.MessageV2Event
import io.github.kloping.qqbot.entities.ex.FileMsg

abstract class BaseCommand(val commands: List<String>) {

    constructor(command: String) : this(listOf(command))

    open suspend fun executeGroup(event: GroupMessageEvent, args: List<String>) {}

    open suspend fun executePrivate(event: FriendMessageEvent, args: List<String>) {}

    open suspend fun executeAll(event: MessageV2Event, args: List<String>) {}

}

class File : FileMsg {
    constructor(url: String, name: String) : super(url, FileType.FILE) {
        super.name = name
    }

    constructor(bytes: ByteArray, name: String) : super(bytes, FileType.FILE) {
        super.name = name
    }
}