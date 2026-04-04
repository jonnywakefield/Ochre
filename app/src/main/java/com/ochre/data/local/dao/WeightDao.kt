package com.ochre.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ochre.data.local.entity.WeightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: WeightEntity)

    @Query("SELECT * FROM weight_entries ORDER BY timestampMillis DESC")
    fun getAll(): Flow<List<WeightEntity>>

    @Query("DELETE FROM weight_entries WHERE id = :id")
    suspend fun deleteById(id: Long)
}
