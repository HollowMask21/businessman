package com.real.businessman.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class DateFilterType { ALL, YEAR, MONTH, CUSTOM }

// Вспомогательная функция форматирования чисел (10000 -> 10 000, 10.0 -> 10, 10.5 -> 10.5)
private fun Double.formatAmount(): String {
    val symbols = DecimalFormatSymbols(Locale.getDefault()).apply {
        groupingSeparator = ' '
    }
    return DecimalFormat("#,##0.##", symbols).format(this)
}

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

    // Состояние прокрутки списка
    val listState = rememberLazyListState()

    // Фильтр по категориям: null - все, "EXPENSE" - закупки и расходы, "SALE" - продажи
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }

    // Фильтр по дате и периодам
    var dateFilterType by remember { mutableStateOf(DateFilterType.ALL) }
    var selectedMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH)) }
    var selectedYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var customStartDate by remember { mutableStateOf<String?>(null) }
    var customEndDate by remember { mutableStateOf<String?>(null) }
    var showDateFilterDialog by remember { mutableStateOf(false) }

    // Сброс прокрутки в начало при изменении любого из фильтров
    LaunchedEffect(
        selectedCategoryFilter,
        dateFilterType,
        selectedMonth,
        selectedYear,
        customStartDate,
        customEndDate
    ) {
        listState.scrollToItem(0)
    }

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
        transactions.sortedByDescending { transaction ->
            try {
                dateFormat.parse(transaction.date)
            } catch (_: Exception) {
                Date(0)
            }
        }
    }

    // Фильтрация по дате/периоду
    val dateFilteredTransactions = remember(
        sortedTransactions,
        dateFilterType,
        selectedYear,
        selectedMonth,
        customStartDate,
        customEndDate
    ) {
        sortedTransactions.filter { transaction ->
            val tDate = try { dateFormat.parse(transaction.date) } catch (_: Exception) { null }
            if (tDate == null) return@filter true

            val cal = Calendar.getInstance().apply { time = tDate }

            when (dateFilterType) {
                DateFilterType.ALL -> true
                DateFilterType.YEAR -> {
                    cal.get(Calendar.YEAR) == selectedYear
                }
                DateFilterType.MONTH -> {
                    cal.get(Calendar.MONTH) == selectedMonth && cal.get(Calendar.YEAR) == selectedYear
                }
                DateFilterType.CUSTOM -> {
                    val start = customStartDate?.let { try { dateFormat.parse(it) } catch (_: Exception) { null } }
                    val end = customEndDate?.let { try { dateFormat.parse(it) } catch (_: Exception) { null } }

                    val isAfterStart = start == null || !tDate.before(start)
                    val isBeforeEnd = end == null || !tDate.after(end)
                    isAfterStart && isBeforeEnd
                }
            }
        }
    }

    // Отображаемый список с учетом фильтрации по категории
    val displayedTransactions = remember(dateFilteredTransactions, selectedCategoryFilter) {
        when (selectedCategoryFilter) {
            "SALE" -> dateFilteredTransactions.filter { it.type == "SALE" }
            "EXPENSE" -> dateFilteredTransactions.filter { it.type == "PURCHASE" || it.type == "EXPENSE" }
            else -> dateFilteredTransactions
        }
    }

    // Расчет сумм за выбранный период
    val totalSales = remember(dateFilteredTransactions) {
        dateFilteredTransactions.filter { it.type == "SALE" }.sumOf { it.totalAmount }
    }
    val totalPurchasesAndExpenses = remember(dateFilteredTransactions) {
        dateFilteredTransactions.filter { it.type == "PURCHASE" || it.type == "EXPENSE" }.sumOf { it.totalAmount }
    }

    val monthNames = remember {
        listOf("Январь", "Февраль", "Март", "Апрель", "Май", "Июнь", "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь")
    }

    val periodLabel = remember(dateFilterType, selectedMonth, selectedYear, customStartDate, customEndDate) {
        when (dateFilterType) {
            DateFilterType.ALL -> "Все время"
            DateFilterType.YEAR -> "$selectedYear год"
            DateFilterType.MONTH -> "${monthNames[selectedMonth]} $selectedYear г."
            DateFilterType.CUSTOM -> {
                val start = customStartDate ?: "..."
                val end = customEndDate ?: "..."
                "$start - $end"
            }
        }
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
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Редактировать")
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
            // Кнопка выбора периода
            FilterChip(
                selected = dateFilterType != DateFilterType.ALL,
                onClick = { showDateFilterDialog = true },
                label = { Text("Период: $periodLabel") },
                leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null) },
                modifier = Modifier.padding(vertical = 4.dp)
            )

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
                            text = "-${totalPurchasesAndExpenses.formatAmount()} ₽",
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
                            text = "+${totalSales.formatAmount()} ₽",
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
                            state = listState,
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
                            text = if (selectedCategoryFilter != null || dateFilterType != DateFilterType.ALL) {
                                "За выбранный период / категорию операций не найдено"
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

        // Диалог фильтрации по датам
        if (showDateFilterDialog) {
            DateFilterSelectionDialog(
                currentFilterType = dateFilterType,
                currentMonth = selectedMonth,
                currentYear = selectedYear,
                currentStartDate = customStartDate,
                currentEndDate = customEndDate,
                onDismiss = { showDateFilterDialog = false },
                onApply = { type, month, year, start, end ->
                    dateFilterType = type
                    selectedMonth = month
                    selectedYear = year
                    customStartDate = start
                    customEndDate = end
                    showDateFilterDialog = false
                }
            )
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
                initialQuantity = firstItem?.quantity?.formatAmount()?.replace(" ", "") ?: "",
                initialPrice = firstItem?.pricePerUnit?.formatAmount()?.replace(" ", "") ?: "",
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
fun DateFilterSelectionDialog(
    currentFilterType: DateFilterType,
    currentMonth: Int,
    currentYear: Int,
    currentStartDate: String?,
    currentEndDate: String?,
    onDismiss: () -> Unit,
    onApply: (type: DateFilterType, month: Int, year: Int, startDate: String?, endDate: String?) -> Unit
) {
    var tempFilterType by remember { mutableStateOf(currentFilterType) }
    var tempMonth by remember { mutableStateOf(currentMonth) }
    var tempYear by remember { mutableStateOf(currentYear) }

    val monthNames = listOf("Январь", "Февраль", "Март", "Апрель", "Май", "Июнь", "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь")
    val currentCalendarYear = Calendar.getInstance().get(Calendar.YEAR)
    val yearList = (currentCalendarYear - 5..currentCalendarYear + 2).toList()

    var isMonthDropdownExpanded by remember { mutableStateOf(false) }
    var isYearDropdownExpanded by remember { mutableStateOf(false) }

    val utcFormat = remember {
        SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    val initialStartMillis = remember(currentStartDate) {
        currentStartDate?.let { try { utcFormat.parse(it)?.time } catch (_: Exception) { null } }
    }
    val initialEndMillis = remember(currentEndDate) {
        currentEndDate?.let { try { utcFormat.parse(it)?.time } catch (_: Exception) { null } }
    }

    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStartMillis,
        initialSelectedEndDateMillis = initialEndMillis
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Фильтр по периоду") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Выбор типа фильтра
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = tempFilterType == DateFilterType.ALL,
                        onClick = { tempFilterType = DateFilterType.ALL },
                        label = { Text("Все") }
                    )
                    FilterChip(
                        selected = tempFilterType == DateFilterType.YEAR,
                        onClick = { tempFilterType = DateFilterType.YEAR },
                        label = { Text("Год") }
                    )
                    FilterChip(
                        selected = tempFilterType == DateFilterType.MONTH,
                        onClick = { tempFilterType = DateFilterType.MONTH },
                        label = { Text("Месяц") }
                    )
                    FilterChip(
                        selected = tempFilterType == DateFilterType.CUSTOM,
                        onClick = { tempFilterType = DateFilterType.CUSTOM },
                        label = { Text("Период") }
                    )
                }

                when (tempFilterType) {
                    DateFilterType.ALL -> {
                        Text(
                            text = "Отображаются все операции за всё время.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DateFilterType.YEAR -> {
                        ExposedDropdownMenuBox(
                            expanded = isYearDropdownExpanded,
                            onExpandedChange = { isYearDropdownExpanded = !isYearDropdownExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = "$tempYear год",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Выберите год") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isYearDropdownExpanded) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = isYearDropdownExpanded,
                                onDismissRequest = { isYearDropdownExpanded = false }
                            ) {
                                yearList.forEach { year ->
                                    DropdownMenuItem(
                                        text = { Text("$year год") },
                                        onClick = {
                                            tempYear = year
                                            isYearDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    DateFilterType.MONTH -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ExposedDropdownMenuBox(
                                expanded = isMonthDropdownExpanded,
                                onExpandedChange = { isMonthDropdownExpanded = !isMonthDropdownExpanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = monthNames[tempMonth],
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Месяц") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isMonthDropdownExpanded) },
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = isMonthDropdownExpanded,
                                    onDismissRequest = { isMonthDropdownExpanded = false }
                                ) {
                                    monthNames.forEachIndexed { index, name ->
                                        DropdownMenuItem(
                                            text = { Text(name) },
                                            onClick = {
                                                tempMonth = index
                                                isMonthDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            ExposedDropdownMenuBox(
                                expanded = isYearDropdownExpanded,
                                onExpandedChange = { isYearDropdownExpanded = !isYearDropdownExpanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = "$tempYear год",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Год") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isYearDropdownExpanded) },
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = isYearDropdownExpanded,
                                    onDismissRequest = { isYearDropdownExpanded = false }
                                ) {
                                    yearList.forEach { year ->
                                        DropdownMenuItem(
                                            text = { Text("$year год") },
                                            onClick = {
                                                tempYear = year
                                                isYearDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    DateFilterType.CUSTOM -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(360.dp)
                        ) {
                            DateRangePicker(
                                state = dateRangePickerState,
                                title = null,
                                headline = null,
                                showModeToggle = false,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val formattedStart = dateRangePickerState.selectedStartDateMillis?.let { utcFormat.format(Date(it)) }
                    val formattedEnd = dateRangePickerState.selectedEndDateMillis?.let { utcFormat.format(Date(it)) }

                    onApply(
                        tempFilterType,
                        tempMonth,
                        tempYear,
                        formattedStart,
                        formattedEnd
                    )
                }
            ) {
                Text("Применить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
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

    // Состояние вызова диалога встроенного календаря Material3 DatePicker
    var showDatePickerDialog by remember { mutableStateOf(false) }

    val typeOptions = listOf("SALE" to "Продажа", "PURCHASE" to "Закупка", "EXPENSE" to "Расход")

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

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePickerDialog = true }
                ) {
                    OutlinedTextField(
                        value = selectedDate,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("Дата") },
                        trailingIcon = {
                            IconButton(onClick = { showDatePickerDialog = true }) {
                                Icon(Icons.Default.DateRange, contentDescription = "Выбрать дату")
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

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
                            text = "${totalSum.formatAmount()} ₽",
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

    // Всплывающий календарь Material3 DatePicker для выбора одиночной даты
    if (showDatePickerDialog) {
        val utcFormat = remember {
            SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
        }
        val initialMillis = remember(selectedDate) {
            try { utcFormat.parse(selectedDate)?.time } catch (_: Exception) { null }
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis
        )

        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = utcFormat.format(Date(millis))
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text("ОК")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Отмена")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false
            )
        }
    }
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
                    text = "$prefix${transaction.totalAmount.formatAmount()} ₽",
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
                                    text = "${item.productName} × ${item.quantity.formatAmount()}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "${(item.pricePerUnit * item.quantity).formatAmount()} ₽",
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