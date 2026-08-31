package com.real.businessman

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.real.businessman.database.FirebaseExcelImporter
import com.real.businessman.database.FirebaseRepository
import com.real.businessman.database.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProductViewModel(
    private val repository: FirebaseRepository // Заменили локальную БД на репозиторий Firebase
) : ViewModel() {

    // Подписываемся на изменения из Firestore и конвертируем в StateFlow для Compose
    val products: StateFlow<List<Product>> = repository.getProductsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _importState = MutableStateFlow<String?>(null)
    val importState: StateFlow<String?> = _importState

    // Импорт файла в фоновом потоке (Dispatchers.IO)
    fun importExcelFile(context: Context, uri: Uri, transactionType: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _importState.value = "Синхронизация с облаком..."

            // Используем новый класс импортера под Firebase
            val importer = FirebaseExcelImporter(repository)
            val result = importer.importExcel(context, uri, transactionType)

            _importState.value = result.fold(
                onSuccess = { count -> "Успешно импортировано в Firebase: $count" },
                onFailure = { error -> "Ошибка импорта: ${error.localizedMessage}" }
            )
        }
    }

    fun clearImportStatus() {
        _importState.value = null
    }
}