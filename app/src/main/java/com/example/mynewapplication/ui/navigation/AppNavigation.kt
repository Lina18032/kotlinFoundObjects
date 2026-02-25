// File: ui/navigation/AppNavigation.kt
package com.example.mynewapplication.ui.navigation



import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
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
    var homeRefreshTrigger by remember { mutableStateOf(0) }
    var profileRefreshTrigger by remember { mutableStateOf(0) }
    var selectedEditItem by remember { mutableStateOf<LostItem?>(null) }

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

    // Navigation Handlers
    BackHandler(enabled = selectedItem != null) {
        selectedItem = null
    }

    BackHandler(enabled = selectedConversationId != null && selectedItem == null) {
        selectedConversationId = null
    }

    BackHandler(enabled = currentScreen != Screen.Home && selectedItem == null && selectedConversationId == null) {
        if (selectedEditItem != null) {
            selectedEditItem = null
        }
        currentScreen = Screen.Home
    }

    // Show chat screen
    if (isLoadingConversation) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } 
    // Show item detail screen - Prioritize this so it can be shown over Chat
    else if (selectedItem != null) {
        val currentUser = firebaseService.getCurrentUser()
        val isOwner = currentUser?.uid == selectedItem?.userId
        val context = LocalContext.current
        
        ItemDetailScreen(
            item = selectedItem!!,
            onBack = { selectedItem = null },
            onContactClick = {
                openConversationForItem(selectedItem!!, closeDetailsAfterOpen = true)
            },
            onEdit = {
                selectedEditItem = selectedItem
                selectedItem = null
                currentScreen = Screen.Add
            },
            onDelete = {
                val itemToDelete = selectedItem
                if (itemToDelete != null) {
                    coroutineScope.launch {
                        val result = firebaseService.deleteLostItem(itemToDelete.id)
                        result.fold(
                            onSuccess = {
                                Toast.makeText(context, "Item deleted successfully", Toast.LENGTH_SHORT).show()
                                selectedItem = null
                                homeRefreshTrigger++
                                profileRefreshTrigger++
                            },
                            onFailure = { error ->
                                Toast.makeText(context, "Failed to delete: ${error.message}", Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                }
            },
            isOwner = isOwner
        )
    }
    else if (selectedConversationId != null) {
        ChatScreen(
            conversationId = selectedConversationId!!,
            onBack = { selectedConversationId = null },
            onItemClick = { item -> selectedItem = item }
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
                        },
                        refreshTrigger = homeRefreshTrigger
                    )
                    is Screen.Add -> AddItemScreen(
                        itemToEdit = selectedEditItem,
                        onBack = { 
                            selectedEditItem = null
                            currentScreen = Screen.Home 
                        },
                        onItemPosted = {
                            homeRefreshTrigger++
                            profileRefreshTrigger++
                            selectedEditItem = null
                            currentScreen = Screen.Home
                        }
                    )
                    is Screen.Messages -> MessagesScreen(
                        onConversationClick = { conversationId ->
                            selectedConversationId = conversationId
                        }
                    )
                    is Screen.Profile -> ProfileScreen(
                        onLogout = onLogout,
                        onItemClick = { item -> selectedItem = item },
                        refreshTrigger = profileRefreshTrigger
                    )
                    else -> HomeScreen(
                        onAddClick = { currentScreen = Screen.Add },
                        onItemClick = { item -> selectedItem = item },
                        onContactClick = { item ->
                            openConversationForItem(item, closeDetailsAfterOpen = false)
                        },
                        refreshTrigger = homeRefreshTrigger
                    )
                }
            }
        }
    }
}
