@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.mynewapplication.ui.screens.home.components


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mynewapplication.data.model.Category
import com.example.mynewapplication.data.model.ItemStatus
import com.example.mynewapplication.ui.theme.*


@Composable
fun CategoryFilterSection(
    selectedCategory: Category?,
    selectedStatus: ItemStatus?,
    onCategorySelect: (Category?) -> Unit,
    onStatusSelect: (ItemStatus?) -> Unit,
    onClearFilters: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Filters",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            if (selectedCategory != null || selectedStatus != null) {
                TextButton(onClick = onClearFilters) {
                    Icon(
                        Icons.Default.Close,
                        "Clear",
                        modifier = Modifier.size(16.dp),
                        tint = PrimaryBlue
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Clear", color = PrimaryBlue)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Status Filter
        Text(
            "Status",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ItemStatus.values().forEach { status ->
                FilterChip(
                    selected = selectedStatus == status,
                    onClick = {
                        onStatusSelect(if (selectedStatus == status) null else status)
                    },
                    label = { Text(status.displayName) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Category Filter
        Text(
            "Category",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(Category.values().toList()) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = {
                        onCategorySelect(if (selectedCategory == category) null else category)
                    },
                    label = { Text(category.displayName) },
                    leadingIcon = {
                        Icon(
                            category.icon,
                            category.displayName,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }
    }
}