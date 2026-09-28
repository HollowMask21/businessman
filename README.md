# Businessman 💼

**Businessman** — это мобильное Android-приложение для внутреннего управленческого и финансового учета, контроля продаж, закупок и анализа товарных остатков[cite: 1, 3]. Приложение оптимизировано для работы на планшетах (Android 14+, API 34)[cite: 3].

---

## 🚀 Основные возможности

- **Авторизация и роли:** Разграничение прав доступа для пользователей (`ADMIN`, `WORKER`)[cite: 1].
- **Управление каталогом:** Единый справочник товаров (`PRODUCT`) и сырья/материалов (`MATERIAL`)[cite: 1, 3].
- **Финансовый учет:** Регистрация продаж (`SALE`), закупок и расходов (`PURCHASE`/`EXPENSE`) с ручным вводом и детальными позициями[cite: 1, 3].
- **Умный импорт Excel:** Загрузка ведомостей `.xlsx` с автоматическим распознаванием колонок, нормализацией дат и разрешением конфликтов[cite: 1, 2].
- **Офлайн-режим:** Синхронизация данных с облаком и поддержка полноценной работы без сети[cite: 1].

---

## 🛠 Стек технологий и библиотека

- **Язык:** [Kotlin](https://kotlinlang.org/)[cite: 1]
- **Интерфейс:** [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)[cite: 1]
- **Архитектура:** Clean Architecture / MVVM (`ViewModel`, `StateFlow`, `Coroutines`)[cite: 1, 2]
- **Бэкенд и облако:** [Google Firebase](https://firebase.google.com/) (`Cloud Firestore`, `Firebase Auth`)[cite: 1]
- **Работа с файлами:** Apache POI (`poi-ooxml`) для парсинга `.xlsx`[cite: 2]
- **Целевая платформа:** Android 14+ (API Level 34)[cite: 3]

---

## 📁 Структура проекта

```text
com.real.businessman
├── database/            # Слой данных и работа с Firebase/Excel
│   ├── ExcelImporter.kt # Парсинг таблиц и нормализация данных
│   ├── FirebaseRepository.kt # Firestore Flow-слушатели и батч-запросы
│   ├── ImportConflictState.kt # Структуры для разрешения конфликтов
│   └── KotlinDataClasses.kt  # Модели (Product, Transaction и др.)
├── screens/             # UI-экраны Jetpack Compose
│   ├── AuthScreen.kt    # Авторизация и регистрация
│   ├── HomeScreen.kt    # Аналитический дашборд
│   ├── ProductsScreen.kt# Каталог товаров и вызов импорта
│   ├── ProfileScreen.kt # Управление профилем и ролями
│   └── TransactionsScreen.kt # Список и ввод транзакций
├── viewmodels/          # Бизнес-логика и управление состоянием
│   ├── AuthViewModel.kt # Авторизация и загрузка ролей
│   ├── ProductViewModel.kt # Управление товарами и импортом
│   └── TransactionViewModel.kt # Проведение операций и расчеты
├── MainActivity.kt      # Точка входа и навигация
└── UserRole.kt          # Перечисление ролей и проверка прав
```[cite: 1]

---

## 📊 Формат Excel для импорта

При импорте файла `.xlsx` приложение автоматически находит следующие ключевые столбцы[cite: 2]:
* **Товар / Материал / Наименование**[cite: 2]
* **Цена / Себестоимость**[cite: 2]
* **Количество / Объем**[cite: 2]
* **Сумма / Итого** *(опционально)*[cite: 2]
* **Дата** *(опционально)*[cite: 2]
