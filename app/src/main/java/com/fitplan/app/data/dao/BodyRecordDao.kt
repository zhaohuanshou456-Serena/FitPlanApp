package com.fitplan.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.fitplan.app.data.entity.BodyRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyRecordDao {

    @Query("SELECT * FROM body_records ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<BodyRecord>>

    @Query("SELECT * FROM body_records ORDER BY timestamp ASC")
    fun observeAscending(): Flow<List<BodyRecord>>

    @Query("SELECT * FROM body_records WHERE timestamp BETWEEN :from AND :to ORDER BY timestamp DESC")
    fun observeBetween(from: Long, to: Long): Flow<List<BodyRecord>>

    @Insert
    suspend fun insert(record: BodyRecord): Long

    @Update
    suspend fun update(record: BodyRecord)

    @Delete
    suspend fun delete(record: BodyRecord)

    @Query("SELECT * FROM body_records ORDER BY timestamp DESC LIMIT 2")
    suspend fun latestTwo(): List<BodyRecord>

    @Query("SELECT * FROM body_records ORDER BY timestamp ASC")
    suspend fun all(): List<BodyRecord>

    @Insert
    suspend fun insertAll(list: List<BodyRecord>)

    @Query("DELETE FROM body_records")
    suspend fun clearAll()
}
