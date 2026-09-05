package com.fitplan.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.fitplan.app.data.entity.Exercise
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Query("SELECT * FROM exercises ORDER BY muscleGroup ASC, name ASC")
    fun observeAll(): Flow<List<Exercise>>

    @Query("SELECT COUNT(*) FROM exercises WHERE name = :name")
    suspend fun countByName(name: String): Int

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun byId(id: Long): Exercise?

    @Insert
    suspend fun insertAll(list: List<Exercise>): List<Long>

    @Insert
    suspend fun insert(exercise: Exercise): Long

    @Update
    suspend fun update(exercise: Exercise)

    @Query("DELETE FROM exercises WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM exercises")
    suspend fun all(): List<Exercise>

    @Query("DELETE FROM exercises")
    suspend fun clearAll()

    @Query("SELECT id FROM exercises WHERE name = :name LIMIT 1")
    suspend fun idByName(name: String): Long?
}
