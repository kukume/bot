package me.kuku.common.entity

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.EntityChangeType
import org.jetbrains.exposed.v1.dao.EntityHook
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.dao.toEntity
import org.jetbrains.exposed.v1.javatime.CurrentDateTime
import org.jetbrains.exposed.v1.javatime.datetime
import java.time.LocalDateTime

abstract class BaseIntIdTable(name: String) : IntIdTable(name) {
    val identityId = varchar("identity_id", 255).uniqueIndex()
    val identityName = varchar("identity_name", 255).default("")
    val created: Column<LocalDateTime> = datetime("created")
        .defaultExpression(CurrentDateTime)
    val modified: Column<LocalDateTime?> = datetime("updated").nullable()

    init {
        index(true, identityId, identityName)
    }
}

abstract class BaseIntEntity(id: EntityID<Int>, table: BaseIntIdTable) : IntEntity(id) {
    var identityId by table.identityId
    val created: LocalDateTime by table.created
    var modified: LocalDateTime? by table.modified
}

abstract class BaseIntEntityClass<out E : BaseIntEntity>(
    table: BaseIntIdTable
) : IntEntityClass<E>(table) {
    init {
        EntityHook.subscribe { action ->
            if (action.changeType == EntityChangeType.Updated) {
                action.toEntity(this)?.modified = LocalDateTime.now()
            }
        }
    }

}

enum class Status {
    ON, OFF;

    override fun toString(): String {
        return when (this) {
            ON -> "√"
            OFF -> "×"
        }
    }

    operator fun not(): Status {
        return when (this) {
            ON -> OFF
            OFF -> ON
        }
    }
}
