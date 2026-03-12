package me.kuku.common.entity

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

object UserConfigTable: IntIdTable("user_config") {
    val identityId = varchar("identity_id", 255).uniqueIndex()
    val epicFreeGamePush = enumeration<Status>("epic_free_game_push").default(Status.OFF)
    val xianBaoPush = enumeration<Status>("xian_bao_push").default(Status.OFF)
}

class UserConfigEntity(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<UserConfigEntity>(UserConfigTable)

    var identityId by UserConfigTable.identityId
    var epicFreeGamePush by UserConfigTable.epicFreeGamePush
    var xianBaoPush by UserConfigTable.xianBaoPush
}
