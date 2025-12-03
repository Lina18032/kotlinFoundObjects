package com.example.mynewapplication.ui.screens.profile


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mynewapplication.data.model.LostItem
import com.example.mynewapplication.data.model.ItemStatus
import com.example.mynewapplication.data.model.User
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
    val showEditDialog: Boolean = false
)

class ProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Sample user data - will be replaced with Firebase later
            val sampleUser = User(
                id = "current_user",
                name = "Ahmed Benali",
                email = "ahmed.benali@estin.dz",
                profileImageUrl = null,
                phoneNumber = "+213 555 123 456"
            )

            // Sample items posted by user
            val myLostItems = listOf(
                LostItem(
                    id = "my1",
                    title = "Black Backpack",
                    description = "Lost my Nike backpack in the cafeteria",
                    category = com.example.mynewapplication.data.model.Category.BAGS,
                    location = "Cafeteria",
                    timestamp = System.currentTimeMillis() - 86400000,
                    status = ItemStatus.LOST,
                    userId = "current_user",
                    userName = "Ahmed Benali",
                    userEmail = "ahmed.benali@estin.dz"
                ),
                LostItem(
                    id = "my2",
                    title = "USB Flash Drive",
                    description = "32GB SanDisk USB with important files",
                    category = com.example.mynewapplication.data.model.Category.ELECTRONICS,
                    location = "Computer Lab 2",
                    timestamp = System.currentTimeMillis() - 172800000,
                    status = ItemStatus.LOST,
                    userId = "current_user",
                    userName = "Ahmed Benali",
                    userEmail = "ahmed.benali@estin.dz"
                )
            )

            val myFoundItems = listOf(
                LostItem(
                    id = "my3",
                    title = "Student Card",
                    description = "Found a student card in Amphi 3",
                    category = com.example.mynewapplication.data.model.Category.CARDS,
                    location = "Amphi 3",
                    timestamp = System.currentTimeMillis() - 43200000,
                    status = ItemStatus.FOUND,
                    userId = "current_user",
                    userName = "Ahmed Benali",
                    userEmail = "ahmed.benali@estin.dz"
                )
            )

            _uiState.value = _uiState.value.copy(
                user = sampleUser,
                myLostItems = myLostItems,
                myFoundItems = myFoundItems,
                isLoading = false
            )
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
            // TODO: Update in Firebase
            val updatedUser = _uiState.value.user?.copy(
                name = name,
                phoneNumber = phoneNumber
            )

            _uiState.value = _uiState.value.copy(
                user = updatedUser,
                showEditDialog = false
            )
        }
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            // TODO: Delete from Firebase
            _uiState.value = _uiState.value.copy(
                myLostItems = _uiState.value.myLostItems.filter { it.id != itemId },
                myFoundItems = _uiState.value.myFoundItems.filter { it.id != itemId }
            )
        }
    }

    fun logout(onLogoutComplete: () -> Unit) {
        viewModelScope.launch {
            // TODO: Sign out from Firebase
            onLogoutComplete()
        }
    }

    fun refreshProfile() {
        loadProfile()
    }
}