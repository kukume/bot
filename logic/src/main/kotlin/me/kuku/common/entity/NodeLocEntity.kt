package me.kuku.common.entity

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

object NodeLocTable: IntIdTable("node_loc") {

    val identityId = varchar("identity_id", 255)
    val identityName = varchar("identity_name", 255).default("")
    val cookie = varchar("cookie", 1000)
    val csrf = varchar("csrf", 255)
    val sign = enumeration<Status>("sign").default(Status.OFF)


    init {
        index(true, identityId, identityName)
    }

}


class NodeLocEntity(id: EntityID<Int>): IntEntity(id) {

    companion object: IntEntityClass<NodeLocEntity>(NodeLocTable)

    var identityId by NodeLocTable.identityId
    var identityName by NodeLocTable.identityName
    var cookie by NodeLocTable.cookie
    var csrf by NodeLocTable.csrf
    var sign by NodeLocTable.sign

}
