package com.fitplan.app.data.workout

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    @Insert
    suspend fun insertSession(session: WorkoutSession): Long

    @Update
    suspend fun updateSession(session: WorkoutSession)

    @Query("SELECT * FROM workout_sessions ORDER BY startedAt DESC")
    fun observeSessions(): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions WHERE id = :id LIMIT 1")
    suspend fun byId(id: Long): WorkoutSession?

    @Query("DELETE FROM workout_sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Insert
    suspend fun insertSets(sets: List<WorkoutSet>)

    @Insert
    suspend fun insertSet(set: WorkoutSet): Long

    @Query("SELECT * FROM workout_sets WHERE sessionId = :sessionId ORDER BY id ASC")
    fun observeSets(sessionId: Long): Flow<List<WorkoutSet>>

    @Query("SELECT * FROM workout_sets WHERE sessionId = :sessionId ORDER BY id ASC")
    suspend fun setsOf(sessionId: Long): List<WorkoutSet>
}
