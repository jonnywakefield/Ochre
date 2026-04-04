package com.ochre.domain.model

data class AloneSession(
    val id: Long = 0,
    val startMillis: Long,
    val endMillis: Long? = null
) {
    val isActive: Boolean get() = endMillis == null
    val durationMillis: Long? get() = endMillis?.let { it - startMillis }
}
