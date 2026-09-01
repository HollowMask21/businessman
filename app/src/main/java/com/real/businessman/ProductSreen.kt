package com.real.businessman

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ProductsScreen(viewModel: ProductViewModel) {
    val context = LocalContext.current

    // Подписка на состояния из ViewModel
    val products by viewModel.products.collectAsStateWithLifecycle()
    val importState by viewModel.importState.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    var selectedType by remember { mutableStateOf("SALE") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importExcelFile(context, it, selectedType) }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Управление каталогом (Firebase)", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        selectedType = "SALE"
                        filePickerLauncher.launch(
                            arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        )
                    }) {
                        Text("Импорт Продаж")
                    }

                    Button(onClick = {
                        selectedType = "PURCHASE"
                        filePickerLauncher.launch(
                            arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        )
                    }) {
                        Text("Импорт Расходов")
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                // 1. Если данные уже есть — сразу показываем список (без мерцания при переключении экранов)
                products.isNotEmpty() -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        importState?.let { status ->
                            Snackbar(
                                action = {
                                    TextButton(onClick = { viewModel.clearImportStatus() }) {
                                        Text("ОК")
                                    }
                                },
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Text(status)
                            }
                        }

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(products) { product ->
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = product.name, style = MaterialTheme.typography.bodyLarge)
                                        AssistChip(
                                            onClick = { },
                                            label = { Text(if (product.type == "PRODUCT") "Товар" else "Материал") }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                // 2. Если списка нет и идет первая загрузка — показываем индикатор
                isLoading -> {
                    CircularProgressIndicator()
                }
                // 3. Если загрузка завершена и товаров действительно нет — показываем текст
                else -> {
                    Text(
                        text = "Список товаров пуст",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Диалог разрешения конфликтов импорта
        viewModel.importPreviewState?.let { preview ->
            if (preview.conflicts.isNotEmpty()) {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissImportConflict() },
                    title = { Text("Обнаружены расхождения") },
                    text = {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                Text("В базе найдены записи за те же даты для тех же товаров, но с другими данными. Хотите обновить их?")
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            items(preview.conflicts) { conflict ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("Товар: ${conflict.productName}", style = MaterialTheme.typography.titleSmall)
                                        Text("Дата: ${conflict.date}")
                                        Text("Было: цена ${conflict.oldPrice}, кол-во ${conflict.oldQuantity}")
                                        Text("Стало: цена ${conflict.newPrice}, кол-во ${conflict.newQuantity}")
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            viewModel.confirmAndForceImport(preview)
                        }) {
                            Text("Подтвердить и обновить")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.dismissImportConflict() }) {
                            Text("Отмена")
                        }
                    }
                )
            }
        }
    }
}