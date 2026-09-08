package com.real.businessman.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.real.businessman.UserRole
import com.real.businessman.database.Product
import com.real.businessman.viewmodels.ProductViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProductsScreen(
    userRole: UserRole,
    viewModel: ProductViewModel
) {
    val context = LocalContext.current

    val products by viewModel.products.collectAsStateWithLifecycle()
    val importState by viewModel.importState.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Множественный выбор
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    val isSelectionMode = selectedIds.isNotEmpty()
    var showDeleteMultipleDialog by remember { mutableStateOf(false) }

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
                // Контекстная панель режима выбора
                TopAppBar(
                    title = { Text("Выбрано: ${selectedIds.size}") },
                    navigationIcon = {
                        IconButton(onClick = { selectedIds = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Сбросить")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            selectedIds = if (selectedIds.size == products.size) {
                                emptySet()
                            } else {
                                products.map { it.id }.toSet()
                            }
                        }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Выбрать все")
                        }
                        IconButton(onClick = { showDeleteMultipleDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Удалить выбранные",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                )
            } else {
                // Обычный заголовок и кнопки импорта/добавления
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
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                products.isNotEmpty() -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(products, key = { it.id }) { product ->
                            val isSelected = selectedIds.contains(product.id)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            if (isSelectionMode) {
                                                selectedIds = if (isSelected) {
                                                    selectedIds - product.id
                                                } else {
                                                    selectedIds + product.id
                                                }
                                            }
                                        },
                                        onLongClick = {
                                            if (!isSelectionMode) {
                                                selectedIds = setOf(product.id)
                                            }
                                        }
                                    ),
                                colors = if (isSelected) {
                                    CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                } else {
                                    CardDefaults.cardColors()
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
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
                                                onCheckedChange = { checked ->
                                                    selectedIds = if (checked) {
                                                        selectedIds + product.id
                                                    } else {
                                                        selectedIds - product.id
                                                    }
                                                },
                                                modifier = Modifier.padding(end = 8.dp)
                                            )
                                        }
                                        Text(
                                            text = product.name,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        AssistChip(
                                            onClick = { },
                                            label = { Text(getProductTypeLabel(product.type)) }
                                        )

                                        if (!isSelectionMode) {
                                            IconButton(onClick = { productToEdit = product }) {
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = "Изменить",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
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

        // Диалог массового удаления
        if (showDeleteMultipleDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteMultipleDialog = false },
                title = { Text("Удалить выбранные позиции?") },
                text = { Text("Вы действительно хотите удалить позиции в количестве: ${selectedIds.size} шт.?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteSelectedProducts(selectedIds)
                            selectedIds = emptySet()
                            showDeleteMultipleDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Удалить")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteMultipleDialog = false }) {
                        Text("Отмена")
                    }
                }
            )
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
    }
}