package com.verdant.feature.finance.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verdant.core.model.Transaction
import com.verdant.core.model.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionDetailUiState(
    val transaction: Transaction? = null,
    val notFound: Boolean = false,
)

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val transactionId: String = checkNotNull(savedStateHandle["transactionId"])

    private val _uiState = MutableStateFlow(TransactionDetailUiState())
    val uiState: StateFlow<TransactionDetailUiState> = _uiState.asStateFlow()

    init {
        loadTransaction()
    }

    private fun loadTransaction() = viewModelScope.launch {
        val txn = transactionRepository.getById(transactionId)
        _uiState.update {
            if (txn != null) it.copy(transaction = txn)
            else it.copy(notFound = true)
        }
    }

    fun delete(onComplete: () -> Unit) = viewModelScope.launch {
        _uiState.value.transaction?.let { txn ->
            transactionRepository.delete(txn)
        }
        onComplete()
    }
}
