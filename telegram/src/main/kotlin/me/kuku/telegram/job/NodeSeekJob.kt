package me.kuku.telegram.job

import me.kuku.common.entity.LogType
import me.kuku.common.entity.NodeSeekEntity
import me.kuku.common.entity.NodeSeekTable
import me.kuku.common.entity.Sign
import me.kuku.common.logic.NodeSeekLogic
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.quartz.JobExecutionContext

class NodeSeekJob: CoroutineJob {
    override suspend fun coroutineExecute(context: JobExecutionContext) {
        suspendTransaction {
            NodeSeekEntity.find { NodeSeekTable.sign neq Sign.None }.toList()
        }.sign(LogType.NodeSeek) { entity ->
            NodeSeekLogic.sign0(entity, entity.sign == Sign.Random)
        }
    }
}
//
//class NodeSeekQueryJob: CoroutineJob {
//    override suspend fun coroutineExecute(context: JobExecutionContext) {
//        newSuspendedTransaction {
//            NodeSeekEntity.find { NodeSeekTable.sign neq NodeSeekTable.Sign.None }.toList()
//        }.forEach { entity ->
//            val num = NodeSeekLogic.querySign(entity)
//            println(num)
//        }
//    }
//}
