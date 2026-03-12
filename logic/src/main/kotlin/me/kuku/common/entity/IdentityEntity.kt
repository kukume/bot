package me.kuku.common.entity

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

object IdentityTable: IntIdTable("identity") {
    val identityId = varchar("identity_id", 255)
    val identityName = varchar("identity_name", 255)
    val showName = varchar("show_name", 255).nullable()

    init {
        index(true, identityId, identityName)
    }

}

class IdentityEntity(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<IdentityEntity>(IdentityTable)

    val identityId by IdentityTable.identityId
    val identityName by IdentityTable.identityName
    var showName by IdentityTable.showName

    fun name() = showName ?: identityName
}
