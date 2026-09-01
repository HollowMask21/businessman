package com.real.businessman

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.real.businessman.database.FirebaseExcelImporter
import com.real.businessman.database.FirebaseRepository
import com.real.businessman.database.Product
import com.real.businessman.database.ImportPreviewState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ProductViewModel(
    private val repository: FirebaseRepository
) : ViewModel() {

    // Подписываемся на изменения из Firestore
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    val products: StateFlow<List<Product>> = repository.getProductsFlow()
        .onEach {
            // Сбрасываем флаг загрузки, как только пришли новые данные из Firestore
            _isLoading.value = false
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _importState = MutableStateFlow<String?>(null)
    val importState: StateFlow<String?> = _importState

    // Состояние для показа окна сравнения конфликтов (дубликатов)
    var importPreviewState by mutableStateOf<ImportPreviewState?>(null)
        private set

    // Импорт файла с предварительной проверкой дубликатов
    fun importExcelFile(context: Context, uri: Uri, transactionType: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _importState.value = "Чтение файла и проверка..."

            val importer = FirebaseExcelImporter(repository)

            // Сначала парсим транзакции из Excel без мгновенной записи
            val parseResult = importer.parseExcelTransactions(context, uri, transactionType)

            parseResult.fold(
                onSuccess = { parsedTransactions ->
                    // Проверяем на конфликты с тем, что уже есть в базе
                    val preview = repository.checkConflictsAndPrepareImport(parsedTransactions)

                    if (preview.conflicts.isNotEmpty()) {
                        // Если есть конфликты, останавливаем процесс и показываем окно пользователю
                        importPreviewState = preview
                        _importState.value = "Найдены расхождения с существующими данными"
                    } else {
                        // Если конфликтов нет — сразу сохраняем чистые новые транзакции
                        repository.saveTransactions(preview.newTransactions)
                        _importState.value = "Успешно импортировано записей: ${preview.newTransactions.size}"
                    }
                },
                onFailure = { error ->
                    _importState.value = "Ошибка импорта: ${error.localizedMessage}"
                }
            )
        }
    }

    // Закрыть окно конфликтов без изменений
    fun dismissImportConflict() {
        importPreviewState = null
        _importState.value = null
    }

    // Подтвердить и принудительно обновить/дописать данные с учетом разрешения конфликтов
    fun confirmAndForceImport(preview: ImportPreviewState) {
        viewModelScope.launch(Dispatchers.IO) {
            _importState.value = "Обновление данных..."

            // Вызываем метод разрешения конфликтов и сохранения в репозитории
            repository.resolveConflictsAndSave(
                newTransactions = preview.newTransactions,
                conflictsToUpdate = preview.conflicts
            )

            importPreviewState = null
            _importState.value = "Данные успешно обновлены!"
        }
    }

    fun clearImportStatus() {
        _importState.value = null
    }
}