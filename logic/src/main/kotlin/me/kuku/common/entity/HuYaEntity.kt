package me.kuku.common.entity

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

object HuYaTable: IntIdTable("hu_ya") {
    val identityId = varchar("identity_id", 255)
    val identityName = varchar("identity_name", 255).default("")
    val cookie = varchar("cookie", 800)
    val push = enumeration<Status>("push").default(Status.OFF)

    init {
        index(true, identityId, identityName)
    }
}

class HuYaEntity(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<HuYaEntity>(HuYaTable)

    val identityId by HuYaTable.identityId
    val identityName by HuYaTable.identityName
    var cookie by HuYaTable.cookie
    var push by HuYaTable.push
}
