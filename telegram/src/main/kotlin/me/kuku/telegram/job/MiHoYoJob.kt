package me.kuku.telegram.job

import kotlinx.coroutines.delay
import me.kuku.common.entity.*
import me.kuku.common.logic.MiHoYoLogic
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.quartz.JobExecutionContext
import kotlin.time.Duration.Companion.seconds

class MiHoYoGenShinJob: CoroutineJob {
    override suspend fun coroutineExecute(context: JobExecutionContext) {
        suspendTransaction {
            MiHoYoEntity.find { MiHoYoTable.sign eq Status.ON }.toList()
        }.sign(LogType.GenShin) { entity ->
            MiHoYoLogic.sign(entity)
            delay(3.seconds)
        }
    }
}

class MiHoYoMysSignJob: CoroutineJob {
    override suspend fun coroutineExecute(context: JobExecutionContext) {
        suspendTransaction {
            MiHoYoEntity.find { MiHoYoTable.mysSign eq Status.ON }.toList()
        }.sign(LogType.Mys) { entity ->
            MiHoYoLogic.mysSign(entity)
            delay(3.seconds)
        }
    }
}
