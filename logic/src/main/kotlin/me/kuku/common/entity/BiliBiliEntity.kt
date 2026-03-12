package me.kuku.common.entity

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

object BiliBiliTable: IntIdTable("bili_bili") {
    val identityId = varchar("identity_id", 255)
    val identityName = varchar("identity_name", 255).default("")
    val cookie = varchar("cookie", 1000)
    val userid = varchar("user_id", 255)
    val token = varchar("token", 255)
    val push = enumeration<Status>("push").default(Status.OFF)
    val sign = enumeration<Status>("sign").default(Status.OFF)
    val live = enumeration<Status>("live").default(Status.OFF)

    init {
        index(true, identityId, identityName)
    }
}

class BiliBiliEntity(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<BiliBiliEntity>(BiliBiliTable)

    val identityId by BiliBiliTable.identityId
    val identityName by BiliBiliTable.identityName
    var cookie by BiliBiliTable.cookie
    var userid by BiliBiliTable.userid
    var token by BiliBiliTable.token
    var push by BiliBiliTable.push
    var sign by BiliBiliTable.sign
    var live by BiliBiliTable.live
}
