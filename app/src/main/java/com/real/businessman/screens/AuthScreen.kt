package com.real.businessman.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.real.businessman.viewmodels.AuthState
import com.real.businessman.viewmodels.AuthViewModel

@Composable
fun AuthScreen(authViewModel: AuthViewModel) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()

    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Анимация смены содержимого при переключении режима
        AnimatedContent(
            targetState = isLoginMode,
            transitionSpec = {
                // Плавное появление со сдвигом в зависимости от направления
                if (targetState) {
                    (slideInHorizontally { width -> -width } + fadeIn(animationSpec = tween(300))) togetherWith
                            (slideOutHorizontally { width -> width } + fadeOut(animationSpec = tween(300)))
                } else {
                    (slideInHorizontally { width -> width } + fadeIn(animationSpec = tween(300))) togetherWith
                            (slideOutHorizontally { width -> -width } + fadeOut(animationSpec = tween(300)))
                }
            },
            label = "AuthModeTransition"
        ) { targetIsLoginMode ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (targetIsLoginMode) "Вход в систему" else "Регистрация",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                TextField(
                    value = email,
                    onValueChange = {
                        email = it
                        authViewModel.clearError()
                    },
                    label = { Text("Email") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = productTextFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )

                TextField(
                    value = password,
                    onValueChange = {
                        password = it
                        authViewModel.clearError()
                    },
                    label = { Text("Пароль") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = productTextFieldColors(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )

                if (authState is AuthState.Error) {
                    Text(
                        text = (authState as AuthState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (authState is AuthState.Loading) {
                    CircularProgressIndicator()
                } else {
                    Button(
                        onClick = {
                            if (targetIsLoginMode) {
                                authViewModel.login(email, password)
                            } else {
                                authViewModel.signUp(email, password)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (targetIsLoginMode) "Войти" else "Зарегистрироваться")
                    }

                    TextButton(
                        onClick = {
                            isLoginMode = !isLoginMode
                            authViewModel.clearError()
                        }
                    ) {
                        Text(
                            if (targetIsLoginMode) "Нет аккаунта? Зарегистрироваться"
                            else "Уже есть аккаунт? Войти"
                        )
                    }
                }
            }
        }
    }
}