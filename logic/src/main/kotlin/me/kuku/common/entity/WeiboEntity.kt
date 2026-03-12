package me.kuku.common.entity

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

object WeiboTable: IntIdTable("weibo") {
    val identityId = varchar("identity_id", 255)
    val identityName = varchar("identity_name", 255).default("")
    val cookie = varchar("cookie", 600)
    val push = enumeration<Status>("push").default(Status.OFF)
    val sign = enumeration<Status>("sign").default(Status.OFF)

    init {
        index(true, identityId, identityName)
    }
}

class WeiboEntity(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<WeiboEntity>(WeiboTable)

    val identityId by WeiboTable.identityId
    val identityName by WeiboTable.identityName
    var cookie by WeiboTable.cookie
    var push by WeiboTable.push
    var sign by WeiboTable.sign
}
