package com.real.businessman.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.real.businessman.UserRole
import com.real.businessman.database.Product
import com.real.businessman.viewmodels.ProductViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    userRole: UserRole,
    viewModel: ProductViewModel
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val products by viewModel.products.collectAsStateWithLifecycle()
    val importState by viewModel.importState.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(importState) {
        importState?.let { status ->
            snackbarHostState.showSnackbar(
                message = status,
                duration = SnackbarDuration.Short
            )
            viewModel.clearImportStatus()
        }
    }

    var selectedType by remember { mutableStateOf("SALE") }
    var showAddProductDialog by remember { mutableStateOf(false) }

    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }

    val typeOptions = listOf(
        "PRODUCT" to "Товар",
        "MATERIAL" to "Материал",
        "OTHER" to "Другое"
    )

    fun getProductTypeLabel(typeKey: String): String {
        return typeOptions.find { it.first == typeKey }?.second ?: "Другое"
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importExcelFile(context, it, selectedType) }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Surface(
                    modifier = Modifier
                        .wrapContentSize()
                        .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh, // Светлый фон
                    contentColor = MaterialTheme.colorScheme.onSurface,     // Тёмный текст
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
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Управление каталогом (Firebase)", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))

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
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(products, key = { it.id }) { product ->
                            var isMenuExpanded by remember { mutableStateOf(false) }
                            var pressOffset by remember { mutableStateOf(DpOffset.Zero) }

                            Box(modifier = Modifier.fillMaxWidth()) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onLongPress = { offset ->
                                                    pressOffset = DpOffset(
                                                        x = with(density) { offset.x.toDp() },
                                                        y = with(density) { offset.y.toDp() }
                                                    )
                                                    isMenuExpanded = true
                                                }
                                            )
                                        }
                                ) {
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
                                            label = { Text(getProductTypeLabel(product.type)) }
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = isMenuExpanded,
                                    onDismissRequest = { isMenuExpanded = false },
                                    offset = pressOffset
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Изменить") },
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                        onClick = {
                                            isMenuExpanded = false
                                            productToEdit = product
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Удалить", color = MaterialTheme.colorScheme.error) },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        },
                                        onClick = {
                                            isMenuExpanded = false
                                            productToDelete = product
                                        }
                                    )
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

        // Диалог создания
        if (showAddProductDialog) {
            var productName by remember { mutableStateOf("") }
            var productType by remember { mutableStateOf("PRODUCT") }
            var isDropdownExpanded by remember { mutableStateOf(false) }

            val isDuplicate = products.any { it.name.trim().equals(productName.trim(), ignoreCase = true) }

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
                                    Text("Позиция с таким названием уже есть в базе", color = MaterialTheme.colorScheme.error)
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
                                value = getProductTypeLabel(productType),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Тип записи") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                modifier = Modifier.menuAnchor().fillMaxWidth()
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

        // Диалог редактирования
        productToEdit?.let { targetProduct ->
            var updatedName by remember { mutableStateOf(targetProduct.name) }
            var updatedType by remember { mutableStateOf(targetProduct.type) }
            var isDropdownExpanded by remember { mutableStateOf(false) }

            val isDuplicate = products.any {
                it.id != targetProduct.id && it.name.trim().equals(updatedName.trim(), ignoreCase = true)
            }

            AlertDialog(
                onDismissRequest = { productToEdit = null },
                title = { Text("Редактировать позицию") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = updatedName,
                            onValueChange = { updatedName = it },
                            label = { Text("Наименование") },
                            singleLine = true,
                            isError = isDuplicate,
                            supportingText = {
                                if (isDuplicate) {
                                    Text("Позиция с таким названием уже есть в базе", color = MaterialTheme.colorScheme.error)
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
                                value = getProductTypeLabel(updatedType),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Тип записи") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )

                            ExposedDropdownMenu(
                                expanded = isDropdownExpanded,
                                onDismissRequest = { isDropdownExpanded = false }
                            ) {
                                typeOptions.forEach { (typeKey, typeLabel) ->
                                    DropdownMenuItem(
                                        text = { Text(typeLabel) },
                                        onClick = {
                                            updatedType = typeKey
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
                            if (updatedName.isNotBlank() && !isDuplicate) {
                                viewModel.updateProduct(targetProduct.id, updatedName, updatedType)
                                productToEdit = null
                            }
                        },
                        enabled = updatedName.isNotBlank() && !isDuplicate
                    ) {
                        Text("Сохранить")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { productToEdit = null }) {
                        Text("Отмена")
                    }
                }
            )
        }

        // Диалог удаления
        productToDelete?.let { targetProduct ->
            AlertDialog(
                onDismissRequest = { productToDelete = null },
                title = { Text("Удалить позицию?") },
                text = { Text("Вы действительно хотите удалить «${targetProduct.name}» из каталога?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteProduct(targetProduct.id, targetProduct.name)
                            productToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Удалить")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { productToDelete = null }) {
                        Text("Отмена")
                    }
                }
            )
        }

        // Диалог конфликтов импорта
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
                        Button(onClick = { viewModel.confirmAndForceImport(preview) }) {
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