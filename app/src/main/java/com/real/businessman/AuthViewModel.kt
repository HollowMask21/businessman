package com.real.businessman

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed class AuthState {
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class Authenticated(val role: UserRole) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            fetchUserRole(currentUser.uid)
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
                result.user?.uid?.let { fetchUserRole(it) }
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
                val uid = result.user?.uid ?: return@addOnSuccessListener
                val userMap = hashMapOf(
                    "email" to email,
                    "role" to defaultRole.name
                )

                // Сохраняем информацию о роли в Firestore
                firestore.collection("users").document(uid)
                    .set(userMap)
                    .addOnSuccessListener {
                        _authState.value = AuthState.Authenticated(defaultRole)
                    }
                    .addOnFailureListener {
                        _authState.value = AuthState.Error("Не удалось сохранить профиль")
                    }
            }
            .addOnFailureListener { exception ->
                _authState.value = AuthState.Error(exception.localizedMessage ?: "Ошибка регистрации")
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
                    // Если документа еще нет в Firestore, создаем его со значением по умолчанию (например, ADMIN для первого входа)
                    val email = auth.currentUser?.email ?: ""
                    val defaultRole = UserRole.WORKER // Укажите ADMIN или WORKER

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

    fun logout() {
        auth.signOut()
        _authState.value = AuthState.Unauthenticated
    }

    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Unauthenticated
        }
    }
}