package com.real.businessman.database

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseRepository {
    private val db = FirebaseFirestore.getInstance()

    // 1. Включение работы в офлайн режиме (включается автоматически в Firebase SDK)
    init {
        val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true) // Сохранять данные локально, если нет интернета
            .build()
        db.firestoreSettings = settings
    }

    // 2. Получение списка товаров в реальном времени (Realtime Flow)
    fun getProductsFlow(): Flow<List<Product>> = callbackFlow {
        val listener = db.collection("products")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val products = snapshot?.toObjects(Product::class.java) ?: emptyList()
                trySend(products)
            }
        awaitClose { listener.remove() }
    }

    // Получение списка всех транзакций в реальном времени
    fun getTransactionsFlow(): Flow<List<Transaction>> = callbackFlow {
        val listener = db.collection("transactions")
            .orderBy("date", Query.Direction.DESCENDING) // Сначала свежие
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val transactions = snapshot?.toObjects(Transaction::class.java) ?: emptyList()
                trySend(transactions)
            }
        awaitClose { listener.remove() }
    }

    // 3. Добавление товара
    suspend fun addProduct(product: Product): String {
        val docRef = db.collection("products").add(product).await()
        return docRef.id
    }

    // 4. Добавление транзакции (Продажа/Расход)
    suspend fun addTransaction(transaction: Transaction) {
        db.collection("transactions").add(transaction).await()
    }
    // Внутри FirebaseRepository.kt

    suspend fun checkConflictsAndPrepareImport(
        parsedTransactions: List<Transaction>
    ): ImportPreviewState {
        val existingSnapshot = db.collection("transactions").get().await()
        val existingTransactions = existingSnapshot.toObjects(Transaction::class.java)

        val conflicts = mutableListOf<ConflictItem>()
        val nonConflictingTransactions = mutableListOf<Transaction>()

        for (newTx in parsedTransactions) {
            val newItem = newTx.items.firstOrNull()

            // Ищем существующую транзакцию с ТЕМ ЖЕ товаром на ТУ ЖЕ дату
            val existingMatch = existingTransactions.find { existing ->
                val existingItem = existing.items.firstOrNull()
                existing.date == newTx.date &&
                        existing.type == newTx.type &&
                        existingItem?.productName == newItem?.productName
            }

            if (existingMatch != null) {
                val oldItem = existingMatch.items.firstOrNull()

                if (oldItem != null && newItem != null && (oldItem.pricePerUnit != newItem.pricePerUnit || oldItem.quantity != newItem.quantity)) {
                    conflicts.add(
                        ConflictItem(
                            documentId = existingMatch.id,
                            productName = newItem.productName,
                            oldPrice = oldItem.pricePerUnit,
                            newPrice = newItem.pricePerUnit,
                            oldQuantity = oldItem.quantity,
                            newQuantity = newItem.quantity,
                            date = newTx.date,
                            type = newTx.type
                        )
                    )
                }
            } else {
                nonConflictingTransactions.add(newTx)
            }
        }

        return ImportPreviewState(
            newTransactions = nonConflictingTransactions,
            conflicts = conflicts
        )
    }
    // Сохранение списка транзакций (используется при импорте)
    suspend fun saveTransactions(transactions: List<Transaction>) {
        val batch = db.batch()
        for (transaction in transactions) {
            val docRef = db.collection("transactions").document()
            batch.set(docRef, transaction)
        }
        batch.commit().await()
    }
    // Обновление конфликтных записей и сохранение новых за один батч
    suspend fun resolveConflictsAndSave(
        newTransactions: List<Transaction>,
        conflictsToUpdate: List<ConflictItem>
    ) {
        val batch = db.batch()

        // 1. Новые транзакции создаем как новые документы
        for (transaction in newTransactions) {
            val docRef = db.collection("transactions").document()
            batch.set(docRef, transaction)
        }

        // 2. Существующие конфликтные записи перезаписываем по их реальному documentId
        for (conflict in conflictsToUpdate) {
            val docRef = db.collection("transactions").document(conflict.documentId)

            val updatedTransaction = Transaction(
                type = conflict.type,
                totalAmount = conflict.newPrice * conflict.newQuantity,
                date = conflict.date,
                comment = "Импорт из Excel (обновлено)",
                items = listOf(
                    TransactionItem(
                        productName = conflict.productName,
                        quantity = conflict.newQuantity,
                        pricePerUnit = conflict.newPrice
                    )
                )
            )
            batch.set(docRef, updatedTransaction)
        }

        batch.commit().await()
    }
}