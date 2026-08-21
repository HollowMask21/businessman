package com.real.businessman

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.real.businessman.database.ProductDao
import com.real.businessman.database.ProductEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProductViewModel(private val productDao: ProductDao) : ViewModel() {

    // Автоматически обновляемый поток списка товаров/материалов
    val products: StateFlow<List<ProductEntity>> = productDao.getAllProducts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addProduct(name: String, type: String) {
        viewModelScope.launch {
            productDao.insertProduct(ProductEntity(name = name, type = type))
        }
    }
}