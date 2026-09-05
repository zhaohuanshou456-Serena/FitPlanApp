package com.fitplan.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 某一天计划（或已执行）的一次锻炼条目。
 * 通过 [dateEpochDay]（自 1970-01-01 起的天数）锚定到具体某一天，
 * 由用户在 UI 中为某天添加若干动作及其组数/次数/重量，去健身房照单执行并打勾。
 */
@Entity(
    tableName = "scheduled_exercises",
    foreignKeys = [
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("exerciseId"), Index("dateEpochDay")]
)
data class ScheduledExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val exerciseId: Long,
    val targetSets: Int,
    val targetReps: Int,
    /** 目标重量(kg)。null 表示自重/无负重。 */
    val weight: Double? = null,
    val restSeconds: Int = 90,
    val sortOrder: Int = 0,
    val isCompleted: Boolean = false,
    val actualNote: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
