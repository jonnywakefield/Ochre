package com.ochre.domain.model

/**
 * The core domain model representing any single action or logged event in the system.
 * This is the purest representation of data, disconnected from how it is stored (Room).
 */
data class DogEvent(
    val id: Long = 0,
    val type: EventType,
    val timestampMillis: Long,
    val value: Float? = null, // Can be used for weight (kg) or food amount (grams)
    val note: String? = null
)
