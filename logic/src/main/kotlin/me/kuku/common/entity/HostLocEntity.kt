package me.kuku.common.entity

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

object HostLocTable: IntIdTable("host_loc") {
    val identityId = varchar("identity_id", 255)
    val identityName = varchar("identity_name", 255).default("")
    val cookie = varchar("cookie", 500)
    val sign = enumeration<Status>("sign").default(Status.OFF)

    init {
        index(true, identityId, identityName)
    }
}

class HostLocEntity(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<HostLocEntity>(HostLocTable)

    val identityId by HostLocTable.identityId
    val identityName by HostLocTable.identityName
    var cookie by HostLocTable.cookie
    var sign by HostLocTable.sign
}
