package com.verdant.feature.finance.create

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verdant.core.model.SpendingCategory
import com.verdant.core.model.Transaction
import com.verdant.core.model.TransactionType
import com.verdant.core.model.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class TransactionDraft(
    val amount: String = "",
    val type: TransactionType = TransactionType.DEBIT,
    val merchant: String = "",
    val category: String = SpendingCategory.OTHER.name,
    val transactionDate: Long = System.currentTimeMillis(),
    val notes: String = "",
)

data class TransactionCreateUiState(
    val draft: TransactionDraft = TransactionDraft(),
    val isEditMode: Boolean = false,
    val amountError: String? = null,
    val isSaving: Boolean = false,
)

@HiltViewModel
class TransactionCreateViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val editTransactionId: String? = savedStateHandle["transactionId"]

    private val _uiState = MutableStateFlow(TransactionCreateUiState())
    val uiState: StateFlow<TransactionCreateUiState> = _uiState.asStateFlow()

    init {
        if (editTransactionId != null) {
            loadExistingTransaction(editTransactionId)
        }
    }

    private fun loadExistingTransaction(id: String) = viewModelScope.launch {
        val txn = transactionRepository.getById(id) ?: return@launch
        _uiState.update {
            it.copy(
                isEditMode = true,
                draft = TransactionDraft(
                    amount = txn.amount.toString(),
                    type = txn.type,
                    merchant = txn.merchant ?: "",
                    category = txn.category ?: SpendingCategory.OTHER.name,
                    transactionDate = txn.transactionDate,
                    notes = "",
                ),
            )
        }
    }

    fun updateAmount(value: String) {
        _uiState.update { it.copy(draft = it.draft.copy(amount = value), amountError = null) }
    }

    fun updateType(type: TransactionType) {
        _uiState.update { it.copy(draft = it.draft.copy(type = type)) }
    }

    fun updateMerchant(value: String) {
        _uiState.update { it.copy(draft = it.draft.copy(merchant = value)) }
    }

    fun updateCategory(value: String) {
        _uiState.update { it.copy(draft = it.draft.copy(category = value)) }
    }

    fun updateDate(millis: Long) {
        _uiState.update { it.copy(draft = it.draft.copy(transactionDate = millis)) }
    }

    fun updateNotes(value: String) {
        _uiState.update { it.copy(draft = it.draft.copy(notes = value)) }
    }

    fun save(onSuccess: () -> Unit) {
        val draft = _uiState.value.draft
        val amount = draft.amount.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _uiState.update { it.copy(amountError = "Enter a valid amount") }
            return
        }

        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val transaction = Transaction(
                id = editTransactionId ?: UUID.randomUUID().toString(),
                amount = amount,
                type = draft.type,
                merchant = draft.merchant.ifBlank { null },
                category = draft.category,
                subCategory = null,
                accountTail = null,
                bank = null,
                upiId = null,
                balanceAfter = null,
                transactionDate = draft.transactionDate,
                rawSmsId = null,
                rawSmsBody = null,
                isRecurring = false,
                parseConfidence = 1f,
                userVerified = true,
                createdAt = System.currentTimeMillis(),
            )

            if (editTransactionId != null) {
                transactionRepository.update(transaction)
            } else {
                transactionRepository.insert(transaction)
            }

            _uiState.update { it.copy(isSaving = false) }
            onSuccess()
        }
    }
}
