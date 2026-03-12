package me.kuku.common.entity

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

object KuGouTable: IntIdTable("ku_gou") {
    val identityId = varchar("identity_id", 255)
    val identityName = varchar("identity_name", 255).default("")
    val token = varchar("token", 255)
    val userid = long("userid")
    val kuGoo = varchar("ku_goo", 600)
    val mid = varchar("mid", 255)
    val sign = enumeration<Status>("sign").default(Status.OFF)

    init {
        index(true, identityId, identityName)
    }
}

class KuGouEntity(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<KuGouEntity>(KuGouTable)

    val identityId by KuGouTable.identityId
    val identityName by KuGouTable.identityName
    var token by KuGouTable.token
    var userid by KuGouTable.userid
    var kuGoo by KuGouTable.kuGoo
    var mid by KuGouTable.mid
    var sign by KuGouTable.sign

}
