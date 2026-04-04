package com.ochre.domain.repository

import com.ochre.domain.model.DogEvent
import com.ochre.domain.model.FoodStockEntry
import com.ochre.domain.model.MealScheduleEntry
import kotlinx.coroutines.flow.Flow

interface FoodRepository {
    // Meal schedule
    fun getMealSchedule(): Flow<List<MealScheduleEntry>>
    suspend fun saveMeal(entry: MealScheduleEntry)
    suspend fun deleteMeal(entryId: Long)

    // Feed logging (stores as DogEvent internally but surface via this repo for food-specific queries)
    suspend fun logFeed(timestampMillis: Long, grams: Int, mealId: Long?)
    fun getFeedLog(): Flow<List<DogEvent>>

    // Stock
    fun getStockEntries(): Flow<List<FoodStockEntry>>
    suspend fun addStock(grams: Int, timestampMillis: Long)
    fun getCurrentStockGrams(): Flow<Int>
}
