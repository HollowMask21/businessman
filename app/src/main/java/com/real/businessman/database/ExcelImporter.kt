package com.real.businessman.database

import android.content.Context
import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import org.apache.poi.ss.usermodel.*

class FirebaseExcelImporter(private val repository: FirebaseRepository) {
    private val db = FirebaseFirestore.getInstance()

    suspend fun importExcel(context: Context, uri: Uri, defaultType: String): Result<Int> {
        return runCatching {
            var importedCount = 0

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val workbook = WorkbookFactory.create(inputStream)
                val sheet = workbook.getSheetAt(0)
                val headerRow = sheet.getRow(0) ?: throw Exception("Файл пуст")

                var colProduct = -1
                var colPrice = -1
                var colQty = -1
                var colTotal = -1
                var colDate = -1

                for (cell in headerRow) {
                    val title = cell.stringCellValue.trim().lowercase()
                    when {
                        title.contains("вид товара") || title.contains("товар") -> colProduct = cell.columnIndex
                        title.contains("цена") -> colPrice = cell.columnIndex
                        title.contains("кол") -> colQty = cell.columnIndex
                        title.contains("сумма") || title.contains(other = "итоговая цена") -> colTotal = cell.columnIndex
                        title.contains("дата") -> colDate = cell.columnIndex
                    }
                }

                // ПРОВЕРКА: Если обязательный столбец с названием товара не найден, прерываем импорт
                if (colProduct == -1) {
                    throw Exception("Столбец 'Вид товара' или 'Товар' не найден в файле")
                }

                for (rowIndex in 1..sheet.lastRowNum) {
                    val row = sheet.getRow(rowIndex) ?: continue
                    val productName = if (colProduct >= 0) getCellValueAsString(row.getCell(colProduct)).trim() else ""
                    if (productName.isEmpty()) continue

                    val pricePerUnit = if (colPrice >= 0) getCellValueAsDouble(row.getCell(colPrice)) else 0.0
                    val quantity = if (colQty >= 0) getCellValueAsDouble(row.getCell(colQty)) else 0.0
                    val totalAmountFromCell = if (colTotal >= 0) getCellValueAsDouble(row.getCell(colTotal)) else 0.0

                    val totalAmount = if (totalAmountFromCell == 0.0) pricePerUnit * quantity else totalAmountFromCell
                    val dateStr = if (colDate >= 0) getCellValueAsString(row.getCell(colDate)) else ""

                    // Проверяем, существует ли товар в Firebase
                    val existingProducts = db.collection("products")
                        .whereEqualTo("name", productName)
                        .get().await()

                    val productId: String = if (!existingProducts.isEmpty) {
                        existingProducts.documents[0].id
                    } else {
                        val productType = if (defaultType == "SALE") "PRODUCT" else "MATERIAL"
                        repository.addProduct(Product(name = productName, type = productType, price = pricePerUnit))
                    }

                    // Создаем транзакцию
                    val item = TransactionItem(
                        productId = productId,
                        productName = productName,
                        quantity = quantity,
                        pricePerUnit = pricePerUnit
                    )

                    val transaction = Transaction(
                        type = defaultType,
                        totalAmount = totalAmount,
                        date = dateStr,
                        comment = "Импорт из Excel",
                        items = listOf(item)
                    )

                    repository.addTransaction(transaction)
                    importedCount++
                }
            }
            importedCount
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