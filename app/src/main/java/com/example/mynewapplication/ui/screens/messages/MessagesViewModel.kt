package com.example.mynewapplication.ui.screens.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mynewapplication.data.model.ChatConversation
import com.example.mynewapplication.data.model.ChatMessage
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

    private val _uiState = MutableStateFlow(MessagesUiState())
    val uiState: StateFlow<MessagesUiState> = _uiState.asStateFlow()

    init {
        loadConversations()
    }

    private fun loadConversations() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Sample data - will be replaced with Firebase later
            val sampleConversations = listOf(
                ChatConversation(
                    id = "conv1",
                    itemId = "item1",
                    participants = listOf("current_user", "user1"),
                    lastMessage = ChatMessage(
                        id = "msg1",
                        conversationId = "conv1",
                        senderId = "user1",
                        senderName = "Lina Lolem",
                        text = "Yes, I still have them. When can you pick them up?",
                        timestamp = System.currentTimeMillis() - 3600000,
                        isRead = false
                    ),
                    updatedAt = System.currentTimeMillis() - 3600000
                ),
                ChatConversation(
                    id = "conv2",
                    itemId = "item2",
                    participants = listOf("current_user", "user2"),
                    lastMessage = ChatMessage(
                        id = "msg2",
                        conversationId = "conv2",
                        senderId = "current_user",
                        senderName = "Me",
                        text = "Thank you so much! I'll come by tomorrow.",
                        timestamp = System.currentTimeMillis() - 86400000,
                        isRead = true
                    ),
                    updatedAt = System.currentTimeMillis() - 86400000
                ),
                ChatConversation(
                    id = "conv3",
                    itemId = "item3",
                    participants = listOf("current_user", "user3"),
                    lastMessage = ChatMessage(
                        id = "msg3",
                        conversationId = "conv3",
                        senderId = "user3",
                        senderName = "Ahmed Kaci",
                        text = "Are you sure it's yours? Can you describe it?",
                        timestamp = System.currentTimeMillis() - 172800000,
                        isRead = true
                    ),
                    updatedAt = System.currentTimeMillis() - 172800000
                )
            )

            _uiState.value = _uiState.value.copy(
                conversations = sampleConversations.sortedByDescending { it.updatedAt },
                isLoading = false
            )
        }
    }

    fun refreshConversations() {
        loadConversations()
    }

    fun markAsRead(conversationId: String) {
        viewModelScope.launch {
            // TODO: Update in Firebase
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