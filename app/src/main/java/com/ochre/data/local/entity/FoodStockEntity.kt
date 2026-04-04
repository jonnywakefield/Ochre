package com.ochre.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ochre.domain.model.FoodStockEntry

@Entity(tableName = "food_stock")
data class FoodStockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMillis: Long,
    val deltaGrams: Int
)

fun FoodStockEntity.toDomain() = FoodStockEntry(id = id, timestampMillis = timestampMillis, deltaGrams = deltaGrams)
fun FoodStockEntry.toEntity() = FoodStockEntity(id = id, timestampMillis = timestampMillis, deltaGrams = deltaGrams)
