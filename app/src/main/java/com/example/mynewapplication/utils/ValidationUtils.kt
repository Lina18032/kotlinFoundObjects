package com.example.mynewapplication.utils

object ValidationUtils {

    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null
    )

    fun validateEmail(email: String): ValidationResult {
        return when {
            email.isBlank() -> ValidationResult(false, "Email is required")
            !email.isValidEmail() -> ValidationResult(false, "Invalid email format")
            !email.isValidEstinEmail() -> ValidationResult(
                false,
                "Only @estin.dz emails are allowed"
            )
            else -> ValidationResult(true)
        }
    }

    fun validatePassword(password: String): ValidationResult {
        return when {
            password.isBlank() -> ValidationResult(false, "Password is required")
            password.length < Constants.MIN_PASSWORD_LENGTH -> ValidationResult(
                false,
                "Password must be at least ${Constants.MIN_PASSWORD_LENGTH} characters"
            )
            else -> ValidationResult(true)
        }
    }

    fun validateTitle(title: String): ValidationResult {
        return when {
            title.isBlank() -> ValidationResult(false, "Title is required")
            title.length > Constants.MAX_TITLE_LENGTH -> ValidationResult(
                false,
                "Title must be less than ${Constants.MAX_TITLE_LENGTH} characters"
            )
            else -> ValidationResult(true)
        }
    }

    fun validateDescription(description: String): ValidationResult {
        return when {
            description.isBlank() -> ValidationResult(false, "Description is required")
            description.length > Constants.MAX_DESCRIPTION_LENGTH -> ValidationResult(
                false,
                "Description must be less than ${Constants.MAX_DESCRIPTION_LENGTH} characters"
            )
            else -> ValidationResult(true)
        }
    }

    fun validateLocation(location: String): ValidationResult {
        return when {
            location.isBlank() -> ValidationResult(false, "Location is required")
            else -> ValidationResult(true)
        }
    }
}