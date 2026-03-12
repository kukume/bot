package me.kuku.telegram.ktor

import io.ktor.server.application.*
import me.kuku.telegram.job.*
import org.quartz.*
import org.quartz.impl.StdSchedulerFactory


fun Application.quartz() {
    val schedulerFactory: SchedulerFactory = StdSchedulerFactory()
    val scheduler = schedulerFactory.scheduler

    scheduler.listenerManager.addJobListener(OwnJobListener())

    val list = mutableListOf(
        dailyAtHourAndMinute<BaiduJob>(2, 41),
        dailyAtHourAndMinute<BiliBiliSignJob>(3, 23),
        withIntervalInMinutes<BiliBiliLiveJob>(2),
        withIntervalInMinutes<BiliBiliDynamicJob>(2),
        withIntervalInMinutes<DouYuLiveJob>(1),
        withIntervalInMinutes<DouYuTitleChangeJob>(1),
        withIntervalInMinutes<DouYuPushJob>(1),
        dailyAtHourAndMinute<DouYuSignJob>(6, 3),
        dailyAtHourAndMinute<ECloudJob>(2, 14),
        dailyAtHourAndMinute<HostLocJob>(4, 12),
        withIntervalInMinutes<HuYaJob>(1),
        dailyAtHourAndMinute<KuGouJob>(3, 41),
        dailyAtHourAndMinute<MiHoYoGenShinJob>(8, 13),
        dailyAtHourAndMinute<MiHoYoMysSignJob>(8, 23),
        dailyAtHourAndMinute<NodeSeekJob>(2, 25),
        dailyAtHourAndMinute<SmZdmJob>(6, 32),
        dailyAtHourAndMinute<StepJob>(5, 12),
        dailyAtHourAndMinute<WeiboSignJob>(4, 51),
        withIntervalInMinutes<WeiboDynamicJob>(2),
        withIntervalInHours<EpicJob>(1),
        withIntervalInMinutes<XianBaoJob>(1),
        dailyAtHourAndMinute<NodeLocJob>(5, 13),
        withIntervalInHours<LeiGodJob>(2)
    )

    list.forEach {
        scheduler.scheduleJob(it.jobDetail, it.trigger)
    }

    scheduler.start()

    monitor.subscribe(ApplicationStopping) {
        scheduler.shutdown()
    }

}

private data class JobDetailAndTrigger(val jobDetail: JobDetail, val trigger: Trigger)

private inline fun <reified T: Job> dailyAtHourAndMinute(hour: Int, minute: Int): JobDetailAndTrigger {
    val jobClass = T::class
    val jobDetail = JobBuilder.newJob(jobClass.java)
        .withIdentity(jobClass.simpleName, "group-sign")
        .build()
    val trigger: Trigger = TriggerBuilder.newTrigger()
        .withIdentity("trigger-${jobClass.simpleName}", "trigger-group-sign")
        .startNow()
        .withSchedule(
            CronScheduleBuilder.dailyAtHourAndMinute(hour, minute)
        )
        .build()
    return JobDetailAndTrigger(jobDetail, trigger)
}

private inline fun <reified T: Job> withIntervalInMinutes(intervalInMinutes: Int): JobDetailAndTrigger {
    val jobClass = T::class
    val jobDetail = JobBuilder.newJob(jobClass.java)
        .withIdentity(jobClass.simpleName, "group-sign")
        .build()
    val trigger: Trigger = TriggerBuilder.newTrigger()
        .withIdentity("trigger-${jobClass.simpleName}", "trigger-group-sign")
        .startAt(DateBuilder.futureDate(10, DateBuilder.IntervalUnit.MINUTE))
        .withSchedule(
            SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInMinutes(intervalInMinutes)
                .repeatForever()
        )
        .build()
    return JobDetailAndTrigger(jobDetail, trigger)
}

private inline fun <reified T: Job> withIntervalInHours(intervalInHours: Int): JobDetailAndTrigger {
    val jobClass = T::class
    val jobDetail = JobBuilder.newJob(jobClass.java)
        .withIdentity(jobClass.simpleName, "group-sign")
        .build()
    val trigger: Trigger = TriggerBuilder.newTrigger()
        .withIdentity("trigger-${jobClass.simpleName}", "trigger-group-sign")
        .startAt(DateBuilder.futureDate(10, DateBuilder.IntervalUnit.MINUTE))
        .withSchedule(
            SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInHours(intervalInHours)
                .repeatForever()
        )
        .build()
    return JobDetailAndTrigger(jobDetail, trigger)
}

inline fun <reified T: Job> testQuartz() {

    val jobDetail = JobBuilder.newJob(T::class.java)
        .withIdentity("myJob", "group1")
        .build()

    val trigger = TriggerBuilder.newTrigger()
        .withIdentity("myTrigger", "group1")
        .startNow() // 立即执行
        .build()

    val scheduler = StdSchedulerFactory.getDefaultScheduler()
    scheduler.start()

    scheduler.scheduleJob(jobDetail, trigger)
}

class OwnJobListener: JobListener {
    override fun getName(): String {
        return "owner"
    }

    override fun jobToBeExecuted(context: JobExecutionContext) {
    }

    override fun jobExecutionVetoed(context: JobExecutionContext) {
    }

    override fun jobWasExecuted(context: JobExecutionContext, jobException: JobExecutionException?) {

    }
}