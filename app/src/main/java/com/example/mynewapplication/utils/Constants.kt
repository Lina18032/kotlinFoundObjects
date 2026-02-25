package com.example.mynewapplication.utils

object Constants {
    // App
    const val APP_NAME = "LGUINAH"

    // Firebase Collections (for later)
    const val COLLECTION_USERS = "users"
    const val COLLECTION_ITEMS = "items"
    const val COLLECTION_CONVERSATIONS = "conversations"
    const val COLLECTION_MESSAGES = "messages"

    // Validation
    const val EMAIL_DOMAIN = "@estin.dz"
    const val MIN_PASSWORD_LENGTH = 6
    const val MAX_TITLE_LENGTH = 100
    const val MAX_DESCRIPTION_LENGTH = 500
    const val MAX_IMAGES = 5

    // UI
    const val ANIMATION_DURATION = 300
    const val DEBOUNCE_TIME = 500L

    // AI Matcher API (10.0.2.2 = localhost from Android emulator)
    const val MATCHER_API_BASE_URL = "http://10.0.2.2:8000"
    const val MATCHER_API_KEY = "change-me-in-production"
}

object PreferenceKeys {
    const val USER_ID = "user_id"
    const val USER_EMAIL = "user_email"
    const val USER_NAME = "user_name"
    const val IS_LOGGED_IN = "is_logged_in"
}
