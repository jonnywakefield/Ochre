package com.ochre.data.local.dao

import androidx.room.*
import com.ochre.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(reminder: ReminderEntity): Long

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM reminders ORDER BY timestampMillis ASC")
    fun getAll(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE timestampMillis BETWEEN :from AND :to ORDER BY timestampMillis ASC")
    fun getInRange(from: Long, to: Long): Flow<List<ReminderEntity>>
}
