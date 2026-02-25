package com.example.mynewapplication.data.model

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val profileImageUrl: String? = null,
    val phoneNumber: String? = null,
    val role: String = "USER", // "USER" or "ADMIN"
    val isBlocked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    @com.google.firebase.firestore.Exclude
    fun isValidEstinEmail(): Boolean {
        return email.endsWith("@estin.dz")
    }

    @com.google.firebase.firestore.Exclude
    fun getInitials(): String {
        return name.split(" ")
            .mapNotNull { it.firstOrNull()?.toString() }
            .take(2)
            .joinToString("")
            .uppercase()
    }
}