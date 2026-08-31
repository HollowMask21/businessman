package com.real.businessman

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.real.businessman.database.FirebaseRepository
import com.real.businessman.ui.theme.BusinessmanTheme

class MainActivity : ComponentActivity() {

    // Инициализация ViewModel с передачей FirebaseRepository
    private val productViewModel: ProductViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                // Создаем экземпляр репозитория Firebase вместо AppDatabase
                val repository = FirebaseRepository()

                @Suppress("UNCHECKED_CAST")
                return ProductViewModel(repository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BusinessmanTheme {
                ProductsScreen(viewModel = productViewModel)
            }
        }
    }
}