package com.real.businessman

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.real.businessman.database.FirebaseRepository
import com.real.businessman.screens.AuthScreen
import com.real.businessman.screens.HomeScreen
import com.real.businessman.screens.ProductsScreen
import com.real.businessman.screens.ProfileScreen
import com.real.businessman.screens.TransactionsScreen
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
            BusinessmanTheme {
                val authState by authViewModel.authState.collectAsStateWithLifecycle()

                when (val state = authState) {
                    is AuthState.Authenticated -> {
                        MainAppContent(
                            userRole = state.role,
                            productViewModel = productViewModel,
                            transactionViewModel = transactionViewModel,
                            authViewModel = authViewModel // Передаем authViewModel вместо onLogout
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
    authViewModel: AuthViewModel
) {
    var selectedTab by remember { mutableIntStateOf(0) }

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
            when (selectedTab) {
                0 -> HomeScreen(
                    transactionViewModel = transactionViewModel,
                    onNavigateToTransactions = { selectedTab = 2 }
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
                    authViewModel = authViewModel
                )
            }
        }
    }
}