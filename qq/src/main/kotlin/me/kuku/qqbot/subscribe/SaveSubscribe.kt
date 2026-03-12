package me.kuku.qqbot.subscribe

import me.kuku.qqbot.context.Subscribe
import me.kuku.qqbot.entity.MessageTable
import me.kuku.qqbot.event.GroupMessage
import me.kuku.qqbot.event.PrivateMessage
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

fun Subscribe.save() {

    all {

        handler("") {
            suspendTransaction {
                MessageTable.insert {
                    it[messageId] = this@handler.id
                    it[body] = this@handler
                    if (this@handler is GroupMessage) {
                        it[groupId] = this@handler.groupId
                        it[authorId] = this@handler.author.memberOpenid
                    } else if (this@handler is PrivateMessage) {
                        it[authorId] = this@handler.author.userOpenid
                    }
                }
            }
        }

    }

}
