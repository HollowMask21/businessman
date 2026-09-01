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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    userRole: UserRole,
    viewModel: ProductViewModel
) {
    val context = LocalContext.current

    val products by viewModel.products.collectAsStateWithLifecycle()
    val importState by viewModel.importState.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    var selectedType by remember { mutableStateOf("SALE") }
    var showAddProductDialog by remember { mutableStateOf(false) }

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

                // Кнопки импорта из Excel доступны только Администратору (canImportExcel)
                if (userRole.canImportExcel) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                selectedType = "SALE"
                                filePickerLauncher.launch(
                                    arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Импорт Продаж")
                        }

                        Button(
                            onClick = {
                                selectedType = "PURCHASE"
                                filePickerLauncher.launch(
                                    arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Импорт Расходов")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Добавление позиций вручную доступно всем ролям (ADMIN и WORKER)
                FilledTonalButton(
                    onClick = { showAddProductDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("+ Добавить позицию вручную")
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
                isLoading -> {
                    CircularProgressIndicator()
                }
                else -> {
                    Text(
                        text = "Список товаров пуст",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Диалог создания новой записи доступен и для WORKER, и для ADMIN
        if (showAddProductDialog) {
            var productName by remember { mutableStateOf("") }
            var productType by remember { mutableStateOf("PRODUCT") }
            var isDropdownExpanded by remember { mutableStateOf(false) }

            val typeOptions = listOf("PRODUCT" to "Товар", "MATERIAL" to "Материал")

            val isDuplicate = products.any {
                it.name.trim().equals(productName.trim(), ignoreCase = true)
            }

            AlertDialog(
                onDismissRequest = { showAddProductDialog = false },
                title = { Text("Новая запись в каталоге") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = productName,
                            onValueChange = { productName = it },
                            label = { Text("Наименование") },
                            singleLine = true,
                            isError = isDuplicate,
                            supportingText = {
                                if (isDuplicate) {
                                    Text(
                                        text = "Позиция с таким названием уже есть в базе",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        ExposedDropdownMenuBox(
                            expanded = isDropdownExpanded,
                            onExpandedChange = { isDropdownExpanded = !isDropdownExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = typeOptions.find { it.first == productType }?.second ?: "Товар",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Тип записи") },
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
                                typeOptions.forEach { (typeKey, typeLabel) ->
                                    DropdownMenuItem(
                                        text = { Text(typeLabel) },
                                        onClick = {
                                            productType = typeKey
                                            isDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (productName.isNotBlank() && !isDuplicate) {
                                viewModel.addProduct(productName, productType)
                                showAddProductDialog = false
                            }
                        },
                        enabled = productName.isNotBlank() && !isDuplicate
                    ) {
                        Text("Создать")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddProductDialog = false }) {
                        Text("Отмена")
                    }
                }
            )
        }

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