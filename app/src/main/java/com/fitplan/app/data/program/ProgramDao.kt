package com.fitplan.app.data.program

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgramDao {
    @Query("SELECT * FROM programs ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Program>>

    @Query("SELECT * FROM programs WHERE id = :id")
    suspend fun byId(id: Long): Program?

    @Query("SELECT * FROM programs ORDER BY createdAt ASC")
    suspend fun allOrdered(): List<Program>

    @Insert
    suspend fun insert(program: Program): Long

    @Update
    suspend fun update(program: Program)

    @Query("DELETE FROM programs WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface ProgramSessionDao {
    @Query("SELECT * FROM program_sessions WHERE programId = :programId ORDER BY sessionOrder ASC")
    fun observeSessions(programId: Long): Flow<List<ProgramSession>>

    @Query("SELECT * FROM program_sessions WHERE id = :id")
    suspend fun byId(id: Long): ProgramSession?

    @Insert
    suspend fun insert(session: ProgramSession): Long

    @Update
    suspend fun update(session: ProgramSession)

    @Query("DELETE FROM program_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM program_sessions WHERE programId = :programId ORDER BY sessionOrder ASC")
    suspend fun sessionsOf(programId: Long): List<ProgramSession>
}

@Dao
interface ProgramItemDao {
    @Query("SELECT * FROM program_items WHERE sessionId = :sessionId ORDER BY itemOrder ASC")
    fun observeItems(sessionId: Long): Flow<List<ProgramItem>>

    @Query("SELECT * FROM program_items WHERE sessionId = :sessionId ORDER BY itemOrder ASC")
    suspend fun itemsOf(sessionId: Long): List<ProgramItem>

    @Insert
    suspend fun insert(item: ProgramItem): Long

    @Insert
    suspend fun insertAll(items: List<ProgramItem>)

    @Update
    suspend fun update(item: ProgramItem)

    @Query("DELETE FROM program_items WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface ProgramDayApplyDao {
    @Query("SELECT * FROM program_day_apply WHERE dateEpochDay = :day LIMIT 1")
    suspend fun byDate(day: Long): ProgramDayApply?

    @Query("SELECT * FROM program_day_apply WHERE programId = :programId AND dateEpochDay < :day ORDER BY dateEpochDay DESC LIMIT 1")
    suspend fun latestBefore(programId: Long, day: Long): ProgramDayApply?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(apply: ProgramDayApply)

    @Query("DELETE FROM program_day_apply WHERE dateEpochDay = :day")
    suspend fun removeByDate(day: Long)

    @Query("DELETE FROM program_day_apply")
    suspend fun clearAll()
}

@Dao
interface ProgressionDao {
    @Query("SELECT * FROM exercise_progression WHERE exerciseId = :exerciseId LIMIT 1")
    suspend fun byExercise(exerciseId: Long): ExerciseProgression?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progression: ExerciseProgression)

    @Query("DELETE FROM exercise_progression")
    suspend fun clearAll()
}
