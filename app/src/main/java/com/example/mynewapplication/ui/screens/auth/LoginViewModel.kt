package com.example.mynewapplication.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mynewapplication.utils.ValidationUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

data class LoginUiState(
    val email: String = "",
    val emailError: String? = null,
    val password: String = "",
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val isGoogleSignInLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false
)

class LoginViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.value = _uiState.value.copy(
            email = email,
            emailError = null,
            errorMessage = null
        )
    }

    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(
            password = password,
            passwordError = null,
            errorMessage = null
        )
    }

    fun signInWithEmail(onSuccess: () -> Unit) {
        // Validate fields
        val emailValidation = ValidationUtils.validateEmail(_uiState.value.email)
        val passwordValidation = ValidationUtils.validatePassword(_uiState.value.password)

        if (!emailValidation.isValid || !passwordValidation.isValid) {
            _uiState.value = _uiState.value.copy(
                emailError = emailValidation.errorMessage,
                passwordError = passwordValidation.errorMessage
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                // TODO: Firebase Authentication
                // Simulate network call
                delay(1500)

                // For now, just check if email ends with @estin.dz
                if (!_uiState.value.email.endsWith("@estin.dz")) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Only @estin.dz emails are allowed"
                    )
                    return@launch
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoggedIn = true
                )

                onSuccess()

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Login failed. Please try again."
                )
            }
        }
    }

    fun signInWithGoogle(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isGoogleSignInLoading = true,
                errorMessage = null
            )

            try {
                // TODO: Firebase Google Sign-In
                // Simulate network call
                delay(2000)

                // Simulate successful Google Sign-In
                _uiState.value = _uiState.value.copy(
                    isGoogleSignInLoading = false,
                    isLoggedIn = true
                )

                onSuccess()

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGoogleSignInLoading = false,
                    errorMessage = "Google Sign-In failed. Please try again."
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}