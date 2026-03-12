package me.kuku.telegram.handler

import io.github.dehuckakpyt.telegrambot.ext.container.fromId
import io.github.dehuckakpyt.telegrambot.factory.keyboard.inlineKeyboard
import io.github.dehuckakpyt.telegrambot.handler.BotHandler
import me.kuku.common.entity.LeiShenEntity
import me.kuku.common.entity.LeiShenTable
import me.kuku.common.logic.LeiShenLogic
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.upsert
import org.koin.core.annotation.Factory

@Factory
class LeiGodHandler: BotHandler({

    command("/leigod") {
        val entity = suspendTransaction {
            LeiShenEntity.find { LeiShenTable.identityId eq fromId.toString() }.firstOrNull()
        }
        val str = if (entity == null) "未登录\n"
        else
        {
            suspend fun userInfo(): String {
                val userInfo = LeiShenLogic.userInfo(entity)
                val pauseStatusId = userInfo.pauseStatusId
                val pause = if (pauseStatusId == 1) "暂停"
                else "恢复"
                return "暂停状态：${pause}\n提醒状态：${entity.status}\n"
            }

            kotlin.runCatching {
                userInfo()
            }.recoverCatching {
                val leiShenLogin = LeiShenLogic.login(entity.username, entity.password)
                suspendTransaction {
                    entity.accountToken = leiShenLogin.accountToken
                    entity.nnToken = leiShenLogin.nnToken
                    entity.expiryTime = leiShenLogin.expiryTime
                }
                userInfo()
            }.getOrThrow()
        }
        sendMessage("雷神加速器\n${str}请选择", replyMarkup = inlineKeyboard(
            callbackButton("登录", "leiGodLogin"),
            callbackButton("提醒", "leiGodStatus"),
            callbackButton("暂停/恢复", "leiGodSwitch"),
            callbackButton("删除", "leiGodDelete")
        ))
    }


    callback("leiGodLogin") {
        editMessageText(message.messageId, "请发送雷神加速器的账号")
        next("leiGodLogin1")
    }

    step("leiGodLogin1") {
        sendMessage("请发送雷神加速器的密码")
        next("leiGodLogin2", text)
    }

    step("leiGodLogin2") {
        val password0 = text
        val account = transferred<String>()
        sendMessage("需要识别验证码，请等待")
        val leiShenLogin = LeiShenLogic.login(account, password0)
        suspendTransaction {
            LeiShenTable.upsert(LeiShenTable.identityId) {
                it[identityId] = fromId.toString()
                it[username] = account
                it[password] = password0
                it[accountToken] = leiShenLogin.accountToken
                it[nnToken] = leiShenLogin.nnToken
                it[expiryTime] = leiShenLogin.expiryTime
            }
        }
        sendMessage("绑定雷神加速器成功")
    }

    callback("leiGodStatus") {
        val entity = suspendTransaction {
            LeiShenEntity.find { LeiShenTable.identityId eq fromId.toString() }.firstOrNull()
        }
        if (entity == null) answerCallbackQuery(query.id, "未绑定雷神加速器", true)
        else {
            suspendTransaction {
                entity.status = !entity.status
            }
        }
        editMessageText(message.messageId, "雷神加速器提醒已切换")
    }

    callback("leiGodSwitch") {
        val entity = suspendTransaction {
            LeiShenEntity.find { LeiShenTable.identityId eq fromId.toString() }.firstOrNull()
        }
        if (entity == null) answerCallbackQuery(query.id, "未绑定雷神加速器", true)
        else {
            val userInfo = LeiShenLogic.userInfo(entity)
            val pauseStatusId = userInfo.pauseStatusId
            if (pauseStatusId == 1) {
                LeiShenLogic.recover(entity)
                editMessageText(message.messageId, "雷神加速器已恢复时间")
            } else {
                LeiShenLogic.pause(entity)
                editMessageText(message.messageId, "雷神加速器已暂停时间")
            }
        }
    }

    callback("leiGodPause") {
        val entity = suspendTransaction {
            LeiShenEntity.find { LeiShenTable.identityId eq fromId.toString() }.firstOrNull()
        }
        if (entity == null) answerCallbackQuery(query.id, "未绑定雷神加速器", true)
        else {
            LeiShenLogic.pause(entity)
            editMessageText(message.messageId, "雷神加速器已暂停时间")
        }
    }

    callback("leiGodDelete") {
        suspendTransaction {
            LeiShenTable.deleteWhere {
                identityId eq fromId.toString()
            }
        }
        editMessageText(message.messageId, "雷神加速器删除成功")
    }



})
