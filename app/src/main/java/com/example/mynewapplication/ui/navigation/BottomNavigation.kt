// File: ui/components/BottomNavigation.kt
package com.example.mynewapplication.ui.components


import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.example.mynewapplication.ui.navigation.Screen
import com.example.mynewapplication.ui.navigation.bottomNavItems
import com.example.mynewapplication.ui.theme.DarkCard
import com.example.mynewapplication.ui.theme.DarkSurface
import com.example.mynewapplication.ui.theme.PrimaryBlue
import com.example.mynewapplication.ui.theme.TextSecondary

@Composable
fun BottomNavigationBar(
    currentScreen: Screen,
    onScreenChange: (Screen) -> Unit
) {
    NavigationBar(
        containerColor = DarkSurface
    ) {
        bottomNavItems.forEach { screen ->
            NavigationBarItem(
                selected = currentScreen::class == screen::class,
                onClick = { onScreenChange(screen) },
                icon = {
                    screen.icon?.let {
                        Icon(it, contentDescription = screen.title)
                    }
                },
                label = { Text(screen.title) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryBlue,
                    selectedTextColor = PrimaryBlue,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary,
                    indicatorColor = DarkCard
                )
            )
        }
    }
}
