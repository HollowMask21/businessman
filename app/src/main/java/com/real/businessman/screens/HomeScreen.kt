package com.real.businessman.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.real.businessman.viewmodels.TransactionViewModel
import com.real.businessman.database.Transaction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    transactionViewModel: TransactionViewModel,
    onNavigateToTransactions: () -> Unit
) {
    val transactions by transactionViewModel.transactions.collectAsStateWithLifecycle()
    val isLoading by transactionViewModel.isLoading.collectAsStateWithLifecycle()

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
            // БЛОК 1: Последние 5 операций
            RecentTransactionsBlock(
                transactions = transactions.take(5),
                isLoading = isLoading,
                onSeeAllClick = onNavigateToTransactions
            )

            // БЛОК 2: Место для будущих блоков (например, Статистика, Быстрый выбор и т.д.)
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
            // Шапка блока
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

            // Контент блока
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
                                transaction = transaction,
                                )
                        }
                    }
                }
            }
        }
    }
}