package com.ochre.data.repository

import com.ochre.data.local.dao.EventDao
import com.ochre.data.local.dao.FoodDao
import com.ochre.data.local.entity.FoodStockEntity
import com.ochre.data.local.entity.toDomain
import com.ochre.data.local.entity.toDomainModel
import com.ochre.data.local.entity.toEntity
import com.ochre.domain.model.DogEvent
import com.ochre.domain.model.EventType
import com.ochre.domain.model.FoodStockEntry
import com.ochre.domain.model.MealScheduleEntry
import com.ochre.domain.repository.FoodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FoodRepositoryImpl(
    private val foodDao: FoodDao,
    private val eventDao: EventDao
) : FoodRepository {

    override fun getMealSchedule(): Flow<List<MealScheduleEntry>> =
        foodDao.getAllMeals().map { list -> list.map { it.toDomain() } }

    override suspend fun saveMeal(entry: MealScheduleEntry) {
        foodDao.upsertMeal(entry.toEntity())
    }

    override suspend fun deleteMeal(entryId: Long) {
        foodDao.deleteMeal(entryId)
    }

    override suspend fun logFeed(timestampMillis: Long, grams: Int, mealId: Long?) {
        // Store as a DogEvent so it appears in the unified event log / calendar
        val event = com.ochre.domain.model.DogEvent(
            type = EventType.FEED,
            timestampMillis = timestampMillis,
            value = grams.toFloat(),
            note = mealId?.let { "meal:$it" }
        )
        eventDao.insertEvent(event.toEntity())
        // Deduct from stock
        foodDao.insertStockEntry(FoodStockEntity(timestampMillis = timestampMillis, deltaGrams = -grams))
    }

    override fun getFeedLog(): Flow<List<DogEvent>> =
        eventDao.getEventsByType(EventType.FEED.name)
            .map { list -> list.mapNotNull { it.toDomainModel() } }

    override fun getStockEntries(): Flow<List<FoodStockEntry>> =
        foodDao.getAllStockEntries().map { list -> list.map { it.toDomain() } }

    override suspend fun addStock(grams: Int, timestampMillis: Long) {
        foodDao.insertStockEntry(FoodStockEntity(timestampMillis = timestampMillis, deltaGrams = grams))
    }

    override fun getCurrentStockGrams(): Flow<Int> = foodDao.getCurrentStockGrams()
}
