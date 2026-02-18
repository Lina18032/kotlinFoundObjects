package com.example.mynewapplication.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mynewapplication.ui.screens.add.AddItemScreen
import com.example.mynewapplication.ui.screens.home.HomeScreen
import com.example.mynewapplication.ui.screens.messages.ChatScreen
import com.example.mynewapplication.ui.screens.messages.MessagesScreen
import com.example.mynewapplication.ui.screens.profile.ProfileScreen
import com.example.mynewapplication.ui.theme.DarkBackground

@Composable
fun AppNavigation(onLogout: () -> Unit) {
    val navController = rememberNavController()

    Scaffold(
        containerColor = DarkBackground,
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onAddClick = { navController.navigate(Screen.Add.route) },
                    onItemClick = { item ->
                        // In a real app, you'd navigate to a detail screen
                        // For now, we do nothing to keep it simple
                    }
                )
            }
            composable(Screen.Add.route) {
                AddItemScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Messages.route) {
                MessagesScreen(onConversationClick = { conversationId ->
                    navController.navigate(Screen.Chat.createRoute(conversationId))
                })
            }
            composable(Screen.Profile.route) {
                ProfileScreen(onLogout = onLogout)
            }
            composable(Screen.Chat.route) { backStackEntry ->
                val conversationId = backStackEntry.arguments?.getString("conversationId")
                if (conversationId != null) {
                    ChatScreen(
                        conversationId = conversationId,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}