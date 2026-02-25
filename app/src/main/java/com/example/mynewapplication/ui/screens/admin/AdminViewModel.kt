package com.example.mynewapplication.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mynewapplication.data.model.LostItem
import com.example.mynewapplication.data.model.User
import com.example.mynewapplication.data.model.ChatConversation
import com.example.mynewapplication.data.remote.FirebaseService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AdminUiState(
    val items: List<LostItem> = emptyList(),
    val users: List<User> = emptyList(),
    val conversations: List<ChatConversation> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedTab: Int = 0 // 0: Items, 1: Users, 2: Messages
)

class AdminViewModel : ViewModel() {
    private val firebaseService = FirebaseService()
    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // Listen to all items
            launch {
                firebaseService.getAllItems().collect { items ->
                    _uiState.value = _uiState.value.copy(items = items, isLoading = false)
                }
            }
            
            // Listen to all users
            launch {
                firebaseService.getAllUsers().collect { users ->
                    _uiState.value = _uiState.value.copy(users = users)
                }
            }

            // Listen to all conversations
            launch {
                firebaseService.getAllConversations().collect { conversations ->
                    _uiState.value = _uiState.value.copy(conversations = conversations)
                }
            }
        }
    }

    fun onTabSelected(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            val result = firebaseService.deleteItem(itemId)
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(error = result.exceptionOrNull()?.message)
            }
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            val result = firebaseService.deleteConversation(conversationId)
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(error = result.exceptionOrNull()?.message)
            }
        }
    }

    fun toggleBlockUser(userId: String, currentBlockedStatus: Boolean) {
        viewModelScope.launch {
            val result = firebaseService.blockUser(userId, !currentBlockedStatus)
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(error = result.exceptionOrNull()?.message)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
