package com.real.businessman.database

import com.google.firebase.firestore.DocumentId

// Товар / Материал
data class Product(
    @DocumentId val id: String = "",
    val name: String = "",
    val type: String = "PRODUCT", // "PRODUCT" или "MATERIAL"
)

// Позиция внутри сделки
data class TransactionItem(
    val productId: String = "",
    val productName: String = "",
    val quantity: Double = 0.0,
    val pricePerUnit: Double = 0.0
)

// Сделка (Продажа / Расход)
data class Transaction(
    @DocumentId val id: String = "",
    val type: String = "SALE", // "SALE", "PURCHASE", "EXPENSE"
    val totalAmount: Double = 0.0,
    val date: String = "",
    val comment: String = "",
    val items: List<TransactionItem> = emptyList()
)