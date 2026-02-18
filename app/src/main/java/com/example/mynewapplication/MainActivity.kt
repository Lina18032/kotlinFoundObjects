package com.example.mynewapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.example.mynewapplication.data.remote.CloudinaryService
import com.example.mynewapplication.data.remote.FirebaseService
import com.example.mynewapplication.ui.navigation.AppNavigation
import com.example.mynewapplication.ui.navigation.Screen
import com.example.mynewapplication.ui.screens.auth.LoginScreen
import com.example.mynewapplication.ui.screens.auth.WelcomeScreen
import com.example.mynewapplication.ui.theme.LguinahTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        CloudinaryService.initialize(this)

        setContent {
            LguinahTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val firebaseService = remember { FirebaseService() }
                    var startDestination by remember { mutableStateOf<String?>(null) }

                    LaunchedEffect(Unit) {
                        startDestination = if (firebaseService.getCurrentUser() != null) {
                            "app_graph"
                        } else {
                            "auth_graph"
                        }
                    }

                    if (startDestination != null) {
                        NavHost(navController = navController, startDestination = startDestination!!) {
                            navigation(startDestination = "welcome", route = "auth_graph") {
                                composable("welcome") {
                                    WelcomeScreen(onGetStarted = { navController.navigate(Screen.Login.route) })
                                }
                                composable(Screen.Login.route) {
                                    LoginScreen(onLoginSuccess = {
                                        navController.navigate("app_graph") {
                                            popUpTo("auth_graph") { inclusive = true }
                                        }
                                    })
                                }
                            }
                            composable("app_graph") {
                                AppNavigation(onLogout = {
                                    firebaseService.signOut()
                                    navController.navigate("auth_graph") {
                                        popUpTo("app_graph") { inclusive = true }
                                    }
                                })
                            }
                        }
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}