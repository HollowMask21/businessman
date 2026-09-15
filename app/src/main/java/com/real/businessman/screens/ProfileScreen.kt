package com.real.businessman.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.real.businessman.viewmodels.AuthViewModel
import com.real.businessman.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userRole: UserRole,
    authViewModel: AuthViewModel,
    isDarkTheme: Boolean,
    onThemeChanged: (Boolean) -> Unit
) {
    val currentUser = authViewModel.currentUser
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Единый цвет фона для всех блоков
    val blockContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Профиль и Аккаунт", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                windowInsets = WindowInsets(0)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Карточка пользователя
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = blockContainerColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Аватар",
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentUser?.email ?: "Неизвестный пользователь",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val (roleName, roleColor) = when (userRole) {
                            UserRole.ADMIN -> "Администратор" to MaterialTheme.colorScheme.primary
                            UserRole.WORKER -> "Сотрудник" to MaterialTheme.colorScheme.secondary
                        }

                        // Некликабельный бейдж роли
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = roleColor.copy(alpha = 0.12f),
                            contentColor = roleColor
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = roleColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = roleName,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = roleColor
                                )
                            }
                        }
                    }
                }
            }

            // Описание возможностей (перемещено выше)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = blockContainerColor)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Доступные возможности:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text("• Создание, редактирование и удаление операций", style = MaterialTheme.typography.bodyMedium)
                    Text("• Управление товарами и материалами", style = MaterialTheme.typography.bodyMedium)

                    if (userRole.canImportExcel) {
                        Text("• Массовый импорт данных из Excel (ADMIN)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    } else {
                        Text("• Массовый импорт Excel недоступен для вашей роли", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }
                }
            }

            // Блок переключения темы (перемещен ниже)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = blockContainerColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Темная тема",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = onThemeChanged
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Прозрачная кнопка выхода с приятным красным текстом
            val redColor = Color(0xFFE53935)

            OutlinedButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                    brush = androidx.compose.ui.graphics.SolidColor(redColor.copy(alpha = 0.2f))
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = redColor.copy(alpha = 0.05f),
                    contentColor = redColor
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Выйти",
                    tint = redColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Выйти из аккаунта",
                    fontWeight = FontWeight.SemiBold,
                    color = redColor
                )
            }
        }

        // Диалог подтверждения выхода
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Выход из системы") },
                text = { Text("Вы действительно хотите выйти из текущего аккаунта?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showLogoutDialog = false
                            authViewModel.logout()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Выйти")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Отмена")
                    }
                }
            )
        }
    }
}