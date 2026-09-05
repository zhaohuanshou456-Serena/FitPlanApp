package com.fitplan.app.ui.workout

/** 本次训练要做的单个动作（由各种来源归一化而来） */
data class RunExercise(
    val exerciseId: Long,
    val name: String,
    val muscle: String,
    val equipment: String,
    val sets: Int,
    /** 每组目标次数 */
    val reps: Int,
    val restSeconds: Int,
    /** 起始重量(kg)；null=自重 */
    val startWeight: Double?,
    /** 来自某天计划时，对应的 ScheduledExercise id（用于完成后打勾） */
    val scheduledId: Long = 0L
)

/** 导航前临时存放本次训练请求（单用户本地，够用） */
object PendingRun {
    var exercises: List<RunExercise> = emptyList()
    var sourceKind: String = "FREE" // DAY / PLAN / FREE
    var sourceRef: Long? = null
    var title: String? = null
}
