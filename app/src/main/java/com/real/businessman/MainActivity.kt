package com.real.businessman

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.real.businessman.database.AppDatabase
import com.real.businessman.ui.theme.BusinessmanTheme

class MainActivity : ComponentActivity() {

    // Инициализация ViewModel с передачей ProductDao из базы данных
    private val productViewModel: ProductViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = AppDatabase.getDatabase(applicationContext)
                @Suppress("UNCHECKED_CAST")
                return ProductViewModel(db.productDao()) as T
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