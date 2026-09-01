package com.real.businessman.database

import android.content.Context
import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.*

class FirebaseExcelImporter(private val repository: FirebaseRepository) {
    private val db = FirebaseFirestore.getInstance()

    suspend fun parseExcelTransactions(
        context: Context,
        uri: Uri,
        transactionType: String
    ): Result<List<Transaction>> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Не удалось открыть файл"))

            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheetAt(0)
            val transactions = mutableListOf<Transaction>()

            // Индексы колонок (по умолчанию -1)
            var colProduct = -1
            var colPrice = -1
            var colQty = -1
            var colTotal = -1
            var colDate = -1

            val headerRow = sheet.getRow(0) ?: return@withContext Result.failure(Exception("Пустой файл"))

            for (cell in headerRow) {
                val title = cell.stringCellValue.trim().lowercase()
                when {
                    title.contains("вид товара") || title.contains("товар") || title.contains("материал") || title.contains("наименование") -> colProduct = cell.columnIndex
                    title.contains("цена") || title.contains("себестоимост") || title.contains("закуп") -> colPrice = cell.columnIndex
                    title.contains("количест") || title.contains("кол-во") || title.contains("объем") -> colQty = cell.columnIndex
                    title.contains("сумма") || title.contains("итого") || title.contains("стоимост") -> colTotal = cell.columnIndex
                    title.contains("дата") -> colDate = cell.columnIndex
                }
            }

            // Проходим по строкам файла начиная со 2-й (индекс 1)
            for (rowIndex in 1..sheet.lastRowNum) {
                val row = sheet.getRow(rowIndex) ?: continue

                val productName = row.getCell(colProduct)?.toString()?.trim() ?: ""
                if (productName.isBlank()) continue

                val price = row.getCell(colPrice)?.numericCellValue ?: 0.0
                val quantity = row.getCell(colQty)?.numericCellValue ?: 1.0
                val total = if (colTotal != -1) row.getCell(colTotal)?.numericCellValue ?: (price * quantity) else (price * quantity)
                val date = row.getCell(colDate)?.toString()?.trim() ?: ""

                // Проверяем, существует ли товар в Firebase, и если нет — создаем его в коллекции products
                val existingProductsSnapshot = db.collection("products")
                    .whereEqualTo("name", productName)
                    .get().await()

                val productId: String = if (!existingProductsSnapshot.isEmpty) {
                    existingProductsSnapshot.documents[0].id
                } else {
                    val productType = if (transactionType == "SALE") "PRODUCT" else "MATERIAL"
                    val newProduct = Product(
                        name = productName,
                        type = productType,
                    )
                    repository.addProduct(newProduct)
                }

                val item = TransactionItem(
                    productId = productId,
                    productName = productName,
                    quantity = quantity,
                    pricePerUnit = price
                )

                val transaction = Transaction(
                    type = transactionType,
                    totalAmount = total,
                    date = date,
                    comment = "Импорт из Excel",
                    items = listOf(item)
                )

                transactions.add(transaction)
            }

            workbook.close()
            inputStream.close()
            Result.success(transactions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getCellValueAsString(cell: Cell?): String {
        if (cell == null) return ""
        return when (cell.cellType) {
            CellType.STRING -> cell.stringCellValue
            CellType.NUMERIC -> cell.numericCellValue.toLong().toString()
            else -> ""
        }
    }

    private fun getCellValueAsDouble(cell: Cell?): Double {
        if (cell == null) return 0.0
        return when (cell.cellType) {
            CellType.NUMERIC -> cell.numericCellValue
            CellType.STRING -> cell.stringCellValue.replace(",", ".").toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
    }
}