package com.real.businessman.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.real.businessman.database.FirebaseRepository
import com.real.businessman.database.Transaction
import com.real.businessman.database.TransactionItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TransactionViewModel(
    private val repository: FirebaseRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading
    // Подписываемся на поток транзакций из Firebase
    val transactions: StateFlow<List<Transaction>> = repository.getTransactionsFlow()
        .onEach {
            _isLoading.value = false
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Добавление операции вручную
    fun addManualTransaction(
        type: String,
        date: String,
        productName: String,
        quantity: Double,
        pricePerUnit: Double,
        comment: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val total = quantity * pricePerUnit
            val item = TransactionItem(
                productName = productName,
                quantity = quantity,
                pricePerUnit = pricePerUnit
            )
            val transaction = Transaction(
                type = type,
                totalAmount = total,
                date = date,
                comment = comment.trim(), // Если строка пустая, сохранится пустой комментарий ""
                items = listOf(item)
            )
            repository.addTransaction(transaction)
        }
    }
}