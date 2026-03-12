package me.kuku.common.entity

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

object SmZdmTable: IntIdTable("sm_zdm") {
    val identityId = varchar("identity_id", 255)
    val identityName = varchar("identity_name", 255).default("")
    val cookie = varchar("cookie", 2000)
    val sign = enumeration<Status>("status").default(Status.OFF)

    init {
        index(true, identityId, identityName)
    }
}

class SmZdmEntity(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<SmZdmEntity>(SmZdmTable)

    val identityId by SmZdmTable.identityId
    val identityName by SmZdmTable.identityName
    var cookie by SmZdmTable.cookie
    var sign by SmZdmTable.sign
}
