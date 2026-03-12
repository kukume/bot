package me.kuku.telegram.job

import kotlinx.coroutines.delay
import me.kuku.common.entity.*
import me.kuku.common.logic.LeXinStepLogic
import me.kuku.common.logic.XiaomiStepLogic
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.quartz.JobExecutionContext
import kotlin.random.Random

class StepJob: CoroutineJob {
    override suspend fun coroutineExecute(context: JobExecutionContext) {
        suspendTransaction {
            StepEntity.find { StepTable.step greater 0 }.toList()
        }.sign(LogType.Step) { entity ->
            var step = entity.step
            if (entity.offset == Status.ON) {
                step = Random.nextInt(step - 1000, step + 1000)
            }
            if (entity.miLoginToken != null) {
                XiaomiStepLogic.modifyStepCount(entity, step)
            }
            if (entity.leXinCookie != null) {
                LeXinStepLogic.modifyStepCount(entity, step)
            }
            delay(3000)
        }
    }
}
