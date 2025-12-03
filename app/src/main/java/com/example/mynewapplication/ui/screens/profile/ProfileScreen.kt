package com.example.mynewapplication.ui.screens.profile


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.mynewapplication.ui.components.EmptyState

@Composable
fun ProfileScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        EmptyState(
            icon = Icons.Default.Person,
            title = "Profile",
            message = "Profile screen coming soon"
        )
    }
}