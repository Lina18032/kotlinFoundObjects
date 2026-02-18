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
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: User? = null,
    val myLostItems: List<LostItem> = emptyList(),
    val myFoundItems: List<LostItem> = emptyList(),
    val selectedTab: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showEditDialog: Boolean = false,
    val onLogoutComplete: Boolean = false

)

class ProfileViewModel : ViewModel() {

    private val firebaseService = FirebaseService()
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val currentUser = firebaseService.getCurrentUser()
                if (currentUser == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Not logged in"
                    )
                    return@launch
                }

                // Load user data
                val userResult = firebaseService.getCurrentUserData()
                val user = userResult.getOrElse {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = it.message ?: "Failed to load user data"
                    )
                    return@launch
                }

                // Load user's items
                val itemsResult = firebaseService.getUserLostItems(currentUser.uid)
                val allItems = itemsResult.getOrElse {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = it.message ?: "Failed to load items"
                    )
                    return@launch
                }

                val lostItems = allItems.filter { it.status == ItemStatus.LOST }
                val foundItems = allItems.filter { it.status == ItemStatus.FOUND }

                _uiState.value = _uiState.value.copy(
                    user = user,
                    myLostItems = lostItems,
                    myFoundItems = foundItems,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load profile"
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

    fun logout() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(onLogoutComplete = true)
        }
    }

    fun onLogoutComplete() {
        _uiState.value = _uiState.value.copy(onLogoutComplete = false)
    }

    fun refreshProfile() {
        loadProfile()
    }
}