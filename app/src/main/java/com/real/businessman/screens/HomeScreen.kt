package com.real.businessman.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // БЛОК 1: Последние операции (занимает оставшуюся часть ~3/4)
                Box(modifier = Modifier.weight(1f)) {
                    RecentTransactionsBlock(
                        transactions = transactions.take(5),
                        isLoading = isLoading,
                        onSeeAllClick = onNavigateToTransactions
                    )
                }

                // БЛОК 2: Быстрые действия (занимает ~1/4 ширины экрана относительно weight(1f))
                Box(modifier = Modifier.weight(0.35f)) {
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
                }
            }
        }
    }
}

private data class QuickActionItem(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

/**
 * Блок быстрых действий с увеличенными кнопками
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
            add(QuickActionItem("Товар", Icons.Default.ShoppingCart, onAddProductClick))
            add(QuickActionItem("Операция", Icons.Default.Add, onAddTransactionClick))
            if (userRole.canImportExcel) {
                add(QuickActionItem("Импорт продаж", Icons.Default.ArrowForward, onImportSalesClick))
                add(QuickActionItem("Импорт расходов", Icons.Default.ArrowBack, onImportExpensesClick))
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Действия",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            // Разбиваем кнопки на ряды по 2 штуки для сетки 2х2
            val rows = actions.chunked(2)
            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Top
                ) {
                    rowItems.forEach { action ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            QuickActionButton(item = action)
                        }
                    }
                    // Если в последнем ряду только одна кнопка, добавляем пустой элемент для выравнивания сетки
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(item: QuickActionItem) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Увеличенный размер круглой кнопки (например, 56.dp)
        Button(
            onClick = item.onClick,
            shape = CircleShape,
            modifier = Modifier.size(96.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                modifier = Modifier.size(48.dp)
            )
        }
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

/**
 * Блок последних операций
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
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                TextButton(
                    onClick = onSeeAllClick,
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
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