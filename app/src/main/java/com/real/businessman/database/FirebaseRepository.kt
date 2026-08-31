package com.real.businessman.database

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseRepository {
    private val db = FirebaseFirestore.getInstance()

    // 1. Включение работы в оффлайн режиме (включается автоматически в Firebase SDK)
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

    // 3. Добавление товара
    suspend fun addProduct(product: Product): String {
        val docRef = db.collection("products").add(product).await()
        return docRef.id
    }

    // 4. Добавление транзакции (Продажа/Расход)
    suspend fun addTransaction(transaction: Transaction) {
        db.collection("transactions").add(transaction).await()
    }
}