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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TransactionViewModel(
    private val repository: FirebaseRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

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
            try {
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
                    comment = comment.trim(),
                    items = listOf(item)
                )
                repository.addTransaction(transaction)
                _statusMessage.value = "Операция успешно добавлена"
            } catch (e: Exception) {
                _statusMessage.value = "Ошибка при добавлении: ${e.localizedMessage}"
            }
        }
    }

    // Редактирование существующей операции
    fun updateManualTransaction(
        id: String,
        type: String,
        date: String,
        productName: String,
        quantity: Double,
        pricePerUnit: Double,
        comment: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val total = quantity * pricePerUnit
                val item = TransactionItem(
                    productName = productName,
                    quantity = quantity,
                    pricePerUnit = pricePerUnit
                )
                val transaction = Transaction(
                    id = id,
                    type = type,
                    totalAmount = total,
                    date = date,
                    comment = comment.trim(),
                    items = listOf(item)
                )
                repository.updateTransaction(transaction)
                _statusMessage.value = "Операция успешно обновлена"
            } catch (e: Exception) {
                _statusMessage.value = "Ошибка при изменении: ${e.localizedMessage}"
            }
        }
    }

    // Удаление одиночной операции
    fun deleteTransaction(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteTransaction(id)
                _statusMessage.value = "Операция успешно удалена"
            } catch (e: Exception) {
                _statusMessage.value = "Ошибка при удалении: ${e.localizedMessage}"
            }
        }
    }

    // Множественное удаление операций
    fun deleteSelectedTransactions(ids: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val count = ids.size
                ids.forEach { repository.deleteTransaction(it) }
                _statusMessage.value = "Успешно удалено операций: $count"
            } catch (e: Exception) {
                _statusMessage.value = "Ошибка при удалении: ${e.localizedMessage}"
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}