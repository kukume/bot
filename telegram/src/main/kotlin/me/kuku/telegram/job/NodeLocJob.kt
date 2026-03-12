package me.kuku.telegram.job

import kotlinx.coroutines.delay
import me.kuku.common.entity.LogType
import me.kuku.common.entity.NodeLocEntity
import me.kuku.common.entity.NodeLocTable
import me.kuku.common.entity.Status
import me.kuku.common.logic.NodeLocLogic
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.quartz.JobExecutionContext
import kotlin.time.Duration.Companion.seconds

class NodeLocJob: CoroutineJob {

    override suspend fun coroutineExecute(context: JobExecutionContext) {
        suspendTransaction {
            NodeLocEntity.find { NodeLocTable.sign eq Status.ON }.toList()
        }.sign(LogType.NodeLoc) {
            NodeLocLogic.sign(it)
            delay(3.seconds)
        }
    }
}
