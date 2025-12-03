// File: ui/navigation/AppNavigation.kt
package com.example.mynewapplication.ui.navigation


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.mynewapplication.ui.components.BottomNavigationBar
import com.example.mynewapplication.ui.screens.home.HomeScreen
import com.example.mynewapplication.ui.screens.add.AddItemScreen
import com.example.mynewapplication.ui.screens.messages.MessagesScreen
import com.example.mynewapplication.ui.screens.messages.ChatScreen
import com.example.mynewapplication.ui.screens.profile.ProfileScreen
import com.example.mynewapplication.ui.theme.DarkBackground
import com.example.mynewapplication.ui.theme.PrimaryBlue

@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var selectedConversationId by remember { mutableStateOf<String?>(null) }

    // Show chat screen
    if (selectedConversationId != null) {
        ChatScreen(
            conversationId = selectedConversationId!!,
            onBack = { selectedConversationId = null }
        )
    } else {
        Scaffold(
            containerColor = DarkBackground,
            bottomBar = {
                BottomNavigationBar(
                    currentScreen = currentScreen,
                    onScreenChange = { currentScreen = it }
                )
            },
            floatingActionButton = {
                if (currentScreen is Screen.Home) {
                    FloatingActionButton(
                        onClick = { currentScreen = Screen.Add },
                        containerColor = PrimaryBlue,
                        contentColor = Color.White,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Add, "Add Item")
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when (currentScreen) {
                    is Screen.Home -> HomeScreen(onAddClick = { currentScreen = Screen.Add })
                    is Screen.Add -> AddItemScreen(onBack = { currentScreen = Screen.Home })
                    is Screen.Messages -> MessagesScreen(
                        onConversationClick = { conversationId ->
                            selectedConversationId = conversationId
                        }
                    )
                    is Screen.Profile -> ProfileScreen()
                    else -> HomeScreen(onAddClick = { currentScreen = Screen.Add })
                }
            }
        }
    }
}