package com.example.mynewapplication.ui.screens.messages


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mynewapplication.data.model.ChatMessage
import com.example.mynewapplication.data.remote.FirebaseService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.Job

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val messageText: String = "",
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null,
    val otherUserName: String = "",
    val otherUser: com.example.mynewapplication.data.model.User? = null,
    val itemTitle: String = "",
    val relatedItem: com.example.mynewapplication.data.model.LostItem? = null
)

class ChatViewModel : ViewModel() {

    private val firebaseService = FirebaseService()
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    private var currentConversationId: String? = null
    private var messagesJob: Job? = null

    fun loadChat(conversationId: String, otherUserName: String = "", itemTitle: String = "") {
        currentConversationId = conversationId
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

                // Get conversation info if not provided
                if (true) { // Always fetch to be sure we have the latest
                    val conversationsResult = firebaseService.getUserConversations(currentUser.uid)
                    conversationsResult.getOrNull()?.firstOrNull { it.id == conversationId }?.let { conv ->
                        // Fetch Item
                        val itemResult = firebaseService.getLostItem(conv.itemId)
                        itemResult.getOrNull()?.let { item ->
                            _uiState.value = _uiState.value.copy(
                                relatedItem = item,
                                itemTitle = item.title
                            )
                        }
                        
                        // Fetch Other User
                        val otherUserId = conv.participants.firstOrNull { it != currentUser.uid }
                        otherUserId?.let { userId ->
                            val userResult = firebaseService.getUser(userId)
                            userResult.getOrNull()?.let { user ->
                                _uiState.value = _uiState.value.copy(
                                    otherUser = user,
                                    otherUserName = user.name
                                )
                            }
                        }
                    }
                }

                // Load messages
                val result = firebaseService.getConversationMessages(conversationId)
                result.fold(
                    onSuccess = { messages ->
                        val processedMessages = messages.map { message ->
                            if (message.senderId == currentUser.uid) {
                                message.copy(senderName = "Me")
                            } else {
                                message
                            }
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            messages = processedMessages,
                            isLoading = false
                        )
                        
                        // Mark messages as read
                        firebaseService.markMessagesAsRead(conversationId, currentUser.uid)
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load messages"
                        )
                    }
                )
            } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                    error = e.message ?: "Failed to load messages"
            )
            }
        }
    }

    fun onMessageTextChange(text: String) {
        _uiState.value = _uiState.value.copy(messageText = text)
    }

    fun refreshChat() {
        currentConversationId?.let { loadChat(it, _uiState.value.otherUserName, _uiState.value.itemTitle) }
    }

    fun sendMessage() {
        val text = _uiState.value.messageText.trim()
        if (text.isEmpty() || currentConversationId == null) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true, error = null)

            try {
                val currentUser = firebaseService.getCurrentUser()
                if (currentUser == null) {
                    _uiState.value = _uiState.value.copy(
                        isSending = false,
                        error = "Not logged in"
                    )
                    return@launch
                }

                val userData = firebaseService.getCurrentUserData().getOrNull()
                val senderName = userData?.name ?: currentUser.displayName ?: "Me"

                val result = firebaseService.sendMessage(
                    currentConversationId!!,
                    text,
                    senderName
                )

                result.fold(
                    onSuccess = { messageId ->
                val newMessage = ChatMessage(
                            id = messageId,
                            conversationId = currentConversationId!!,
                            senderId = currentUser.uid,
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
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isSending = false,
                            error = error.message ?: "Failed to send message"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSending = false,
                    error = e.message ?: "Failed to send message"
                )
            }
        }
    }
}