package me.kuku.telegram.job

import kotlinx.coroutines.delay
import me.kuku.common.entity.*
import me.kuku.common.logic.BaiduLogic
import me.kuku.telegram.job.sign
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.quartz.JobExecutionContext

class BaiduJob: CoroutineJob {
    // 0 41 2 * * ?
    override suspend fun coroutineExecute(context: JobExecutionContext) {
        suspendTransaction {
            BaiduEntity.find { BaiduTable.sign eq Status.ON }.toList()
        }.sign(LogType.Baidu) { entity ->
            for (i in 0 until 12) {
                delay(1000 * 15)
                BaiduLogic.ybbWatchAd(entity)
            }
            for (i in 0 until 4) {
                delay(1000 * 30)
                BaiduLogic.ybbWatchAd(entity, "v3")
            }
            BaiduLogic.ybbSign(entity)
            delay(2000)
            BaiduLogic.ybbExchangeVip(entity)
            BaiduLogic.tieBaSign(entity)
        }
    }
}
