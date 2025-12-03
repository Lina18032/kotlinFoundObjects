// File: MainActivity.kt
package com.example.mynewapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.mynewapplication.ui.theme.LguinahTheme
import com.example.mynewapplication.ui.navigation.AppNavigation
import com.example.mynewapplication.ui.screens.auth.LoginScreen
import com.example.mynewapplication.ui.screens.auth.WelcomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LguinahTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var isLoggedIn by remember { mutableStateOf(false) }
                    var showWelcome by remember { mutableStateOf(true) }

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
                            AppNavigation()
                        }
                    }
                }
            }
        }
    }
}