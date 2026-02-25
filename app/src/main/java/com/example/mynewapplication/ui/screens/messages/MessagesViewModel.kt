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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.Job

data class MessagesUiState(
    val conversations: List<ChatConversation> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class MessagesViewModel : ViewModel() {

    private val firebaseService = FirebaseService()
    private val _uiState = MutableStateFlow(MessagesUiState())
    val uiState: StateFlow<MessagesUiState> = _uiState.asStateFlow()

    private var listenerJob: Job? = null

    init {
        startListening()
    }

    private fun startListening() {
        listenerJob?.cancel()
        val currentUser = firebaseService.getCurrentUser()
        if (currentUser == null) {
            _uiState.value = _uiState.value.copy(error = "Not logged in")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true)
        
        listenerJob = viewModelScope.launch {
            firebaseService.listenToConversations(currentUser.uid)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to listen to conversations"
                    )
                }
                .collect { conversations ->
                    _uiState.value = _uiState.value.copy(
                        conversations = conversations,
                        isLoading = false
                    )
                }
        }
    }

    private fun loadConversations() {
        startListening()
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