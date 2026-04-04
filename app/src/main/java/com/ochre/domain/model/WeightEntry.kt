package com.ochre.domain.model

data class WeightEntry(
    val id: Long = 0,
    val timestampMillis: Long,
    val weightKg: Float,
    val note: String = ""
)
