package com.ochre.domain.model

data class WalkSession(
    val id: Long = 0,
    val startMillis: Long,
    val endMillis: Long? = null,
    val pooEvents: List<Long> = emptyList(),
    val peeEvents: List<Long> = emptyList()
) {
    val isActive: Boolean get() = endMillis == null
    val durationMillis: Long? get() = endMillis?.let { it - startMillis }
}
