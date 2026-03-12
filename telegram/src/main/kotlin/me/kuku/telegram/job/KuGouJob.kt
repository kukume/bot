package me.kuku.telegram.job

import kotlinx.coroutines.delay
import me.kuku.common.entity.*
import me.kuku.common.logic.KuGouLogic
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.quartz.JobExecutionContext
import kotlin.time.Duration.Companion.seconds

class KuGouJob: CoroutineJob {
    override suspend fun coroutineExecute(context: JobExecutionContext) {
        suspendTransaction {
            KuGouEntity.find { KuGouTable.sign eq Status.ON }.toList()
        }.sign(LogType.KuGou) { entity ->
            KuGouLogic.listenMusic(entity)
            repeat(8) {
                delay(25.seconds)
                KuGouLogic.watchAd(entity)
            }
        }
    }
}
