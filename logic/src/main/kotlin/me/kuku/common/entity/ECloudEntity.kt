package me.kuku.common.entity

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

object ECloudTable: IntIdTable("e_cloud") {
    val identityId = varchar("identity_id", 255)
    val identityName = varchar("identity_name", 255).default("")
    val cookie = varchar("cookie", 255)
    val eCookie = varchar("e_cookie", 800)
    val sign = enumeration<Status>("sign").default(Status.OFF)

    init {
        index(true, identityId, identityName)
    }
}

class ECloudEntity(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<ECloudEntity>(ECloudTable)

    val identityId by ECloudTable.identityId
    val identityName by ECloudTable.identityName
    var cookie by ECloudTable.cookie
    var eCookie by ECloudTable.eCookie
    var sign by ECloudTable.sign
}
