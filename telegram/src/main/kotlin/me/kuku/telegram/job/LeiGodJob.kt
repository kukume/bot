package me.kuku.telegram.job

import io.github.dehuckakpyt.telegrambot.TelegramBot
import io.github.dehuckakpyt.telegrambot.factory.keyboard.inlineKeyboard
import io.github.dehuckakpyt.telegrambot.model.telegram.InlineKeyboardButton
import me.kuku.common.entity.LeiShenEntity
import me.kuku.common.entity.LeiShenTable
import me.kuku.common.entity.Status
import me.kuku.common.logic.LeiShenLogic
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.koin.core.component.inject
import org.quartz.JobExecutionContext

class LeiGodJob: CoroutineJob {

    private val telegrambot by inject<TelegramBot>()

    override suspend fun coroutineExecute(context: JobExecutionContext) {

        val entities = suspendTransaction {
            LeiShenEntity.find { LeiShenTable.status eq Status.ON }.toList()
        }
        entities.push { entity ->
            val expiryTime = entity.expiryTime
            try {
                if (System.currentTimeMillis() / 1000 > expiryTime) {
                    val newEntity = LeiShenLogic.login(entity.username, entity.password)
                    suspendTransaction {
                        entity.accountToken = newEntity.accountToken
                        entity.nnToken = newEntity.nnToken
                        entity.expiryTime = newEntity.expiryTime
                    }
                }
            } catch (e: Exception) {
                suspendTransaction {
                    entity.status = Status.OFF
                }
                telegrambot.sendMessage(entity.identityId, """
                    #雷神加速器登录失败提醒
                    您的雷神加速器cookie已失效，重新登录失败，原因：${e.message}
                """.trimIndent())
                return@push
            }
            val userInfo = try {
                LeiShenLogic.userInfo(entity)
            } catch (_: Exception) {
                suspendTransaction {
                    entity.expiryTime = 0
                }
                return@push
            }
            if (userInfo.pauseStatusId == 0) {
                telegrambot.sendMessage(entity.identityId, """
                    #雷神加速器未暂停时间提醒 2小时提醒一次
                    您的雷神加速器未暂停时间，如果您未在玩游戏，请尽快暂停
                """.trimIndent(), replyMarkup = inlineKeyboard(
                    InlineKeyboardButton("暂停时间", callbackData = "leiGodPause")
                ))
            }
        }
    }
}
