package com.ochre.domain.model

data class FoodStockEntry(
    val id: Long = 0,
    val timestampMillis: Long,
    val deltaGrams: Int  // positive = stock added, negative = consumed (auto-logged on feed)
)
