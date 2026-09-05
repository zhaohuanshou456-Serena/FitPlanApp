package com.fitplan.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 动作库中的一条动作/器械记录。
 */
@Entity(
    tableName = "exercises",
    indices = [Index(value = ["name"], unique = true)]
)
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** 目标肌群，例如：胸、背、腿、肩、手臂、核心 */
    val muscleGroup: String,
    /** 器械/形式，例如：杠铃、哑铃、器械、自重、绳索 */
    val equipment: String,
    val note: String? = null,
    val isCustom: Boolean = false,
    /** 训练类型 hint：Strength / Cardio 等，保留扩展 */
    val kind: String = "STRENGTH"
)
