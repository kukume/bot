package me.kuku.qqbot.entity

import me.kuku.common.utils.Jackson
import me.kuku.qqbot.event.Message
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.json.json

object MessageTable: IntIdTable("message") {
    val messageId = varchar("message_id", 255)
    val groupId = varchar("group_id", 255).default("")
    val authorId = varchar("author_id", 255)
    val body = json("body", { Jackson.writeValueAsString(it) }, { Jackson.readValue<Message>(it) })
}

class MessageEntity(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<MessageEntity>(MessageTable)
    var messageId by MessageTable.messageId
    var groupId by MessageTable.groupId
    var authorId by MessageTable.authorId
    var body by MessageTable.body
}
