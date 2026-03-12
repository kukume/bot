package me.kuku.common.entity

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

object DouYuTable: IntIdTable("dou_yu") {
    val identityId = varchar("identity_id", 255)
    val identityName = varchar("identity_name", 255).default("")
    val cookie = varchar("cookie", 2000)
    val live = enumeration<Status>("live").default(Status.OFF)
    val fishGroup = enumeration<Status>("fish_group").default(Status.OFF)
    val push = enumeration<Status>("push").default(Status.OFF)
    val titleChange = enumeration<Status>("title_change").default(Status.OFF)

    init {
        index(true, identityId, identityName)
    }
}

class DouYuEntity(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<DouYuEntity>(DouYuTable)

    val identityId by DouYuTable.identityId
    val identityName by DouYuTable.identityName
    var cookie by DouYuTable.cookie
    var live by DouYuTable.live
    var fishGroup by DouYuTable.fishGroup
    var push by DouYuTable.push
    var titleChange by DouYuTable.titleChange
}
