package com.fitplan.app.data.workout

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 一次「开始训练」的会话（一次完整训练）。
 * sourceKind: DAY=来自某天计划 / PLAN=来自方案一节 / FREE=自由选择
 */
@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long? = null,
    val title: String? = null,
    /** DAY / PLAN / FREE */
    val sourceKind: String = "FREE",
    /** 来源参考：DAY->dateEpochDay；PLAN->sessionId；FREE->null */
    val sourceRef: Long? = null,
    val note: String? = null
)

/**
 * 训练中做下的每一组：记录真实重量×次数（留存完整历史）。
 * 存动作名称快照，避免动作库删除后历史丢失。
 */
@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId"), Index("exerciseId")]
)
data class WorkoutSet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val muscleGroup: String,
    /** 该动作内第几组（从1起） */
    val setIndex: Int,
    val weightKg: Double? = null,
    val reps: Int,
    /** 该动作目标次数（快照），便于复盘 */
    val plannedReps: Int? = null,
    val restSeconds: Int? = null,
    val loggedAt: Long = System.currentTimeMillis()
)
