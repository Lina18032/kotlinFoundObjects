// File: MainActivity.kt
package com.example.mynewapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.mynewapplication.data.remote.CloudinaryService
import com.example.mynewapplication.data.remote.FirebaseService
import com.example.mynewapplication.ui.theme.LguinahTheme
import com.example.mynewapplication.ui.navigation.AppNavigation
import com.example.mynewapplication.ui.screens.auth.LoginScreen
import com.example.mynewapplication.ui.screens.auth.WelcomeScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Cloudinary
        CloudinaryService.initialize(this)
        
        setContent {
            LguinahTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val firebaseService = remember { FirebaseService() }
                    var isLoggedIn by remember { mutableStateOf(false) }
                    var showWelcome by remember { mutableStateOf(true) }
                    var isCheckingAuth by remember { mutableStateOf(true) }

                    // Check if user is already logged in
                    LaunchedEffect(Unit) {
                        val currentUser = firebaseService.getCurrentUser()
                        isLoggedIn = currentUser != null
                        isCheckingAuth = false
                    }

                    if (isCheckingAuth) {
                        // Show loading while checking auth state
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                    when {
                        !isLoggedIn && showWelcome -> {
                            WelcomeScreen(
                                onGetStarted = { showWelcome = false }
                            )
                        }
                        !isLoggedIn -> {
                            LoginScreen(
                                onLoginSuccess = { isLoggedIn = true }
                            )
                        }
                            else -> {
                                AppNavigation(
                                    onLogout = {
                                        isLoggedIn = false
                                        showWelcome = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}