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
import com.example.mynewapplication.data.model.LostItem
import com.example.mynewapplication.data.remote.FirebaseService
import com.example.mynewapplication.ui.components.BottomNavigationBar
import com.example.mynewapplication.ui.screens.home.HomeScreen
import com.example.mynewapplication.ui.screens.add.AddItemScreen
import com.example.mynewapplication.ui.screens.messages.MessagesScreen
import com.example.mynewapplication.ui.screens.messages.ChatScreen
import com.example.mynewapplication.ui.screens.profile.ProfileScreen
import com.example.mynewapplication.ui.screens.detail.ItemDetailScreen
import com.example.mynewapplication.ui.theme.DarkBackground
import com.example.mynewapplication.ui.theme.PrimaryBlue
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(
    onLogout: () -> Unit
) {
    val firebaseService = remember { FirebaseService() }
    val coroutineScope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var selectedConversationId by remember { mutableStateOf<String?>(null) }
    var selectedItem by remember { mutableStateOf<LostItem?>(null) }
    var isLoadingConversation by remember { mutableStateOf(false) }

    fun openConversationForItem(item: LostItem, closeDetailsAfterOpen: Boolean) {
        val currentUser = firebaseService.getCurrentUser()
        if (currentUser != null && item.userId != currentUser.uid) {
            isLoadingConversation = true
            coroutineScope.launch {
                val result = firebaseService.getOrCreateConversation(
                    item.id,
                    item.userId
                )
                result.fold(
                    onSuccess = { conversationId ->
                        selectedConversationId = conversationId
                        if (closeDetailsAfterOpen) {
                            selectedItem = null
                            currentScreen = Screen.Messages
                        }
                        isLoadingConversation = false
                    },
                    onFailure = {
                        isLoadingConversation = false
                        // Handle error - could show snackbar
                    }
                )
            }
        } else if (closeDetailsAfterOpen) {
            selectedItem = null
            currentScreen = Screen.Messages
        } else {
            currentScreen = Screen.Messages
        }
    }

    // Show item detail screen
    if (selectedItem != null) {
        ItemDetailScreen(
            item = selectedItem!!,
            onBack = { selectedItem = null },
            onContactClick = {
                openConversationForItem(selectedItem!!, closeDetailsAfterOpen = true)
            }
        )
    }
    
    if (isLoadingConversation) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
    // Show chat screen
    else if (selectedConversationId != null) {
        ChatScreen(
            conversationId = selectedConversationId!!,
            onBack = { selectedConversationId = null }
        )
    }
    // Show main app
    else {
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
                    is Screen.Home -> HomeScreen(
                        onAddClick = { currentScreen = Screen.Add },
                        onItemClick = { item -> selectedItem = item },
                        onContactClick = { item ->
                            openConversationForItem(item, closeDetailsAfterOpen = false)
                        }
                    )
                    is Screen.Add -> AddItemScreen(onBack = { currentScreen = Screen.Home })
                    is Screen.Messages -> MessagesScreen(
                        onConversationClick = { conversationId ->
                            selectedConversationId = conversationId
                        }
                    )
                    is Screen.Profile -> ProfileScreen(onLogout = onLogout)
                    else -> HomeScreen(
                        onAddClick = { currentScreen = Screen.Add },
                        onItemClick = { item -> selectedItem = item },
                        onContactClick = { item ->
                            openConversationForItem(item, closeDetailsAfterOpen = false)
                        }
                    )
                }
            }
        }
    }
}
