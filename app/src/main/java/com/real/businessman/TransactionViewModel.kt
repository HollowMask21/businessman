package com.real.businessman

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.real.businessman.database.FirebaseRepository
import com.real.businessman.database.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.onEach

class TransactionViewModel(
    private val repository: FirebaseRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading
    // Подписываемся на поток транзакций из Firebase
    val transactions: StateFlow<List<Transaction>> = repository.getTransactionsFlow()
        .onEach{
            _isLoading.value = false
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}