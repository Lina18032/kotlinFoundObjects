package com.example.mynewapplication.ui.screens.messages


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mynewapplication.data.model.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val messageText: String = "",
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null,
    val otherUserName: String = "",
    val itemTitle: String = ""
)

class ChatViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val currentUserId = "current_user"

    fun loadChat(conversationId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Sample data - will be replaced with Firebase later
            val sampleMessages = listOf(
                ChatMessage(
                    id = "1",
                    conversationId = conversationId,
                    senderId = "user1",
                    senderName = "Lina Lolem",
                    text = "Hi! I found your keys near Block B.",
                    timestamp = System.currentTimeMillis() - 7200000,
                    isRead = true
                ),
                ChatMessage(
                    id = "2",
                    conversationId = conversationId,
                    senderId = currentUserId,
                    senderName = "Me",
                    text = "Oh thank you so much! Are they still with you?",
                    timestamp = System.currentTimeMillis() - 7000000,
                    isRead = true
                ),
                ChatMessage(
                    id = "3",
                    conversationId = conversationId,
                    senderId = "user1",
                    senderName = "Lina Lolem",
                    text = "Yes, I still have them. When can you pick them up?",
                    timestamp = System.currentTimeMillis() - 3600000,
                    isRead = true
                ),
                ChatMessage(
                    id = "4",
                    conversationId = conversationId,
                    senderId = currentUserId,
                    senderName = "Me",
                    text = "I can come by in 30 minutes. Where should we meet?",
                    timestamp = System.currentTimeMillis() - 3500000,
                    isRead = true
                ),
                ChatMessage(
                    id = "5",
                    conversationId = conversationId,
                    senderId = "user1",
                    senderName = "Lina Lolem",
                    text = "Let's meet at the cafeteria entrance.",
                    timestamp = System.currentTimeMillis() - 1800000,
                    isRead = true
                )
            )

            _uiState.value = _uiState.value.copy(
                messages = sampleMessages.sortedBy { it.timestamp },
                isLoading = false,
                otherUserName = "Lina Lolem",
                itemTitle = "Keys with blue keychain"
            )
        }
    }

    fun onMessageTextChange(text: String) {
        _uiState.value = _uiState.value.copy(messageText = text)
    }

    fun sendMessage() {
        val text = _uiState.value.messageText.trim()
        if (text.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true)

            try {
                // TODO: Send to Firebase
                val newMessage = ChatMessage(
                    id = System.currentTimeMillis().toString(),
                    conversationId = "conv1", // TODO: Use actual conversation ID
                    senderId = currentUserId,
                    senderName = "Me",
                    text = text,
                    timestamp = System.currentTimeMillis(),
                    isRead = false
                )

                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + newMessage,
                    messageText = "",
                    isSending = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSending = false,
                    error = "Failed to send message"
                )
            }
        }
    }
}