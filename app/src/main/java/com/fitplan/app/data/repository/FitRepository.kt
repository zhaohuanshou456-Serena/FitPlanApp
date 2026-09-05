package com.fitplan.app.data.repository

import androidx.room.withTransaction
import com.fitplan.app.data.ExerciseCueSeed
import com.fitplan.app.data.FitPlanDatabase
import com.fitplan.app.data.dao.ScheduledWithExercise
import com.fitplan.app.data.entity.BodyRecord
import com.fitplan.app.data.entity.Exercise
import com.fitplan.app.data.entity.ScheduledExercise
import com.fitplan.app.data.program.ExerciseProgression
import com.fitplan.app.data.program.Program
import com.fitplan.app.data.program.ProgramDayApply
import com.fitplan.app.data.program.ProgramItem
import com.fitplan.app.data.program.ProgramJson
import com.fitplan.app.data.program.ProgramSeeder
import com.fitplan.app.data.program.ProgramSession
import com.fitplan.app.data.workout.WorkoutSession
import com.fitplan.app.data.workout.WorkoutSet
import kotlinx.coroutines.flow.Flow

/** 方案轮换建议：下一个应练的节 */
data class SessionSuggestion(
    val programId: Long,
    val sessionId: Long,
    val programName: String,
    val sessionName: String
)

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

    // ---------- 训练方案（可复用模板） ----------
    fun observePrograms(): Flow<List<Program>> = db.programDao().observeAll()

    suspend fun insertProgram(program: Program): Long = db.programDao().insert(program)
    suspend fun deleteProgram(id: Long) = db.programDao().deleteById(id)

    fun observeProgramSessions(programId: Long): Flow<List<ProgramSession>> =
        db.programSessionDao().observeSessions(programId)

    suspend fun sessionsOf(programId: Long): List<ProgramSession> =
        db.programSessionDao().sessionsOf(programId)

    suspend fun insertSession(session: ProgramSession): Long =
        db.programSessionDao().insert(session)

    suspend fun deleteSession(id: Long) = db.programSessionDao().deleteById(id)

    /** 重命名一节（或改节名） */
    suspend fun renameSession(id: Long, newName: String) {
        val s = db.programSessionDao().byId(id) ?: return
        db.programSessionDao().update(s.copy(name = newName))
    }

    suspend fun itemsOf(sessionId: Long): List<ProgramItem> =
        db.programItemDao().itemsOf(sessionId)

    fun observeProgramItems(sessionId: Long): Flow<List<ProgramItem>> =
        db.programItemDao().observeItems(sessionId)

    suspend fun insertProgramItem(item: ProgramItem): Long =
        db.programItemDao().insert(item)

    suspend fun insertProgramItems(items: List<ProgramItem>) =
        db.programItemDao().insertAll(items)

    suspend fun deleteProgramItem(id: Long) = db.programItemDao().deleteById(id)

    /** 把方案的某一节应用到某一天：清空当天→按节生成当天计划条目→记录映射。 */
    suspend fun applyProgramSession(day: Long, programId: Long, sessionId: Long) {
        db.withTransaction {
            db.scheduledExerciseDao().deleteForDate(day)
            db.programDayApplyDao().removeByDate(day)
            val items = db.programItemDao().itemsOf(sessionId)
            items.forEachIndexed { i, it ->
                val cue = buildString {
                    it.formCue?.let { append(it) }
                    if (it.repMin > 0 && it.repMax > 0) {
                        if (isNotEmpty()) append("；")
                        append("目标 ${it.repMin}–${it.repMax} 次")
                    }
                }
                db.scheduledExerciseDao().insert(
                    ScheduledExercise(
                        dateEpochDay = day,
                        exerciseId = it.exerciseId,
                        targetSets = it.targetSets,
                        targetReps = it.repMax,
                        weight = it.weightKg,
                        restSeconds = it.restSeconds,
                        sortOrder = i,
                        actualNote = cue.ifBlank { null }
                    )
                )
            }
            db.programDayApplyDao().upsert(
                ProgramDayApply(dateEpochDay = day, programId = programId, sessionId = sessionId)
            )
        }
    }

    suspend fun appliedProgramOn(day: Long): ProgramDayApply? =
        db.programDayApplyDao().byDate(day)

    /** 取消某天已应用的方案：只清当天计划与映射，方案模板本身不动。 */
    suspend fun removeAppliedProgram(day: Long) {
        db.withTransaction {
            db.scheduledExerciseDao().deleteForDate(day)
            db.programDayApplyDao().removeByDate(day)
        }
    }

    // ---------- 渐进基线 ----------
    suspend fun progressionOf(exerciseId: Long): ExerciseProgression? =
        db.progressionDao().byExercise(exerciseId)

    suspend fun saveProgression(p: ExerciseProgression) =
        db.progressionDao().upsert(p)

    // ---------- 开始训练 / 组记录 ----------
    fun observeWorkoutSessions(): Flow<List<WorkoutSession>> =
        db.workoutDao().observeSessions()

    suspend fun beginWorkout(session: WorkoutSession): Long =
        db.workoutDao().insertSession(session)

    suspend fun endWorkout(id: Long) {
        val s = db.workoutDao().byId(id) ?: return
        db.workoutDao().updateSession(s.copy(endedAt = System.currentTimeMillis()))
    }

    suspend fun logWorkoutSet(set: WorkoutSet) = db.workoutDao().insertSet(set)

    suspend fun setsOfWorkout(sessionId: Long): List<WorkoutSet> =
        db.workoutDao().setsOf(sessionId)

    fun observeWorkoutSets(sessionId: Long): Flow<List<WorkoutSet>> =
        db.workoutDao().observeSets(sessionId)

    suspend fun deleteWorkout(id: Long) = db.workoutDao().deleteSession(id)

    // ---------- 训练方案（种子 + 轮换建议） ----------
    /** 首次确保内置 U/L 方案被写入（仅当无任何方案时） */
    suspend fun ensureBuiltInPlan(): Boolean = ProgramSeeder.ensure(db)

    suspend fun programsAll(): List<Program> = db.programDao().allOrdered()

    /** 首次把“动作要领+易错点”内容库动作写入动作库（幂等） */
    suspend fun ensureCueExercises(): Int = ExerciseCueSeed.ensure(db)

    suspend fun exportProgramsJson(): String = ProgramJson.export(db)

    suspend fun importProgramsJson(json: String): Int = ProgramJson.import(db, json)

    suspend fun exerciseById(id: Long): Exercise? = db.exerciseDao().byId(id)

    /** 依据 A→B→C→D 轮换给出“今天(day)该练哪一节”的建议；优先用 activeProgramId，否则用第一个方案。 */
    suspend fun nextSuggestedSession(day: Long, activeProgramId: Long?): SessionSuggestion? {
        val programs = db.programDao().allOrdered()
        val prog = programs.firstOrNull { it.id == activeProgramId } ?: programs.firstOrNull() ?: return null
        val sessions = db.programSessionDao().sessionsOf(prog.id)
        if (sessions.isEmpty()) return null
        val last = db.programDayApplyDao().latestBefore(prog.id, day)
        var idx = 0
        if (last != null) {
            val li = sessions.indexOfFirst { it.id == last.sessionId }
            if (li in sessions.indices) idx = (li + 1) % sessions.size
        }
        val s = sessions[idx]
        return SessionSuggestion(prog.id, s.id, prog.name, s.name)
    }
}
