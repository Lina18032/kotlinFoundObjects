package com.example.mynewapplication.ui.screens.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mynewapplication.data.model.ChatConversation
import com.example.mynewapplication.data.model.ChatMessage
import com.example.mynewapplication.data.remote.FirebaseService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MessagesUiState(
    val conversations: List<ChatConversation> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class MessagesViewModel : ViewModel() {

    private val firebaseService = FirebaseService()
    private val _uiState = MutableStateFlow(MessagesUiState())
    val uiState: StateFlow<MessagesUiState> = _uiState.asStateFlow()

    init {
        loadConversations()
    }

    private fun loadConversations() {
        val currentUser = firebaseService.getCurrentUser()
        if (currentUser == null) {
            _uiState.value = _uiState.value.copy(error = "Not logged in")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            try {
                val result = firebaseService.getUserConversations(currentUser.uid)
                result.fold(
                    onSuccess = { conversations ->
                        _uiState.value = _uiState.value.copy(
                            conversations = conversations,
                            isLoading = false,
                            error = null
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load conversations"
                        )
                    }
                )
            } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load conversations"
                    )
            }
        }
    }

    fun refreshConversations() {
        loadConversations()
    }

    fun markAsRead(conversationId: String) {
        viewModelScope.launch {
            val currentUser = firebaseService.getCurrentUser()
            if (currentUser != null) {
                firebaseService.markMessagesAsRead(conversationId, currentUser.uid)
            }
            
            val updatedConversations = _uiState.value.conversations.map { conversation ->
                if (conversation.id == conversationId) {
                    conversation.copy(
                        lastMessage = conversation.lastMessage?.copy(isRead = true)
                    )
                } else {
                    conversation
                }
            }
            _uiState.value = _uiState.value.copy(conversations = updatedConversations)
        }
    }
}
