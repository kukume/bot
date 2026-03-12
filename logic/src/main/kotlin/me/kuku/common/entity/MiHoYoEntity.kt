package me.kuku.common.entity

import kotlinx.serialization.json.Json
import me.kuku.common.logic.MiHoYoFix
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.json.json

object MiHoYoTable: IntIdTable("mi_ho_yo") {
    val identityId = varchar("identity_id", 255)
    val identityName = varchar("identity_name", 255).default("")
    val aid = varchar("aid", 255)
    val mid = varchar("mid", 255)
    val cookie = varchar("cookie", 1000).nullable()
    val token = varchar("token", 255).nullable()
    val sToken = varchar("s_token", 255).nullable()
    val ticket = varchar("ticket", 255).nullable()
    val sign = enumeration<Status>("sign").default(Status.OFF)
    val mysSign = enumeration<Status>("mys_sign").default(Status.OFF)
    val fix = json<MiHoYoFix>("fix", Json.Default)

    init {
        index(true, identityId, identityName)
    }
}

class MiHoYoEntity(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<MiHoYoEntity>(MiHoYoTable)

    val identityId by MiHoYoTable.identityId
    val identityName by MiHoYoTable.identityName
    var aid by MiHoYoTable.aid
    var mid by MiHoYoTable.mid
    var cookie by MiHoYoTable.cookie
    var token by MiHoYoTable.token
    var sToken by MiHoYoTable.sToken
    var ticket by MiHoYoTable.ticket
    var sign by MiHoYoTable.sign
    var mysSign by MiHoYoTable.mysSign
    var fix by MiHoYoTable.fix


    fun hubCookie(): String {
        if (token == null) error("未设置token，请使用app账号密码重新登录")
        return "stuid=$aid; stoken=$token; mid=$mid; "
    }

    fun cookie(): String {
        return cookie ?: error("未设置cookie，请使用app账号密码以外的重新登录")
    }
}
