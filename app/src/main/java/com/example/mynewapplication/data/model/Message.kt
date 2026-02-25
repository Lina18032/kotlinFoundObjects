package com.example.mynewapplication.data.model

data class ChatConversation(
    val id: String = "",
    val itemId: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: ChatMessage? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val otherUserName: String = "",
    val itemTitle: String = ""
)

data class ChatMessage(
    val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
) {
    fun getFormattedTime(): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60000 -> "Just now"
            diff < 3600000 -> "${diff / 60000}m ago"
            diff < 86400000 -> "${diff / 3600000}h ago"
            else -> "${diff / 86400000}d ago"
        }
    }
}