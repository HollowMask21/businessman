package com.real.businessman.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
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
import androidx.compose.ui.draw.clip
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

private fun Double.formatAmount(): String {
    val symbols = DecimalFormatSymbols(Locale.getDefault()).apply {
        groupingSeparator = ' '
    }
    return DecimalFormat("#,##0.##", symbols).format(this)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun transactionTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    errorContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    errorIndicatorColor = Color.Transparent
)

@Composable
fun TransactionTypeSegmentedButton(
    selectedType: String,
    onTypeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface) // Цвет фона приведен к основному
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val options = listOf("SALE" to "Продажа", "PURCHASE" to "Закупка", "EXPENSE" to "Расход")

        options.forEach { (typeKey, typeLabel) ->
            val isSelected = selectedType == typeKey

            val targetBackgroundColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)
            }

            val targetTextColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }

            val backgroundColor by animateColorAsState(
                targetValue = targetBackgroundColor,
                animationSpec = tween(durationMillis = 300),
                label = "bgColorAnimation"
            )

            val textColor by animateColorAsState(
                targetValue = targetTextColor,
                animationSpec = tween(durationMillis = 300),
                label = "textColorAnimation"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(backgroundColor)
                    .clickable { onTypeSelected(typeKey) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = textColor
                )
            }
        }
    }
}

@Composable
fun DateFilterTypeSegmentedButton(
    selectedType: DateFilterType,
    onTypeSelected: (DateFilterType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface) // Цвет фона приведен к основному
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val options = listOf(
            DateFilterType.ALL to "Все",
            DateFilterType.YEAR to "Год",
            DateFilterType.MONTH to "Месяц",
            DateFilterType.CUSTOM to "Период"
        )

        options.forEach { (typeKey, typeLabel) ->
            val isSelected = selectedType == typeKey

            val targetBackgroundColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)
            }

            val targetTextColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }

            val backgroundColor by animateColorAsState(
                targetValue = targetBackgroundColor,
                animationSpec = tween(durationMillis = 300),
                label = "dateFilterBgAnim"
            )

            val textColor by animateColorAsState(
                targetValue = targetTextColor,
                animationSpec = tween(durationMillis = 300),
                label = "dateFilterTextAnim"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(backgroundColor)
                    .clickable { onTypeSelected(typeKey) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = textColor
                )
            }
        }
    }
}

