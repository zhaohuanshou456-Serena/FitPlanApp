package com.fitplan.app.data.program

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.fitplan.app.data.entity.Exercise

/**
 * 一套可复用的训练方案（模板）。
 */
@Entity(tableName = "programs")
data class Program(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val goal: String? = null,
    /** 一轮共几节（几分化/几天的轮换） */
    val dayCount: Int = 4,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 方案里的一节（如 “A 上肢”、“Day1 胸+三头”）。
 */
@Entity(
    tableName = "program_sessions",
    foreignKeys = [
        ForeignKey(
            entity = Program::class,
            parentColumns = ["id"],
            childColumns = ["programId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("programId")]
)
data class ProgramSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val programId: Long,
    val sessionOrder: Int,
    val name: String,
    val note: String? = null
)

/**
 * 某节里的一个动作（引用动作库），带组/次区间/起始重量等设计。
 */
@Entity(
    tableName = "program_items",
    foreignKeys = [
        ForeignKey(
            entity = ProgramSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId"), Index("exerciseId")]
)
data class ProgramItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val itemOrder: Int,
    val targetSets: Int,
    val repMin: Int,
    val repMax: Int,
    /** 起始重量(kg)；null=自重 */
    val weightKg: Double? = null,
    val restSeconds: Int = 90,
    /** 动作要领/安全提醒 */
    val formCue: String? = null,
    val enableProgressive: Boolean = true,
    val note: String? = null
)

/**
 * 某一天应用了方案的哪一节（映射；执行实例仍用 ScheduledExercise）。
 */
@Entity(
    tableName = "program_day_apply",
    foreignKeys = [
        ForeignKey(
            entity = ProgramSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["dateEpochDay"], unique = true), Index("sessionId")]
)
data class ProgramDayApply(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val programId: Long,
    val sessionId: Long,
    val appliedAt: Long = System.currentTimeMillis()
)

/**
 * 某个动作的渐进基线：最近最好表现 + 建议重量。一个动作全局一条。
 */
@Entity(tableName = "exercise_progression")
data class ExerciseProgression(
    @PrimaryKey val exerciseId: Long,
    val bestWeightKg: Double? = null,
    val bestReps: Int? = null,
    val suggestedWeightKg: Double? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
