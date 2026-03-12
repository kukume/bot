package me.kuku.common.entity

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

object BaiduTable: IntIdTable("baidu") {
    val identityId = varchar("identity_id", 255)
    val identityName = varchar("identity_name", 255).default("")
    val cookie = varchar("cookie", 600)
    val tieBaSToken = varchar("tie_ba_s_token", 255).nullable()
    val sign = enumeration<Status>("sign").default(Status.OFF)

    init {
        index(true, identityId, identityName)
    }
}

class BaiduEntity(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<BaiduEntity>(BaiduTable)

    val identityId by BaiduTable.identityId
    val identityName by BaiduTable.identityName
    var cookie by BaiduTable.cookie
    var tieBaSToken by BaiduTable.tieBaSToken
    var sign by BaiduTable.sign

    private fun otherCookie(sToken: String): String {
        return "BDUSS=.*?;".toRegex().find(cookie)!!.value + "STOKEN=$sToken; "
    }

    fun teiBaCookie(): String {
        return otherCookie(tieBaSToken ?: error("tieba token not found"))
    }

}

data class BaiduPojo(val cookie: String)
