package com.example.mynewapplication.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mynewapplication.data.remote.FirebaseService
import com.example.mynewapplication.utils.ValidationUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    private val firebaseService = FirebaseService()
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

        // Check if email ends with @estin.dz
        if (!_uiState.value.email.endsWith("@estin.dz")) {
            _uiState.value = _uiState.value.copy(
                emailError = "Only @estin.dz emails are allowed"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                val result = firebaseService.signInWithEmailAndPassword(
                    _uiState.value.email,
                    _uiState.value.password
                )

                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isLoggedIn = true
                        )
                        onSuccess()
                    },
                    onFailure = { error ->
                        val errorMessage = when {
                            error.message?.contains("password") == true -> 
                                "Invalid password. Please try again."
                            error.message?.contains("user") == true || 
                            error.message?.contains("email") == true -> 
                                "No account found with this email. Please sign up first."
                            error.message?.contains("network") == true -> 
                                "Network error. Please check your connection."
                            else -> error.message ?: "Login failed. Please try again."
                        }
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = errorMessage
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Login failed. Please try again."
                )
            }
        }
    }

    /**
     * Handle Google Sign-In result
     * This is called after Google Sign-In activity returns
     */
    fun handleGoogleSignInResult(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount?, onSuccess: () -> Unit) {
        if (account == null) {
            _uiState.value = _uiState.value.copy(
                isGoogleSignInLoading = false,
                errorMessage = "Google Sign-In was cancelled"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isGoogleSignInLoading = true,
                errorMessage = null
            )

            try {
                // Check if email ends with @estin.dz
                if (account.email?.endsWith("@estin.dz") != true) {
                    _uiState.value = _uiState.value.copy(
                        isGoogleSignInLoading = false,
                        errorMessage = "Only @estin.dz emails are allowed"
                    )
                    return@launch
                }

                val result = firebaseService.signInWithGoogle(account)

                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            isGoogleSignInLoading = false,
                            isLoggedIn = true
                        )
                        onSuccess()
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isGoogleSignInLoading = false,
                            errorMessage = error.message ?: "Google Sign-In failed. Please try again."
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGoogleSignInLoading = false,
                    errorMessage = e.message ?: "Google Sign-In failed. Please try again."
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}