package com.ochre.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ochre.data.local.entity.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity)

    @Update
    suspend fun updateEvent(event: EventEntity)

    @Delete
    suspend fun deleteEvent(event: EventEntity)

    @Query("SELECT * FROM events ORDER BY timestampMillis DESC")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE eventType = :type ORDER BY timestampMillis DESC")
    fun getEventsByType(type: String): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE timestampMillis BETWEEN :from AND :to ORDER BY timestampMillis DESC")
    fun getEventsInRange(from: Long, to: Long): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE eventType = :type ORDER BY timestampMillis DESC LIMIT 1")
    suspend fun getLastEventOfType(type: String): EventEntity?
}
