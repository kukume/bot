package me.kuku.common.entity

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

object StepTable: IntIdTable("step") {
    val identityId = varchar("identity_id", 255)
    val identityName = varchar("identity_name", 255).default("")
    val leXinCookie = varchar("le_xin_cookie", 500).nullable()
    val leXinUserid = varchar("le_xin_userid", 255).nullable()
    val leXinAccessToken = varchar("le_xin_access_token", 500).nullable()
    val miLoginToken = varchar("mi_login_token", 300).nullable()
    val step = integer("step").default(-1)
    val offset = enumeration<Status>("offset").default(Status.OFF)

    init {
        index(true, identityId, identityName)
    }
}

class StepEntity(id: EntityID<Int>): IntEntity(id) {
    companion object : IntEntityClass<StepEntity>(StepTable)

    val identityId by StepTable.identityId
    val identityName by StepTable.identityName
    var leXinCookie by StepTable.leXinCookie
    var leXinUserid by StepTable.leXinUserid
    var leXinAccessToken by StepTable.leXinAccessToken
    var miLoginToken by StepTable.miLoginToken
    var step by StepTable.step
    var offset by StepTable.offset
}
