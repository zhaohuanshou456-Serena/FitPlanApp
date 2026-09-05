package com.fitplan.app.data.program

import androidx.room.withTransaction
import com.fitplan.app.data.FitPlanDatabase
import com.fitplan.app.data.entity.Exercise

/** 内置种子的一节 */
internal data class SessionSeed(
    val name: String,
    val items: List<ItemSeed>
)

internal data class ItemSeed(
    val exerciseName: String,
    val muscle: String,
    val equipment: String,
    val sets: Int,
    val repMin: Int,
    val repMax: Int,
    val weightKg: Double? = null,
    val restSeconds: Int,
    val cue: String? = null
)

/**
 * 内置「U/L 轮换 A/B/C/D」训练方案。仅当方案表为空时写入，避免覆盖用户自己的方案。
 */
object ProgramSeeder {

    private val sessions = listOf(
        SessionSeed("A · 上肢 I（水平推拉·卧推主打）", listOf(
            ItemSeed("杠铃卧推", "胸", "杠铃", 4, 8, 10, restSeconds = 120, cue = "肩胛后收下沉、杠落中下胸、脚踩实"),
            ItemSeed("高位下拉", "背", "器械", 4, 10, 12, restSeconds = 90, cue = "沉肩、下拉到锁骨、勿过度后仰"),
            ItemSeed("坐姿绳索划船", "背", "绳索", 3, 10, 12, restSeconds = 90, cue = "挺胸收背、肩胛后缩"),
            ItemSeed("上斜哑铃卧推", "胸", "哑铃", 3, 10, 12, restSeconds = 90, cue = "上胸为主、下放控制"),
            ItemSeed("哑铃侧平举", "肩", "哑铃", 3, 12, 15, restSeconds = 60, cue = "不要耸肩、手肘微屈"),
            ItemSeed("绳索下压", "手臂", "绳索", 3, 12, 15, restSeconds = 60, cue = "肘夹紧身体")
        )),
        SessionSeed("B · 下肢 I（深蹲主导）", listOf(
            ItemSeed("杠铃深蹲", "腿", "杠铃", 4, 8, 10, restSeconds = 150, cue = "大腿平行、膝与脚尖同向、核心绷紧"),
            ItemSeed("腿举", "腿", "器械", 3, 10, 12, restSeconds = 120, cue = "腰贴靠垫、不锁膝到底"),
            ItemSeed("罗马尼亚硬拉", "腿", "杠铃", 3, 10, 12, restSeconds = 150, cue = "髋向后、杠贴腿、背直不塌腰"),
            ItemSeed("腿屈伸", "腿", "器械", 3, 12, 15, restSeconds = 60),
            ItemSeed("腿弯举", "腿", "器械", 3, 12, 15, restSeconds = 60),
            ItemSeed("站姿提踵", "腿", "器械", 3, 15, 20, restSeconds = 45, cue = "顶峰停顿")
        )),
        SessionSeed("C · 上肢 II（推举/引体 + 后束）", listOf(
            ItemSeed("坐姿杠铃推举", "肩", "杠铃", 4, 8, 10, restSeconds = 120, cue = "核心收紧、腰不反弓"),
            ItemSeed("引体向上", "背", "自重", 4, 6, 10, restSeconds = 120, cue = "全幅度；做不了用辅助引体"),
            ItemSeed("单臂哑铃划船", "背", "哑铃", 3, 10, 12, restSeconds = 90, cue = "背部发力、不耸肩借力"),
            ItemSeed("蝴蝶机夹胸", "胸", "器械", 3, 12, 15, restSeconds = 60, cue = "顶峰收紧1s"),
            ItemSeed("面拉", "肩", "绳索", 3, 15, 15, restSeconds = 60, cue = "后拉时肘外展、外旋"),
            ItemSeed("锤式弯举", "手臂", "哑铃", 3, 12, 15, restSeconds = 60),
            ItemSeed("反向蝴蝶机", "肩", "器械", 3, 15, 15, restSeconds = 60, cue = "后束补充")
        )),
        SessionSeed("D · 下肢 II（髋主导）", listOf(
            ItemSeed("罗马尼亚硬拉", "腿", "杠铃", 4, 8, 10, restSeconds = 150, cue = "髋向后、杠贴腿、背直"),
            ItemSeed("杠铃臀桥", "臀", "杠铃", 4, 10, 12, restSeconds = 120, cue = "顶峰夹臀停留"),
            ItemSeed("腿举", "腿", "器械", 3, 10, 12, restSeconds = 120),
            ItemSeed("腿弯举", "腿", "器械", 3, 12, 15, restSeconds = 60),
            ItemSeed("腿屈伸", "腿", "器械", 3, 12, 15, restSeconds = 60),
            ItemSeed("农夫行走", "核心", "哑铃", 3, 30, 30, restSeconds = 60, cue = "躯干直立、握紧")
        ))
    )

    suspend fun ensure(db: FitPlanDatabase): Boolean {
        if (db.programDao().allOrdered().isNotEmpty()) return false
        db.withTransaction {
            val programId = db.programDao().insert(
                Program(name = "U/L 轮换 A/B/C/D", goal = "减脂/保肌", dayCount = 4,
                    note = "内置推荐：每周 4 练按 A→B→C→D 轮换，可进入今日顶部一键应用")
            )
            sessions.forEachIndexed { order, seed ->
                val sessionId = db.programSessionDao().insert(
                    ProgramSession(programId = programId, sessionOrder = order + 1, name = seed.name)
                )
                seed.items.forEachIndexed { i, it ->
                    val exId = db.exerciseDao().idByName(it.exerciseName)
                        ?: db.exerciseDao().insert(
                            Exercise(name = it.exerciseName, muscleGroup = it.muscle, equipment = it.equipment,
                                note = it.cue, isCustom = false)
                        )
                    db.programItemDao().insert(
                        ProgramItem(
                            sessionId = sessionId, exerciseId = exId, itemOrder = i,
                            targetSets = it.sets, repMin = it.repMin, repMax = it.repMax,
                            weightKg = it.weightKg, restSeconds = it.restSeconds,
                            formCue = it.cue, enableProgressive = true
                        )
                    )
                }
            }
        }
        return true
    }
}
