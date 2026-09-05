package com.fitplan.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 一次身体参数读数（对应人体分析仪/InBody 的一次测量）。
 * 各指标可空：未测的项不填。
 */
@Entity(
    tableName = "body_records",
    indices = [Index("timestamp")]
)
data class BodyRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 测量时间（epoch millis），用于排序与趋势横轴 */
    val timestamp: Long,
    val weightKg: Double? = null,
    val bodyFatPct: Double? = null,
    val muscleKg: Double? = null,
    val boneKg: Double? = null,
    val waterPct: Double? = null,
    val bmi: Double? = null,
    val bmrKcal: Double? = null,
    val visceralFat: Double? = null,
    val waistCm: Double? = null,
    /** 身高(cm) */
    val heightCm: Double? = null,
    /** 蛋白质(%) */
    val proteinPct: Double? = null,
    /** 皮下脂肪(%) */
    val subcutaneousPct: Double? = null,
    val note: String? = null,
    /** 关联照片在本地文件里的路径（拍照/选图留档），可空 */
    val photoPath: String? = null
)
