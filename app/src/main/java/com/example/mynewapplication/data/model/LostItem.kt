package com.example.mynewapplication.data.model

import com.google.firebase.firestore.PropertyName
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
    @get:PropertyName("isResolved")
    val isResolved: Boolean = false,
    
    // Legacy fields that might exist in old documents
    @get:JvmName("getFirebaseImageUrls")
    val imageURLs: List<String> = emptyList(),
    val resolved: Boolean = false,
    @get:PropertyName("formattedDate")
    val _formattedDate: String = ""
) {
    @com.google.firebase.firestore.Exclude
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