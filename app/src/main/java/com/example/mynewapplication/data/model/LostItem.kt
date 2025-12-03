package com.example.mynewapplication.data.model

data class LostItem(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: Category = Category.OTHER,
    val location: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: ItemStatus = ItemStatus.LOST,
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val imageUrls: List<String> = emptyList(),
    val isResolved: Boolean = false
) {
    fun getFormattedDate(): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60000 -> "Just now"
            diff < 3600000 -> "${diff / 60000} minutes ago"
            diff < 86400000 -> "${diff / 3600000} hours ago"
            diff < 604800000 -> "${diff / 86400000} days ago"
            else -> "${diff / 604800000} weeks ago"
        }
    }
}