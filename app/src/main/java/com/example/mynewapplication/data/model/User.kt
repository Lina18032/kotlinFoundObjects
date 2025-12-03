package com.example.mynewapplication.data.model

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val profileImageUrl: String? = null,
    val phoneNumber: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun isValidEstinEmail(): Boolean {
        return email.endsWith("@estin.dz")
    }

    fun getInitials(): String {
        return name.split(" ")
            .mapNotNull { it.firstOrNull()?.toString() }
            .take(2)
            .joinToString("")
            .uppercase()
    }
}