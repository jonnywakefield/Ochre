package com.ochre.data.local.dao

import androidx.room.*
import com.ochre.data.local.entity.WalkScheduleConfigEntity
import com.ochre.data.local.entity.WalkScheduleEntryEntity
import com.ochre.data.local.entity.WalkSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WalkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WalkSessionEntity): Long

    @Update
    suspend fun updateSession(session: WalkSessionEntity)

    @Query("DELETE FROM walk_sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Query("SELECT * FROM walk_sessions WHERE endMillis IS NULL LIMIT 1")
    fun getActiveSession(): Flow<WalkSessionEntity?>

    @Query("SELECT * FROM walk_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): WalkSessionEntity?

    @Query("SELECT * FROM walk_sessions ORDER BY startMillis DESC")
    fun getAllSessions(): Flow<List<WalkSessionEntity>>

    // Schedule entries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleEntry(entry: WalkScheduleEntryEntity): Long

    @Delete
    suspend fun deleteScheduleEntry(entry: WalkScheduleEntryEntity)

    @Query("DELETE FROM walk_schedule_entries")
    suspend fun deleteAllScheduleEntries()

    @Query("SELECT * FROM walk_schedule_entries ORDER BY targetHour ASC, targetMinute ASC")
    fun getAllScheduleEntries(): Flow<List<WalkScheduleEntryEntity>>

    // Schedule config (single row)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConfig(config: WalkScheduleConfigEntity)

    @Query("SELECT * FROM walk_schedule_config WHERE id = 1")
    fun getConfig(): Flow<WalkScheduleConfigEntity?>
}
