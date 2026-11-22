package com.example.ui.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AuthRepository
import com.example.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthStep { EMAIL_LOGIN, EMAIL_SIGNUP, PROFILE_SETUP }

data class AuthUiState(
    val step: AuthStep = AuthStep.EMAIL_LOGIN,
    val emailInput: String = "",
    val passwordInput: String = "",
    val confirmPasswordInput: String = "",
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val displayNameInput: String = "",
    val usernameInput: String = "",
    val aboutInput: String = "Hey there! I am using Alok Chat."
)

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun updateEmailInput(value: String) {
        _state.update { it.copy(emailInput = value.trim(), errorMessage = null, infoMessage = null) }
    }

    fun updatePasswordInput(value: String) {
        _state.update { it.copy(passwordInput = value, errorMessage = null, infoMessage = null) }
    }

    fun updateConfirmPasswordInput(value: String) {
        _state.update { it.copy(confirmPasswordInput = value, errorMessage = null, infoMessage = null) }
    }

    fun togglePasswordVisibility() {
        _state.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun switchStep(step: AuthStep) {
        _state.update {
            it.copy(
                step = step,
                confirmPasswordInput = "",
                errorMessage = null,
                infoMessage = null
            )
        }
    }

    fun updateDisplayName(value: String) {
        _state.update { it.copy(displayNameInput = value, errorMessage = null) }
    }

    fun updateUsername(value: String) {
        val clean = value.lowercase().filter { it.isLetterOrDigit() || it == '_' || it == '.' }
        _state.update { it.copy(usernameInput = clean, errorMessage = null) }
    }

    fun updateAbout(value: String) {
        _state.update { it.copy(aboutInput = value, errorMessage = null) }
    }

    fun enterProfileSetup(defaultUsername: String) {
        _state.update {
            if (it.step == AuthStep.PROFILE_SETUP) it
            else it.copy(
                step = AuthStep.PROFILE_SETUP,
                isLoading = false,
                usernameInput = it.usernameInput.ifBlank { defaultUsername },
                errorMessage = null,
                infoMessage = null
            )
        }
    }

    fun resetState() {
        _state.value = AuthUiState()
    }

    fun signInWithEmail() {
        val current = _state.value
        val email = current.emailInput.trim()
        val password = current.passwordInput
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _state.update { it.copy(errorMessage = "Please enter a valid email address.") }
            return
        }
        if (password.length < 6) {
            _state.update { it.copy(errorMessage = "Password must be at least 6 characters.") }
            return
        }
        _state.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
        viewModelScope.launch {
            val result = authRepository.signInWithEmail(email, password)
            result.onSuccess { user ->
                handleSignedInUser(
                    uid = user.uid,
                    email = user.email ?: email,
                    phone = user.phoneNumber.orEmpty()
                )
            }.onFailure { err ->
                _state.update {
                    it.copy(isLoading = false, errorMessage = err.message ?: "Sign in failed.")
                }
            }
        }
    }

    fun signUpWithEmail() {
        val current = _state.value
        val email = current.emailInput.trim()
        val password = current.passwordInput
        val confirm = current.confirmPasswordInput
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _state.update { it.copy(errorMessage = "Please enter a valid email address.") }
            return
        }
        if (password.length < 6) {
            _state.update { it.copy(errorMessage = "Password must be at least 6 characters.") }
            return
        }
        if (password != confirm) {
            _state.update { it.copy(errorMessage = "Passwords do not match.") }
            return
        }
        _state.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
        viewModelScope.launch {
            val result = authRepository.signUpWithEmail(email, password)
            result.onSuccess { user ->
                handleSignedInUser(
                    uid = user.uid,
                    email = user.email ?: email,
                    phone = user.phoneNumber.orEmpty()
                )
            }.onFailure { err ->
                _state.update {
                    it.copy(isLoading = false, errorMessage = err.message ?: "Could not create account.")
                }
            }
        }
    }

    fun sendPasswordReset() {
        val email = _state.value.emailInput.trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _state.update { it.copy(errorMessage = "Enter your email address first to reset your password.") }
            return
        }
        _state.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
        viewModelScope.launch {
            authRepository.sendPasswordReset(email)
                .onSuccess {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            infoMessage = "Password reset link sent to $email."
                        )
                    }
                }
                .onFailure { err ->
                    _state.update {
                        it.copy(isLoading = false, errorMessage = err.message ?: "Could not send reset email.")
                    }
                }
        }
    }

    private suspend fun handleSignedInUser(uid: String, email: String, phone: String) {
        val record = userRepository.ensureUserRecord(uid, email = email, phone = phone)
        record.onSuccess { profile ->
            userRepository.syncFcmToken(uid)
            if (profile.displayName.isBlank()) {
                _state.update {
                    it.copy(
                        step = AuthStep.PROFILE_SETUP,
                        isLoading = false,
                        usernameInput = profile.username,
                        errorMessage = null
                    )
                }
            } else {
                _state.update { it.copy(isLoading = false, errorMessage = null) }
            }
        }.onFailure { err ->
            _state.update { it.copy(isLoading = false, errorMessage = err.message) }
        }
    }

    fun completeProfileSetup(onCompleted: () -> Unit) {
        val current = _state.value
        val uid = authRepository.currentUser?.uid ?: return
        if (current.displayNameInput.trim().isEmpty()) {
            _state.update { it.copy(errorMessage = "Please enter your display name.") }
            return
        }
        if (current.usernameInput.trim().length < 3) {
            _state.update { it.copy(errorMessage = "Username must be at least 3 characters.") }
            return
        }
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val res = userRepository.updateProfile(
                uid = uid,
                displayName = current.displayNameInput,
                username = current.usernameInput,
                about = current.aboutInput
            )
            res.onSuccess {
                _state.update { it.copy(isLoading = false) }
                onCompleted()
            }.onFailure { err ->
                _state.update { it.copy(isLoading = false, errorMessage = err.message) }
            }
        }
    }
}
