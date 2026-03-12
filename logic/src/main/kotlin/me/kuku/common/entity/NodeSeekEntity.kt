package me.kuku.common.entity

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

object NodeSeekTable: IntIdTable("node_seek") {
    val identityId = varchar("identity_id", 255)
    val identityName = varchar("identity_name", 255).default("")
    val cookie = varchar("cookie", 1000)
    val sign = enumeration<Sign>("sign").default(Sign.None)

    init {
        index(true, identityId, identityName)
    }
}

enum class Sign(val value: String) {
    Random("随机"), Fix("固定"), None("关闭");

    override fun toString(): String {
        return this.value
    }

    operator fun not(): Sign {
        return if (this == Random) Fix
        else if (this == Fix) None
        else if (this == None) Random
        else None
    }
}

class NodeSeekEntity(id: EntityID<Int>): IntEntity(id) {
    companion object : IntEntityClass<NodeSeekEntity>(NodeSeekTable)

    val identityId by NodeSeekTable.identityId
    val identityName by NodeSeekTable.identityName
    var cookie by NodeSeekTable.cookie
    var sign by NodeSeekTable.sign
}
