package com.ochre.presentation.medical

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ochre.domain.model.WeightEntry
import com.ochre.domain.usecase.weight.DeleteWeightUseCase
import com.ochre.domain.usecase.weight.GetWeightHistoryUseCase
import com.ochre.domain.usecase.weight.LogWeightUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MedicalUiState(
    val entries: List<WeightEntry> = emptyList()
)

class MedicalViewModel(
    private val logWeightUseCase: LogWeightUseCase,
    private val getWeightHistoryUseCase: GetWeightHistoryUseCase,
    private val deleteWeightUseCase: DeleteWeightUseCase
) : ViewModel() {

    val uiState: StateFlow<MedicalUiState> = getWeightHistoryUseCase()
        .map { MedicalUiState(entries = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MedicalUiState())

    fun logWeight(weightKg: Float, timestampMillis: Long, note: String = "") {
        viewModelScope.launch {
            logWeightUseCase(weightKg, timestampMillis, note)
        }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch { deleteWeightUseCase(id) }
    }

    companion object {
        fun provideFactory(
            logWeightUseCase: LogWeightUseCase,
            getWeightHistoryUseCase: GetWeightHistoryUseCase,
            deleteWeightUseCase: DeleteWeightUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                MedicalViewModel(logWeightUseCase, getWeightHistoryUseCase, deleteWeightUseCase) as T
        }
    }
}