@Composable
fun DateFilterButton(
    selectedFilterType: DateFilterType,
    selectedMonth: Int,
    selectedYear: Int,
    startDate: String?,
    endDate: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAllSelected = selectedFilterType == DateFilterType.ALL

    val backgroundColor = if (isAllSelected) {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }

    val contentColor = if (isAllSelected) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }

    val monthNames = remember {
        listOf("Январь", "Февраль", "Март", "Апрель", "Май", "Июнь", "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь")
    }

    val filterLabel = when (selectedFilterType) {
        DateFilterType.ALL -> "Все"
        DateFilterType.YEAR -> "$selectedYear год"
        DateFilterType.MONTH -> "${monthNames.getOrElse(selectedMonth) { "" }} $selectedYear"
        DateFilterType.CUSTOM -> {
            if (startDate != null && endDate != null) "$startDate – $endDate"
            else if (startDate != null) "с $startDate"
            else "Период"
        }
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        contentColor = contentColor,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = "Фильтр периода",
                modifier = Modifier.size(18.dp),
                tint = contentColor
            )
            Text(
                text = filterLabel,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor
            )
        }
    }
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
    val listState = rememberLazyListState()

    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }

    var dateFilterType by remember { mutableStateOf(DateFilterType.ALL) }
    var selectedMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH)) }
    var selectedYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var customStartDate by remember { mutableStateOf<String?>(null) }
    var customEndDate by remember { mutableStateOf<String?>(null) }
    var showDateFilterDialog by remember { mutableStateOf(false) }

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

    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    val isSelectionMode = selectedIds.isNotEmpty()
    var showDeleteDialog by remember { mutableStateOf(false) }

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
                DateFilterType.YEAR -> cal.get(Calendar.YEAR) == selectedYear
                DateFilterType.MONTH -> cal.get(Calendar.MONTH) == selectedMonth && cal.get(Calendar.YEAR) == selectedYear
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

    val displayedTransactions = remember(dateFilteredTransactions, selectedCategoryFilter) {
        when (selectedCategoryFilter) {
            "SALE" -> dateFilteredTransactions.filter { it.type == "SALE" }
            "EXPENSE" -> dateFilteredTransactions.filter { it.type == "PURCHASE" || it.type == "EXPENSE" }
            else -> dateFilteredTransactions
        }
    }

    val totalSales = remember(dateFilteredTransactions) {
        dateFilteredTransactions.filter { it.type == "SALE" }.sumOf { it.totalAmount }
    }
    val totalPurchasesAndExpenses = remember(dateFilteredTransactions) {
        dateFilteredTransactions.filter { it.type == "PURCHASE" || it.type == "EXPENSE" }.sumOf { it.totalAmount }
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
                    title = { Text("Выбрано: ${selectedIds.size}", fontWeight = FontWeight.Bold) },
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
                        containerColor = MaterialTheme.colorScheme.background // Одинаковый цвет фона
                    ),
                    windowInsets = WindowInsets(0) // Смещение заголовка максимально вверх
                )
            } else {
                TopAppBar(
                    title = { Text("История операций", fontWeight = FontWeight.Bold) }, // Жирный текст
                    actions = {
                        TextButton(onClick = { showAddDialog = true }) {
                            Text("+ Операция")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background // Одинаковый цвет фона
                    ),
                    windowInsets = WindowInsets(0) // Смещение заголовка максимально вверх
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
            DateFilterButton(
                selectedFilterType = dateFilterType,
                selectedMonth = selectedMonth,
                selectedYear = selectedYear,
                startDate = customStartDate,
                endDate = customEndDate,
                onClick = { showDateFilterDialog = true },
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isExpenseSelected = selectedCategoryFilter == "EXPENSE"
                val isSaleSelected = selectedCategoryFilter == "SALE"

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            selectedCategoryFilter = if (isExpenseSelected) null else "EXPENSE"
                        },
                    border = if (isExpenseSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                    colors = CardDefaults.cardColors(
                        containerColor = if (isExpenseSelected) {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            selectedCategoryFilter = if (isSaleSelected) null else "SALE"
                        },
                    border = if (isSaleSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSaleSelected) {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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

            Spacer(modifier = Modifier.height(32.dp))

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
                DateFilterTypeSegmentedButton(
                    selectedType = tempFilterType,
                    onTypeSelected = { tempFilterType = it }
                )

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
                            TextField(
                                value = "$tempYear год",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Выберите год") },
                                trailingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .padding(4.dp)
                                    ) {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = isYearDropdownExpanded)
                                    }
                                },
                                shape = MaterialTheme.shapes.medium,
                                colors = transactionTextFieldColors(),
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = isYearDropdownExpanded,
                                onDismissRequest = { isYearDropdownExpanded = false },
                                modifier = Modifier.clip(RoundedCornerShape(12.dp))
                            ) {
                                yearList.forEach { year ->
                                    DropdownMenuItem(
                                        text = { Text("$year год") },
                                        onClick = {
                                            tempYear = year
                                            isYearDropdownExpanded = false
                                        },
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                            .clip(RoundedCornerShape(8.dp))
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
                                TextField(
                                    value = monthNames[tempMonth],
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Месяц") },
                                    trailingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .padding(4.dp)
                                        ) {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isMonthDropdownExpanded)
                                        }
                                    },
                                    shape = MaterialTheme.shapes.medium,
                                    colors = transactionTextFieldColors(),
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = isMonthDropdownExpanded,
                                    onDismissRequest = { isMonthDropdownExpanded = false },
                                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                                ) {
                                    monthNames.forEachIndexed { index, name ->
                                        DropdownMenuItem(
                                            text = { Text(name) },
                                            onClick = {
                                                tempMonth = index
                                                isMonthDropdownExpanded = false
                                            },
                                            modifier = Modifier
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                    }
                                }
                            }

                            ExposedDropdownMenuBox(
                                expanded = isYearDropdownExpanded,
                                onExpandedChange = { isYearDropdownExpanded = !isYearDropdownExpanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TextField(
                                    value = "$tempYear год",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Год") },
                                    trailingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .padding(4.dp)
                                        ) {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isYearDropdownExpanded)
                                        }
                                    },
                                    shape = MaterialTheme.shapes.medium,
                                    colors = transactionTextFieldColors(),
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = isYearDropdownExpanded,
                                    onDismissRequest = { isYearDropdownExpanded = false },
                                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                                ) {
                                    yearList.forEach { year ->
                                        DropdownMenuItem(
                                            text = { Text("$year год") },
                                            onClick = {
                                                tempYear = year
                                                isYearDropdownExpanded = false
                                            },
                                            modifier = Modifier
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                                .clip(RoundedCornerShape(8.dp))
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

    var showDatePickerDialog by remember { mutableStateOf(false) }

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
                TransactionTypeSegmentedButton(
                    selectedType = selectedType,
                    onTypeSelected = { selectedType = it }
                )

                ExposedDropdownMenuBox(
                    expanded = isDropdownExpanded,
                    onExpandedChange = { isDropdownExpanded = !isDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextField(
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
                        trailingIcon = {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(4.dp)
                            ) {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded)
                            }
                        },
                        shape = MaterialTheme.shapes.medium,
                        colors = transactionTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false },
                        modifier = Modifier.clip(RoundedCornerShape(12.dp))
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
                                    },
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                        .clip(RoundedCornerShape(8.dp))
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
                    TextField(
                        value = selectedDate,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("Дата") },
                        trailingIcon = {
                            IconButton(
                                onClick = { showDatePickerDialog = true },
                                modifier = Modifier.clip(CircleShape)
                            ) {
                                Icon(Icons.Default.DateRange, contentDescription = "Выбрать дату")
                            }
                        },
                        shape = MaterialTheme.shapes.medium,
                        colors = TextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("Кол-во") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        colors = transactionTextFieldColors(),
                        modifier = Modifier.weight(1f)
                    )

                    TextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Цена за ед.") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        colors = transactionTextFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Итоговая сумма",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }

                        Text(
                            text = "${totalSum.formatAmount()} ₽",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                TextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    label = { Text("Комментарий (опционально)") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = transactionTextFieldColors(),
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
    val amountColor = if (isIncome) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
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
                    style = MaterialTheme.typography.titleMedium,
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