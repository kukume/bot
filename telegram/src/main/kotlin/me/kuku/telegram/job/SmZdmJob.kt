package me.kuku.telegram.job

import kotlinx.coroutines.delay
import me.kuku.common.entity.*
import me.kuku.common.logic.SmZdmLogic
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.quartz.JobExecutionContext

class SmZdmJob: CoroutineJob {
    override suspend fun coroutineExecute(context: JobExecutionContext) {
        suspendTransaction {
            SmZdmEntity.find { SmZdmTable.sign eq Status.ON }.toList()
        }.sign(LogType.SmZdm) { entity ->
            delay(3000)
            SmZdmLogic.webSign(entity)
            SmZdmLogic.appSign(entity)
        }
    }
}
