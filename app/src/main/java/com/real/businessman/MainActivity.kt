package com.real.businessman

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.real.businessman.database.FirebaseRepository
import com.real.businessman.screens.AddEditTransactionDialog
import com.real.businessman.screens.AuthScreen
import com.real.businessman.screens.HomeScreen
import com.real.businessman.screens.ProductsScreen
import com.real.businessman.screens.ProfileScreen
import com.real.businessman.screens.TransactionsScreen
import com.real.businessman.screens.productTextFieldColors
import com.real.businessman.ui.theme.BusinessmanTheme
import com.real.businessman.viewmodels.AuthState
import com.real.businessman.viewmodels.AuthViewModel
import com.real.businessman.viewmodels.ProductViewModel
import com.real.businessman.viewmodels.TransactionViewModel

class MainActivity : ComponentActivity() {

    private val repository by lazy { FirebaseRepository() }

    private val authViewModel: AuthViewModel by viewModels()

    private val productViewModel: ProductViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ProductViewModel(repository) as T
            }
        }
    }

    private val transactionViewModel: TransactionViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return TransactionViewModel(repository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var isDarkTheme by remember { mutableStateOf(false) } // Светлая тема по умолчанию

            BusinessmanTheme(darkTheme = isDarkTheme) {
                val authState by authViewModel.authState.collectAsStateWithLifecycle()

                when (val state = authState) {
                    is AuthState.Authenticated -> {
                        MainAppContent(
                            userRole = state.role,
                            productViewModel = productViewModel,
                            transactionViewModel = transactionViewModel,
                            authViewModel = authViewModel,
                            isDarkTheme = isDarkTheme,
                            onThemeChanged = { isDarkTheme = it }
                        )
                    }
                    is AuthState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    else -> {
                        AuthScreen(authViewModel = authViewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun MainAppContent(
    userRole: UserRole,
    productViewModel: ProductViewModel,
    transactionViewModel: TransactionViewModel,
    authViewModel: AuthViewModel,
    isDarkTheme: Boolean,
    onThemeChanged: (Boolean) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    var showAddProductDialog by remember { mutableStateOf(false) }
    var showAddTransactionDialog by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Главная") },
                    label = { Text("Главная") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Товары") },
                    label = { Text("Каталог") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.List, contentDescription = "Транзакции") },
                    label = { Text("Операции") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Профиль") },
                    label = { Text("Профиль") }
                )
            }
        }
    ) { paddingValues ->
        Surface(modifier = Modifier.padding(paddingValues)) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally(animationSpec = tween(300)) { width -> width } + fadeIn(animationSpec = tween(300))) togetherWith
                                (slideOutHorizontally(animationSpec = tween(300)) { width -> -width } + fadeOut(animationSpec = tween(300)))
                    } else {
                        (slideInHorizontally(animationSpec = tween(300)) { width -> -width } + fadeIn(animationSpec = tween(300))) togetherWith
                                (slideOutHorizontally(animationSpec = tween(300)) { width -> width } + fadeOut(animationSpec = tween(300)))
                    }
                },
                label = "ScreenTransition"
            ) { targetTab ->
                when (targetTab) {
                    0 -> HomeScreen(
                        userRole = userRole,
                        transactionViewModel = transactionViewModel,
                        productViewModel = productViewModel,
                        onNavigateToTransactions = { selectedTab = 2 },
                        onAddProductClick = { showAddProductDialog = true },
                        onAddTransactionClick = { showAddTransactionDialog = true }
                    )
                    1 -> ProductsScreen(
                        userRole = userRole,
                        viewModel = productViewModel
                    )
                    2 -> TransactionsScreen(
                        userRole = userRole,
                        transactionViewModel = transactionViewModel,
                        productViewModel = productViewModel
                    )
                    3 -> ProfileScreen(
                        userRole = userRole,
                        authViewModel = authViewModel,
                        isDarkTheme = isDarkTheme,
                        onThemeChanged = onThemeChanged
                    )
                }
            }
        }

        if (showAddProductDialog) {
            AddProductDialog(
                productViewModel = productViewModel,
                onDismiss = { showAddProductDialog = false },
                onSuccess = {
                    showAddProductDialog = false
                    selectedTab = 1
                }
            )
        }

        if (showAddTransactionDialog) {
            val products by productViewModel.products.collectAsStateWithLifecycle()
            AddEditTransactionDialog(
                products = products,
                onDismiss = { showAddTransactionDialog = false },
                onConfirm = { type, date, productName, quantity, price, comment ->
                    transactionViewModel.addManualTransaction(
                        type = type,
                        date = date,
                        productName = productName,
                        quantity = quantity,
                        pricePerUnit = price,
                        comment = comment
                    )
                    showAddTransactionDialog = false
                    selectedTab = 2
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductDialog(
    productViewModel: ProductViewModel,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val products by productViewModel.products.collectAsStateWithLifecycle()
    var productName by remember { mutableStateOf("") }
    var productType by remember { mutableStateOf("PRODUCT") }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    val typeOptions = listOf(
        "PRODUCT" to "Товар",
        "MATERIAL" to "Материал",
        "OTHER" to "Другое"
    )

    fun getProductTypeLabel(typeKey: String): String {
        return typeOptions.find { it.first == typeKey }?.second ?: "Другое"
    }

    val isDuplicate = products.any { it.name.trim().equals(productName.trim(), ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая запись в каталоге") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextField(
                    value = productName,
                    onValueChange = { productName = it },
                    label = { Text("Наименование") },
                    singleLine = true,
                    isError = isDuplicate,
                    shape = MaterialTheme.shapes.medium,
                    colors = productTextFieldColors(),
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
                    TextField(
                        value = getProductTypeLabel(productType),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Тип записи") },
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
                        colors = productTextFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false },
                        modifier = Modifier.clip(RoundedCornerShape(12.dp))
                    ) {
                        typeOptions.filter { it.first != productType }.forEach { (typeKey, typeLabel) ->
                            DropdownMenuItem(
                                text = { Text(typeLabel) },
                                onClick = {
                                    productType = typeKey
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
        },
        confirmButton = {
            Button(
                onClick = {
                    if (productName.isNotBlank() && !isDuplicate) {
                        productViewModel.addProduct(productName, productType)
                        onSuccess()
                    }
                },
                enabled = productName.isNotBlank() && !isDuplicate
            ) {
                Text("Создать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}