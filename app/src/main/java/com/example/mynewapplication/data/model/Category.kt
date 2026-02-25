package com.example.mynewapplication.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class Category(val displayName: String, val icon: ImageVector) {
    KEYS("Keys", Icons.Default.Key),
    CARDS("Cards", Icons.Default.CreditCard),
    ELECTRONICS("Electronics", Icons.Default.PhoneAndroid),
    BAGS("Bags", Icons.Default.ShoppingBag),
    DOCUMENTS("Documents", Icons.Default.Description),
    CLOTHING("Clothing", Icons.Default.Checkroom),
    ACCESSORIES("Accessories", Icons.Default.Watch),
    BOOKS("Books", Icons.Default.MenuBook),
    PHONE("Phone", Icons.Default.Smartphone),
    WALLET("Wallet", Icons.Default.AccountBalanceWallet),
    PETS("Pets", Icons.Default.Pets),
    OTHER("Other", Icons.Default.MoreHoriz)
}