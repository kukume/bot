package me.kuku.common.entity

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.javatime.CurrentDateTime
import org.jetbrains.exposed.v1.javatime.datetime

object SignLogTable: IntIdTable("sign_log") {
    val identityId = varchar("identity_id", 255)
    val identityName = varchar("identity_name", 255)
    val type = enumeration<LogType>("sign_log_type")
    val success = bool("success")
    val remark = varchar("text", 255).nullable()
    val errReason = varchar("error_reason", 255).nullable()
    val exceptionStack = text("exception_stack").nullable()
    val logTime = datetime("log_time").defaultExpression(CurrentDateTime)

}

enum class LogType(val value: String) {
    Baidu("百度"), BiliBili("哔哩哔哩"), YuBa("斗鱼鱼吧"), ECloud("天翼云盘"),
    HostLoc("HostLoc"), KuGou("酷狗"), GenShin("原神"), Mys("米游社"),
    NodeSeek("NodeSeek"), SmZdm("什么值得买"), Step("刷步数"), Weibo("微博"),
    NodeLoc("NodeLoc")
    ;
}


class SignLogEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object: IntEntityClass<SignLogEntity>(SignLogTable)

    val identityId by SignLogTable.identityId
    val identityName by SignLogTable.identityName
    val type by SignLogTable.type
    val success by SignLogTable.success
    val remark by SignLogTable.remark
    val errReason by SignLogTable.errReason
    val exceptionStack by SignLogTable.exceptionStack
    val logTime by SignLogTable.logTime
}
