package me.kuku.telegram.job

import me.kuku.common.entity.*
import me.kuku.common.logic.ECloudLogic
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.quartz.JobExecutionContext

class ECloudJob: CoroutineJob {
    override suspend fun coroutineExecute(context: JobExecutionContext) {
        suspendTransaction {
            ECloudEntity.find { ECloudTable.sign eq Status.ON }.toList()
        }.sign(LogType.ECloud) { entity ->
            ECloudLogic.sign(entity)
        }
    }
}
