package com.real.businessman.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SelectAll
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
import com.real.businessman.UserRole
import com.real.businessman.database.Product
import com.real.businessman.database.Transaction
import com.real.businessman.viewmodels.ProductViewModel
import com.real.businessman.viewmodels.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    userRole: UserRole,
    transactionViewModel: TransactionViewModel,
    productViewModel: ProductViewModel
) {
    val transactions by transactionViewModel.transactions.collectAsStateWithLifecycle()
    val products by productViewModel.products.collectAsStateWithLifecycle()
    val isLoading by transactionViewModel.isLoading.collectAsStateWithLifecycle()
    val statusMessage by transactionViewModel.statusMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Состояние фильтрации по категории: null - все, "EXPENSE" - закупки и расходы, "SALE" - продажи
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }

    // Состояния множественного выбора
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    val isSelectionMode = selectedIds.isNotEmpty()
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Состояния диалогов
    var showAddDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }

    LaunchedEffect(statusMessage) {
        statusMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            transactionViewModel.clearStatusMessage()
        }
    }

    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    val sortedTransactions = remember(transactions) {
        transactions.sortedBy { transaction ->
            try {
                dateFormat.parse(transaction.date)
            } catch (_: Exception) {
                Date(0)
            }
        }
    }

    // Отфильтрованный список транзакций в зависимости от выбранного блока
    val displayedTransactions = remember(sortedTransactions, selectedCategoryFilter) {
        when (selectedCategoryFilter) {
            "SALE" -> sortedTransactions.filter { it.type == "SALE" }
            "EXPENSE" -> sortedTransactions.filter { it.type == "PURCHASE" || it.type == "EXPENSE" }
            else -> sortedTransactions
        }
    }

    // Расчет общих сумм (всегда считывается от всего списка)
    val totalSales = remember(transactions) {
        transactions.filter { it.type == "SALE" }.sumOf { it.totalAmount }
    }
    val totalPurchasesAndExpenses = remember(transactions) {
        transactions.filter { it.type == "PURCHASE" || it.type == "EXPENSE" }.sumOf { it.totalAmount }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Surface(
                    modifier = Modifier
                        .wrapContentSize()
                        .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = data.visuals.message,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = { Text("Выбрано: ${selectedIds.size}") },
                    navigationIcon = {
                        IconButton(onClick = { selectedIds = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Сбросить")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                val selectedId = selectedIds.firstOrNull()
                                transactionToEdit = sortedTransactions.find { it.id == selectedId }
                            },
                            enabled = selectedIds.size == 1
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Редактировать"
                            )
                        }

                        IconButton(onClick = {
                            selectedIds = if (selectedIds.size == displayedTransactions.size) {
                                emptySet()
                            } else {
                                displayedTransactions.map { it.id }.toSet()
                            }
                        }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Выбрать все")
                        }

                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Удалить выбранное",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                )
            } else {
                TopAppBar(
                    title = { Text("История операций") },
                    actions = {
                        TextButton(onClick = { showAddDialog = true }) {
                            Text("+ Операция")
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Интерактивные блоки сумм
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isExpenseSelected = selectedCategoryFilter == "EXPENSE"
                val isSaleSelected = selectedCategoryFilter == "SALE"

                // Блок 1: Закупки и расходы
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            selectedCategoryFilter = if (isExpenseSelected) null else "EXPENSE"
                        },
                    border = if (isExpenseSelected) BorderStroke(2.dp, Color(0xFFC62828)) else null,
                    colors = CardDefaults.cardColors(
                        containerColor = if (isExpenseSelected) {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Закупки и расходы",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "-$totalPurchasesAndExpenses ₽",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC62828)
                        )
                    }
                }

                // Блок 2: Продажи
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            selectedCategoryFilter = if (isSaleSelected) null else "SALE"
                        },
                    border = if (isSaleSelected) BorderStroke(2.dp, Color(0xFF2E7D32)) else null,
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSaleSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Продажи",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "+$totalSales ₽",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Список транзакций
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    displayedTransactions.isNotEmpty() -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(displayedTransactions, key = { it.id }) { transaction ->
                                val isSelected = selectedIds.contains(transaction.id)

                                TransactionItemCard(
                                    transaction = transaction,
                                    isSelected = isSelected,
                                    isSelectionMode = isSelectionMode,
                                    onSelectToggle = {
                                        selectedIds = if (isSelected) {
                                            selectedIds - transaction.id
                                        } else {
                                            selectedIds + transaction.id
                                        }
                                    },
                                    onLongClick = {
                                        if (!isSelectionMode) {
                                            selectedIds = setOf(transaction.id)
                                        }
                                    }
                                )
                            }
                        }
                    }
                    isLoading -> {
                        CircularProgressIndicator()
                    }
                    else -> {
                        Text(
                            text = if (selectedCategoryFilter != null) {
                                "В этой категории нет транзакций"
                            } else {
                                "Транзакций пока нет.\nСоздайте новую запись."
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Диалог создания
        if (showAddDialog) {
            AddEditTransactionDialog(
                products = products,
                onDismiss = { showAddDialog = false },
                onConfirm = { type, date, productName, quantity, price, comment ->
                    transactionViewModel.addManualTransaction(type, date, productName, quantity, price, comment)
                    showAddDialog = false
                }
            )
        }

        // Диалог редактирования
        transactionToEdit?.let { target ->
            val firstItem = target.items.firstOrNull()
            AddEditTransactionDialog(
                products = products,
                initialType = target.type,
                initialDate = target.date,
                initialProduct = firstItem?.productName ?: "",
                initialQuantity = firstItem?.quantity?.toString() ?: "",
                initialPrice = firstItem?.pricePerUnit?.toString() ?: "",
                initialComment = target.comment,
                isEditMode = true,
                onDismiss = { transactionToEdit = null },
                onConfirm = { type, date, productName, quantity, price, comment ->
                    transactionViewModel.updateManualTransaction(
                        id = target.id,
                        type = type,
                        date = date,
                        productName = productName,
                        quantity = quantity,
                        pricePerUnit = price,
                        comment = comment
                    )
                    transactionToEdit = null
                    selectedIds = emptySet()
                }
            )
        }

        // Диалог удаления
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(if (selectedIds.size == 1) "Удалить операцию?" else "Удалить выбранные операции?") },
                text = { Text("Вы действительно хотите удалить записи (${selectedIds.size} шт.)?") },
                confirmButton = {
                    Button(
                        onClick = {
                            transactionViewModel.deleteSelectedTransactions(selectedIds)
                            selectedIds = emptySet()
                            showDeleteDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Удалить")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Отмена")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionDialog(
    products: List<Product>,
    initialType: String = "SALE",
    initialDate: String = "",
    initialProduct: String = "",
    initialQuantity: String = "",
    initialPrice: String = "",
    initialComment: String = "",
    isEditMode: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (type: String, date: String, productName: String, quantity: Double, price: Double, comment: String) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    var selectedType by remember { mutableStateOf(initialType) }
    var selectedProduct by remember { mutableStateOf(initialProduct) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    val filteredProducts = remember(selectedType, products) {
        products.filter { product ->
            when (selectedType) {
                "SALE" -> product.type.equals("PRODUCT", ignoreCase = true) || product.type == "Товар"
                "PURCHASE" -> product.type.equals("MATERIAL", ignoreCase = true) || product.type == "Материал"
                "EXPENSE" -> product.type.equals("OTHER", ignoreCase = true) || product.type == "Другое"
                else -> true
            }
        }
    }

    LaunchedEffect(selectedType) {
        if (filteredProducts.none { it.name == selectedProduct }) {
            selectedProduct = ""
        }
    }

    val defaultDate = remember {
        val currentDay = String.format(Locale.getDefault(), "%02d", calendar.get(Calendar.DAY_OF_MONTH))
        val currentMonth = String.format(Locale.getDefault(), "%02d", calendar.get(Calendar.MONTH) + 1)
        val currentYear = calendar.get(Calendar.YEAR)
        "$currentDay.$currentMonth.$currentYear"
    }

    var selectedDate by remember { mutableStateOf(if (initialDate.isNotBlank()) initialDate else defaultDate) }
    var quantityText by remember { mutableStateOf(initialQuantity) }
    var priceText by remember { mutableStateOf(initialPrice) }
    var commentText by remember { mutableStateOf(initialComment) }

    val typeOptions = listOf("SALE" to "Продажа", "PURCHASE" to "Закупка", "EXPENSE" to "Расход")

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

    val quantityVal = quantityText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val priceVal = priceText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val totalSum = quantityVal * priceVal

    val isValid = selectedProduct.isNotBlank() && quantityVal > 0 && priceVal > 0 && selectedDate.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditMode) "Редактировать операцию" else "Новая операция") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    typeOptions.forEach { (typeKey, typeLabel) ->
                        FilterChip(
                            selected = selectedType == typeKey,
                            onClick = { selectedType = typeKey },
                            label = { Text(typeLabel) }
                        )
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = isDropdownExpanded,
                    onExpandedChange = { isDropdownExpanded = !isDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedProduct,
                        onValueChange = {},
                        readOnly = true,
                        label = {
                            Text(
                                when (selectedType) {
                                    "SALE" -> "Товар"
                                    "PURCHASE" -> "Материал"
                                    "EXPENSE" -> "Расходная позиция"
                                    else -> "Позиция"
                                }
                            )
                        },
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
                        if (filteredProducts.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Нет доступных позиций") },
                                onClick = { isDropdownExpanded = false }
                            )
                        } else {
                            filteredProducts.forEach { product ->
                                DropdownMenuItem(
                                    text = { Text(product.name) },
                                    onClick = {
                                        selectedProduct = product.name
                                        isDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

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
                Text(if (isEditMode) "Сохранить" else "Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionItemCard(
    transaction: Transaction,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onSelectToggle: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
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

    val headerTitle = if (transaction.date.isNotBlank()) {
        "$typeTitle • ${transaction.date}"
    } else {
        typeTitle
    }

    val hasDetails = transaction.items.isNotEmpty() || transaction.comment.isNotBlank()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onSelectToggle()
                    } else {
                        isExpanded = !isExpanded
                    }
                },
                onLongClick = onLongClick
            ),
        colors = if (isSelected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    AnimatedVisibility(visible = isSelectionMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onSelectToggle() },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }

                    Text(
                        text = headerTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "$prefix${transaction.totalAmount} ₽",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
            }

            AnimatedVisibility(visible = isExpanded && hasDetails) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))

                    if (transaction.items.isNotEmpty()) {
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

                    if (transaction.comment.isNotBlank()) {
                        if (transaction.items.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Text(
                            text = "Комментарий:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = transaction.comment,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}