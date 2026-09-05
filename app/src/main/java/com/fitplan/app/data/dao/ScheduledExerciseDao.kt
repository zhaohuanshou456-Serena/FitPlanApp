package com.fitplan.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.fitplan.app.data.entity.ScheduledExercise
import kotlinx.coroutines.flow.Flow

/** 某日计划条目 + 对应动作信息的联表结果 */
data class ScheduledWithExercise(
    val id: Long,
    val dateEpochDay: Long,
    val exerciseId: Long,
    val targetSets: Int,
    val targetReps: Int,
    val weight: Double?,
    val restSeconds: Int,
    val sortOrder: Int,
    val isCompleted: Boolean,
    val actualNote: String?,
    val exerciseName: String,
    val muscleGroup: String,
    val equipment: String,
    val exerciseNote: String?
)

@Dao
interface ScheduledExerciseDao {

    @Query(
        """
        SELECT se.id, se.dateEpochDay, se.exerciseId, se.targetSets, se.targetReps,
               se.weight, se.restSeconds, se.sortOrder, se.isCompleted, se.actualNote,
               e.name AS exerciseName, e.muscleGroup, e.equipment, e.note AS exerciseNote
        FROM scheduled_exercises se
        JOIN exercises e ON e.id = se.exerciseId
        WHERE se.dateEpochDay = :day
        ORDER BY se.sortOrder ASC, se.id ASC
        """
    )
    fun observeForDate(day: Long): Flow<List<ScheduledWithExercise>>

    @Query(
        """
        SELECT se.id, se.dateEpochDay, se.exerciseId, se.targetSets, se.targetReps,
               se.weight, se.restSeconds, se.sortOrder, se.isCompleted, se.actualNote,
               e.name AS exerciseName, e.muscleGroup, e.equipment, e.note AS exerciseNote
        FROM scheduled_exercises se
        JOIN exercises e ON e.id = se.exerciseId
        ORDER BY se.dateEpochDay DESC, se.sortOrder ASC
        """
    )
    fun observeAll(): Flow<List<ScheduledWithExercise>>

    @Query("SELECT COUNT(*) FROM scheduled_exercises WHERE dateEpochDay = :day AND isCompleted = 0")
    suspend fun countIncomplete(day: Long): Int

    @Insert
    suspend fun insert(item: ScheduledExercise): Long

    @Insert
    suspend fun insertAll(items: List<ScheduledExercise>)

    @Update
    suspend fun update(item: ScheduledExercise)

    @Query("DELETE FROM scheduled_exercises WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM scheduled_exercises WHERE dateEpochDay = :day")
    suspend fun deleteForDate(day: Long)

    @Query("SELECT * FROM scheduled_exercises WHERE dateEpochDay < :day ORDER BY dateEpochDay DESC")
    suspend fun beforeDate(day: Long): List<ScheduledExercise>

    @Query("SELECT * FROM scheduled_exercises WHERE dateEpochDay = :day ORDER BY sortOrder ASC")
    suspend fun forDate(day: Long): List<ScheduledExercise>

    @Query("UPDATE scheduled_exercises SET isCompleted = :done WHERE id = :id")
    suspend fun setCompleted(id: Long, done: Boolean)

    @Query(
        "UPDATE scheduled_exercises SET targetSets = :sets, targetReps = :reps, weight = :weight, restSeconds = :rest WHERE id = :id"
    )
    suspend fun updateParams(id: Long, sets: Int, reps: Int, weight: Double?, rest: Int)

    @Query("SELECT * FROM scheduled_exercises")
    suspend fun all(): List<ScheduledExercise>

    @Query("DELETE FROM scheduled_exercises")
    suspend fun clearAll()
}
