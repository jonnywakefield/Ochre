package com.ochre.data.local.dao

import androidx.room.*
import com.ochre.data.local.entity.AloneSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AloneDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: AloneSessionEntity): Long

    @Update
    suspend fun updateSession(session: AloneSessionEntity)

    @Query("DELETE FROM alone_sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Query("SELECT * FROM alone_sessions WHERE endMillis IS NULL LIMIT 1")
    fun getActiveSession(): Flow<AloneSessionEntity?>

    @Query("SELECT * FROM alone_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): AloneSessionEntity?

    @Query("SELECT * FROM alone_sessions ORDER BY startMillis DESC")
    fun getAllSessions(): Flow<List<AloneSessionEntity>>
}
