package com.ochre.data.local.dao

import androidx.room.*
import com.ochre.data.local.entity.FoodStockEntity
import com.ochre.data.local.entity.MealScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    // Meal schedule
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMeal(meal: MealScheduleEntity): Long

    @Query("DELETE FROM meal_schedule WHERE id = :id")
    suspend fun deleteMeal(id: Long)

    @Query("SELECT * FROM meal_schedule ORDER BY targetHour ASC, targetMinute ASC")
    fun getAllMeals(): Flow<List<MealScheduleEntity>>

    // Stock
    @Insert
    suspend fun insertStockEntry(entry: FoodStockEntity)

    @Query("SELECT * FROM food_stock ORDER BY timestampMillis DESC")
    fun getAllStockEntries(): Flow<List<FoodStockEntity>>

    @Query("SELECT COALESCE(SUM(deltaGrams), 0) FROM food_stock")
    fun getCurrentStockGrams(): Flow<Int>
}
