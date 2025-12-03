package com.example.mynewapplication.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mynewapplication.data.model.LostItem
import com.example.mynewapplication.ui.components.InfoChip
import com.example.mynewapplication.ui.components.StatusChip
import com.example.mynewapplication.ui.components.UserAvatar
import com.example.mynewapplication.ui.theme.*

@Composable
fun ItemCard(
    item: LostItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: User info + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UserAvatar(
                        userName = item.userName,
                        imageUrl = null,
                        size = 40.dp
                    )

                    Spacer(Modifier.width(12.dp))

                    Column {
                        Text(
                            text = item.userName,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                        Text(
                            text = item.getFormattedDate(),
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                StatusChip(status = item.status)
            }

            Spacer(Modifier.height(12.dp))

            // Title
            Text(
                text = item.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Spacer(Modifier.height(8.dp))

            // Description
            Text(
                text = item.description,
                color = TextSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                maxLines = 3
            )

            Spacer(Modifier.height(12.dp))

            // Location & Category
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoChip(
                    icon = Icons.Outlined.LocationOn,
                    text = item.location
                )
                InfoChip(
                    icon = Icons.Outlined.Category,
                    text = item.category.displayName
                )
            }

            Spacer(Modifier.height(12.dp))

            // Contact Button
            Button(
                onClick = { /* TODO: Open chat */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Chat,
                    "Contact",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Contact",
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}