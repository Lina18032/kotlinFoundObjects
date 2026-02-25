package com.example.mynewapplication.ui.screens.profile


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mynewapplication.data.model.LostItem
import com.example.mynewapplication.data.model.ItemStatus
import com.example.mynewapplication.data.model.User
import com.example.mynewapplication.data.remote.FirebaseService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: User? = null,
    val myLostItems: List<LostItem> = emptyList(),
    val myFoundItems: List<LostItem> = emptyList(),
    val selectedTab: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showEditDialog: Boolean = false,
    val showPasswordDialog: Boolean = false,
    val passwordSuccess: String? = null,
    val passwordError: String? = null,
    val hasPasswordProvider: Boolean = true
)

class ProfileViewModel : ViewModel() {

    private val firebaseService = FirebaseService()
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    private var itemsJob: kotlinx.coroutines.Job? = null

    init {
        loadProfile()
    }

    private var profileJob: kotlinx.coroutines.Job? = null

    private fun loadProfile() {
        profileJob?.cancel()
        profileJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val currentUser = firebaseService.getCurrentUser()
            if (currentUser == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Not logged in"
                )
                return@launch
            }

            // Start real-time listener for items
            startItemsListener(currentUser.uid)

            // Start real-time listener for user data
            firebaseService.listenToUser(currentUser.uid).collect { userFromDb ->
                val user: User = if (userFromDb != null) {
                    userFromDb.copy(
                        name = if (userFromDb.name.isNotBlank()) userFromDb.name else currentUser.displayName ?: userFromDb.email.substringBefore("@"),
                        email = if (userFromDb.email.isNotBlank()) userFromDb.email else currentUser.email ?: ""
                    )
                } else {
                    User(
                        id = currentUser.uid,
                        name = currentUser.displayName ?: (currentUser.email?.substringBefore("@") ?: ""),
                        email = currentUser.email ?: ""
                    )
                }

                _uiState.value = _uiState.value.copy(
                    user = user,
                    hasPasswordProvider = firebaseService.hasPasswordProvider(),
                    isLoading = false
                )
            }
        }
    }

    private fun startItemsListener(userId: String) {
        itemsJob?.cancel()
        itemsJob = viewModelScope.launch {
            firebaseService.listenToUserLostItems(userId).collect { allItems ->
                val lostItems = allItems.filter { it.status == ItemStatus.LOST }
                val foundItems = allItems.filter { it.status == ItemStatus.FOUND }

                _uiState.value = _uiState.value.copy(
                    myLostItems = lostItems,
                    myFoundItems = foundItems,
                    isLoading = false
                )
            }
        }
    }

    fun onTabSelected(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }

    fun showEditDialog() {
        _uiState.value = _uiState.value.copy(showEditDialog = true)
    }

    fun hideEditDialog() {
        _uiState.value = _uiState.value.copy(showEditDialog = false)
    }

    fun showPasswordDialog() {
        _uiState.value = _uiState.value.copy(
            showPasswordDialog = true, 
            passwordSuccess = null, 
            passwordError = null,
            hasPasswordProvider = firebaseService.hasPasswordProvider()
        )
    }

    fun hidePasswordDialog() {
        _uiState.value = _uiState.value.copy(showPasswordDialog = false)
    }

    fun clearPasswordMessages() {
        _uiState.value = _uiState.value.copy(passwordSuccess = null, passwordError = null)
    }

    fun updateProfile(name: String, phoneNumber: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val currentUser = firebaseService.getCurrentUser()
            if (currentUser == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Not logged in"
                )
                return@launch
            }

            try {
                val result = firebaseService.updateUserProfile(
                    currentUser.uid,
                    name,
                    phoneNumber.ifEmpty { null }
                )

                result.fold(
                    onSuccess = {
                        val updatedUser = _uiState.value.user?.copy(
                            name = name,
                            phoneNumber = phoneNumber.ifEmpty { null }
                        )
                        _uiState.value = _uiState.value.copy(
                            user = updatedUser,
                            showEditDialog = false,
                            isLoading = false
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to update profile"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to update profile"
                )
            }
        }
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = firebaseService.deleteLostItem(itemId)
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            myLostItems = _uiState.value.myLostItems.filter { it.id != itemId },
                            myFoundItems = _uiState.value.myFoundItems.filter { it.id != itemId },
                            isLoading = false
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to delete item"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to delete item"
                )
            }
        }
    }

    fun logout(context: android.content.Context, onLogoutComplete: () -> Unit) {
        viewModelScope.launch {
            firebaseService.signOut(context)
            onLogoutComplete()
        }
    }

    fun refreshProfile() {
        loadProfile()
    }

    fun changePassword(currentPassword: String?, newPassword: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, passwordError = null, passwordSuccess = null)

            try {
                val result = firebaseService.updatePassword(currentPassword, newPassword)
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            showPasswordDialog = false,
                            passwordSuccess = "Password updated successfully!",
                            hasPasswordProvider = true
                        )
                    },
                    onFailure = { error ->
                        val errorMessage = when {
                            error.message?.contains("password") == true -> "Incorrect current password"
                            else -> error.message ?: "Failed to update password"
                        }
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            passwordError = errorMessage
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    passwordError = e.message ?: "Failed to update password"
                )
            }
        }
    }
}
