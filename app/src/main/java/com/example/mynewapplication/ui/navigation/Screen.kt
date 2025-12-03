// File: ui/navigation/Screen.kt
package com.example.mynewapplication.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector? = null
) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Add : Screen("add", "Add Item")
    object Messages : Screen("messages", "Messages", Icons.Default.Message)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    object Login : Screen("login", "Login")
    object ItemDetail : Screen("item/{itemId}", "Item Details") {
        fun createRoute(itemId: String) = "item/$itemId"
    }
    object Chat : Screen("chat/{conversationId}", "Chat") {
        fun createRoute(conversationId: String) = "chat/$conversationId"
    }
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Messages,
    Screen.Profile
)