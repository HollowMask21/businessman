package com.real.businessman.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.real.businessman.UserRole
import com.real.businessman.database.Transaction
import com.real.businessman.viewmodels.ProductViewModel
import com.real.businessman.viewmodels.TransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userRole: UserRole,
    transactionViewModel: TransactionViewModel,
    productViewModel: ProductViewModel,
    onNavigateToTransactions: () -> Unit,
    onAddProductClick: () -> Unit,
    onAddTransactionClick: () -> Unit
) {
    val context = LocalContext.current
    val transactions by transactionViewModel.transactions.collectAsStateWithLifecycle()
    val isLoading by transactionViewModel.isLoading.collectAsStateWithLifecycle()

    var importType by remember { mutableStateOf("SALE") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { productViewModel.importExcelFile(context, it, importType) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Главная") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // БЛОК 1: Быстрые действия
            QuickActionsBlock(
                userRole = userRole,
                onAddProductClick = onAddProductClick,
                onAddTransactionClick = onAddTransactionClick,
                onImportSalesClick = {
                    importType = "SALE"
                    filePickerLauncher.launch(
                        arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    )
                },
                onImportExpensesClick = {
                    importType = "PURCHASE"
                    filePickerLauncher.launch(
                        arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    )
                }
            )

            // БЛОК 2: Последние 5 операций
            RecentTransactionsBlock(
                transactions = transactions.take(5),
                isLoading = isLoading,
                onSeeAllClick = onNavigateToTransactions
            )
        }
    }
}

private data class QuickActionItem(
    val label: String,
    val onClick: () -> Unit
)

/**
 * Блок быстрых действий: максимум 2 кнопки в ряд с равным делением ширины
 */
@Composable
fun QuickActionsBlock(
    userRole: UserRole,
    onAddProductClick: () -> Unit,
    onAddTransactionClick: () -> Unit,
    onImportSalesClick: () -> Unit,
    onImportExpensesClick: () -> Unit
) {
    val actions = remember(userRole) {
        buildList {
            add(QuickActionItem("+ Товар", onAddProductClick))
            add(QuickActionItem("+ Операция", onAddTransactionClick))
            if (userRole.canImportExcel) {
                add(QuickActionItem("Импорт Продаж", onImportSalesClick))
                add(QuickActionItem("Импорт Расходов", onImportExpensesClick))
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Быстрые действия",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Разбиваем кнопки на строки по 2 штуки
            val rows = actions.chunked(2)
            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { action ->
                        Button(
                            onClick = action.onClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(action.label)
                        }
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * Выделенный блок последних операций для главного экрана
 */
@Composable
fun RecentTransactionsBlock(
    transactions: List<Transaction>,
    isLoading: Boolean,
    onSeeAllClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Последние операции",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onSeeAllClick) {
                    Text("Все")
                }
            }

            when {
                isLoading && transactions.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                transactions.isEmpty() -> {
                    Text(
                        text = "Операций пока нет",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        transactions.forEach { transaction ->
                            TransactionItemCard(
                                transaction = transaction
                            )
                        }
                    }
                }
            }
        }
    }
}