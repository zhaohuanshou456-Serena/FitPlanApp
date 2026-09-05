package com.fitplan.app.data.repository

import com.fitplan.app.data.FitPlanDatabase
import com.fitplan.app.data.dao.ScheduledWithExercise
import com.fitplan.app.data.entity.BodyRecord
import com.fitplan.app.data.entity.Exercise
import com.fitplan.app.data.entity.ScheduledExercise
import kotlinx.coroutines.flow.Flow

/**
 * 统一的数据访问门面，屏蔽 DAO 细节，供 ViewModel 调用。
 */
class FitRepository(private val db: FitPlanDatabase) {

    // ---------- 动作库 ----------
    fun observeExercises(): Flow<List<Exercise>> = db.exerciseDao().observeAll()
    suspend fun addExercise(exercise: Exercise) = db.exerciseDao().insert(exercise)
    suspend fun updateExercise(exercise: Exercise) = db.exerciseDao().update(exercise)
    suspend fun deleteExercise(id: Long) = db.exerciseDao().deleteById(id)

    // ---------- 计划（每日条目） ----------
    fun observeForDate(day: Long): Flow<List<ScheduledWithExercise>> =
        db.scheduledExerciseDao().observeForDate(day)

    fun observeAllScheduled(): Flow<List<ScheduledWithExercise>> =
        db.scheduledExerciseDao().observeAll()

    suspend fun addScheduled(item: ScheduledExercise) = db.scheduledExerciseDao().insert(item)
    suspend fun updateScheduled(item: ScheduledExercise) = db.scheduledExerciseDao().update(item)
    suspend fun deleteScheduled(id: Long) = db.scheduledExerciseDao().deleteById(id)
    suspend fun deleteForDate(day: Long) = db.scheduledExerciseDao().deleteForDate(day)
    suspend fun countIncomplete(day: Long): Int = db.scheduledExerciseDao().countIncomplete(day)
    suspend fun scheduledBefore(day: Long): List<ScheduledExercise> =
        db.scheduledExerciseDao().beforeDate(day)
    suspend fun scheduledOn(day: Long): List<ScheduledExercise> =
        db.scheduledExerciseDao().forDate(day)
    suspend fun setScheduledCompleted(id: Long, done: Boolean) =
        db.scheduledExerciseDao().setCompleted(id, done)
    suspend fun updateScheduledParams(id: Long, sets: Int, reps: Int, weight: Double?, rest: Int) =
        db.scheduledExerciseDao().updateParams(id, sets, reps, weight, rest)

    // ---------- 身体参数 ----------
    fun observeBodyAscending(): Flow<List<BodyRecord>> = db.bodyRecordDao().observeAscending()
    fun observeBodyAll(): Flow<List<BodyRecord>> = db.bodyRecordDao().observeAll()
    suspend fun addBodyRecord(record: BodyRecord) = db.bodyRecordDao().insert(record)
    suspend fun updateBodyRecord(record: BodyRecord) = db.bodyRecordDao().update(record)
    suspend fun deleteBodyRecord(record: BodyRecord) = db.bodyRecordDao().delete(record)
    suspend fun latestBodyTwo(): List<BodyRecord> = db.bodyRecordDao().latestTwo()
}
