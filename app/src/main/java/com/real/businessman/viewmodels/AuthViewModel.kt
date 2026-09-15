package com.real.businessman.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.real.businessman.UserRole
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class Authenticated(val role: UserRole) : AuthState()
    data class NeedsVerification(val email: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState

    // Таймер обратного отсчета в секундах для повторной отправки
    private val _resendCooldown = MutableStateFlow(0)
    val resendCooldown: StateFlow<Int> = _resendCooldown

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            currentUser.reload().addOnCompleteListener {
                if (currentUser.isEmailVerified) {
                    fetchUserRole(currentUser.uid)
                } else {
                    _authState.value = AuthState.NeedsVerification(currentUser.email ?: "")
                }
            }
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Заполните все поля")
            return
        }
        _authState.value = AuthState.Loading
        auth.signInWithEmailAndPassword(email.trim(), password.trim())
            .addOnSuccessListener { result ->
                val user = result.user
                if (user != null) {
                    if (user.isEmailVerified) {
                        fetchUserRole(user.uid)
                    } else {
                        _authState.value = AuthState.NeedsVerification(user.email ?: email)
                    }
                }
            }
            .addOnFailureListener { exception ->
                _authState.value = AuthState.Error(exception.localizedMessage ?: "Ошибка входа")
            }
    }

    fun signUp(email: String, password: String, defaultRole: UserRole = UserRole.WORKER) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Заполните все поля")
            return
        }
        if (password.length < 6) {
            _authState.value = AuthState.Error("Пароль должен содержать не менее 6 символов")
            return
        }
        _authState.value = AuthState.Loading
        auth.createUserWithEmailAndPassword(email.trim(), password.trim())
            .addOnSuccessListener { result ->
                val user = result.user ?: return@addOnSuccessListener
                val userMap = hashMapOf(
                    "email" to email,
                    "role" to defaultRole.name
                )

                // Отправляем письмо и сразу запускаем таймер на 60 секунд
                user.sendEmailVerification()
                startResendCooldown(60)

                firestore.collection("users").document(user.uid)
                    .set(userMap)
                    .addOnSuccessListener {
                        _authState.value = AuthState.NeedsVerification(email)
                    }
                    .addOnFailureListener {
                        _authState.value = AuthState.Error("Не удалось сохранить профиль")
                    }
            }
            .addOnFailureListener { exception ->
                _authState.value = AuthState.Error(exception.localizedMessage ?: "Ошибка регистрации")
            }
    }

    // Повторная отправка письма с запуском кулдауна
    fun resendVerificationEmail(cooldownSeconds: Int = 60) {
        if (_resendCooldown.value > 0) return

        val user = auth.currentUser
        user?.sendEmailVerification()
        startResendCooldown(cooldownSeconds)
    }

    private fun startResendCooldown(seconds: Int) {
        viewModelScope.launch {
            _resendCooldown.value = seconds
            while (_resendCooldown.value > 0) {
                delay(1000L)
                _resendCooldown.value -= 1
            }
        }
    }

    fun checkVerificationStatus() {
        _authState.value = AuthState.Loading
        val user = auth.currentUser
        user?.reload()?.addOnCompleteListener { task ->
            if (task.isSuccessful && user.isEmailVerified) {
                fetchUserRole(user.uid)
            } else {
                _authState.value = AuthState.NeedsVerification(user?.email ?: "")
            }
        }
    }

    private fun fetchUserRole(uid: String) {
        val userRef = firestore.collection("users").document(uid)

        userRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val roleStr = document.getString("role")
                    val role = UserRole.fromString(roleStr)
                    _authState.value = AuthState.Authenticated(role)
                } else {
                    val email = auth.currentUser?.email ?: ""
                    val defaultRole = UserRole.WORKER

                    val userMap = hashMapOf(
                        "email" to email,
                        "role" to defaultRole.name
                    )

                    userRef.set(userMap)
                        .addOnSuccessListener {
                            _authState.value = AuthState.Authenticated(defaultRole)
                        }
                        .addOnFailureListener {
                            _authState.value = AuthState.Authenticated(UserRole.WORKER)
                        }
                }
            }
            .addOnFailureListener {
                _authState.value = AuthState.Authenticated(UserRole.WORKER)
            }
    }

    fun logout(onLoggedOut: () -> Unit = {}) {
        auth.signOut()
        _authState.value = AuthState.Unauthenticated
        onLoggedOut()
    }

    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Unauthenticated
        }
    }
}