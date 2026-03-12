package me.kuku.telegram.job

import me.kuku.common.entity.*
import me.kuku.common.logic.HostLocLogic
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.quartz.JobExecutionContext

class HostLocJob: CoroutineJob {
    override suspend fun coroutineExecute(context: JobExecutionContext) {
        suspendTransaction {
            HostLocEntity.find { HostLocTable.sign eq Status.ON }.toList()
        }.sign(LogType.HostLoc) { entity ->
            HostLocLogic.sign(entity)
        }
    }
}
