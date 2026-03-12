package me.kuku.common.entity

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

object LeiShenTable: IntIdTable("lei_shen") {
    val identityId = varchar("identity_id", 255)
    val username = varchar("username", 255)
    val password = varchar("password", 255)
    val accountToken = varchar("account_token", 255)
    val nnToken = varchar("nn_token", 255)
    val status = enumeration<Status>("status").default(Status.OFF)
    val expiryTime = long("expiry_time")

    init {
        index(true, identityId)
    }
}

class LeiShenEntity(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<LeiShenEntity>(LeiShenTable)

    val identityId by LeiShenTable.identityId
    var username by LeiShenTable.username
    var password by LeiShenTable.password
    var accountToken by LeiShenTable.accountToken
    var nnToken by LeiShenTable.nnToken
    var status by LeiShenTable.status
    var expiryTime by LeiShenTable.expiryTime
}
