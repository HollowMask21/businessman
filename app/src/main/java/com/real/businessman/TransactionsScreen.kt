package com.real.businessman

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.real.businessman.database.Transaction
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    transactionViewModel: TransactionViewModel,
    productViewModel: ProductViewModel
) {
    val transactions by transactionViewModel.transactions.collectAsStateWithLifecycle()
    val products by productViewModel.products.collectAsStateWithLifecycle()
    val isLoading by transactionViewModel.isLoading.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("История операций") },
                actions = {
                    TextButton(onClick = { showAddDialog = true }) {
                        Text("+ Операция")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else if (transactions.isEmpty()) {
                Text(
                    text = "Транзакций пока нет.\nИмпортируйте Excel или создайте запись.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(transactions) { transaction ->
                        TransactionItemCard(transaction = transaction)
                    }
                }
            }
        }

        if (showAddDialog) {
            AddTransactionDialog(
                products = products.map { it.name },
                onDismiss = { showAddDialog = false },
                onConfirm = { type, date, productName, quantity, price, comment ->
                    transactionViewModel.addManualTransaction(type, date, productName, quantity, price, comment)
                    showAddDialog = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    products: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (type: String, date: String, productName: String, quantity: Double, price: Double, comment: String) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    var selectedType by remember { mutableStateOf("SALE") }
    var selectedProduct by remember { mutableStateOf("") }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    // Текущая дата по умолчанию (ДД.ММ.ГГГГ)
    val currentDay = String.format(Locale.getDefault(), "%02d", calendar.get(Calendar.DAY_OF_MONTH))
    val currentMonth = String.format(Locale.getDefault(), "%02d", calendar.get(Calendar.MONTH) + 1)
    val currentYear = calendar.get(Calendar.YEAR)
    var selectedDate by remember { mutableStateOf("$currentDay.$currentMonth.$currentYear") }

    var quantityText by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var commentText by remember { mutableStateOf("") }

    val typeOptions = listOf("SALE" to "Продажа", "PURCHASE" to "Закупка", "EXPENSE" to "Расход")

    // Окно выбора даты
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val dayStr = String.format(Locale.getDefault(), "%02d", dayOfMonth)
            val monthStr = String.format(Locale.getDefault(), "%02d", month + 1)
            selectedDate = "$dayStr.$monthStr.$year"
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    // Автоматический расчет суммы
    val quantityVal = quantityText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val priceVal = priceText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val totalSum = quantityVal * priceVal

    val isValid = selectedProduct.isNotBlank() && quantityVal > 0 && priceVal > 0 && selectedDate.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая операция") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // 1. Выбор типа операции
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    typeOptions.forEach { (typeKey, typeLabel) ->
                        FilterChip(
                            selected = selectedType == typeKey,
                            onClick = { selectedType = typeKey },
                            label = { Text(typeLabel) }
                        )
                    }
                }

                // 2. Выбор товара (выпадающий список)
                ExposedDropdownMenuBox(
                    expanded = isDropdownExpanded,
                    onExpandedChange = { isDropdownExpanded = !isDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedProduct,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Товар / Позиция") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false }
                    ) {
                        if (products.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Каталог пуст") },
                                onClick = { isDropdownExpanded = false }
                            )
                        } else {
                            products.forEach { productName ->
                                DropdownMenuItem(
                                    text = { Text(productName) },
                                    onClick = {
                                        selectedProduct = productName
                                        isDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // 3. Выбор даты
                OutlinedTextField(
                    value = selectedDate,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Дата") },
                    trailingIcon = {
                        IconButton(onClick = { datePickerDialog.show() }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Выбрать дату")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { datePickerDialog.show() }
                )

                // 4. Количество и цена
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("Кол-во") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Цена за ед.") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // 5. Автоматически пересчитываемая итоговая сумма
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Итоговая сумма:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "$totalSum ₽",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 6. Опциональный комментарий
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    label = { Text("Комментарий (опционально)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) {
                        onConfirm(selectedType, selectedDate, selectedProduct, quantityVal, priceVal, commentText)
                    }
                },
                enabled = isValid
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun TransactionItemCard(transaction: Transaction) {
    var isExpanded by remember { mutableStateOf(false) }

    val isIncome = transaction.type == "SALE"
    val amountColor = if (isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)
    val prefix = if (isIncome) "+" else "-"

    val typeTitle = when (transaction.type) {
        "SALE" -> "Продажа"
        "PURCHASE" -> "Закупка товара"
        "EXPENSE" -> "Расход"
        else -> "Операция"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = typeTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (transaction.date.isNotBlank()) {
                        Text(
                            text = transaction.date,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "$prefix${transaction.totalAmount} ₽",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
            }

            if (transaction.comment.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = transaction.comment,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = isExpanded && transaction.items.isNotEmpty()) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
                    Text(
                        text = "Позиции в чеке:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    transaction.items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${item.productName} × ${item.quantity}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${item.pricePerUnit * item.quantity} ₽",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}