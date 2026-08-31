package com.real.businessman.database

data class ConflictItem(
    val productName: String,
    val date: String,
    val oldPrice: Double,
    val newPrice: Double,
    val oldQuantity: Double,
    val newQuantity: Double,
    val type: String
)

data class ImportPreviewState(
    val newTransactions: List<Transaction>,
    val conflicts: List<ConflictItem>
)