package com.real.businessman

enum class UserRole {
    ADMIN,
    WORKER;

    // Права на импорт файлов Excel
    val canImportExcel: Boolean
        get() = this == ADMIN

    companion object {
        fun fromString(role: String?): UserRole {
            return entries.find { it.name.equals(role, ignoreCase = true) } ?: WORKER
        }
    }
}