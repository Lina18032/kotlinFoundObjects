package com.example.mynewapplication.ui.screens.messages


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mynewapplication.data.model.ChatConversation
import com.example.mynewapplication.ui.components.EmptyState
import com.example.mynewapplication.ui.components.LoadingIndicator
import com.example.mynewapplication.ui.components.UserAvatar
import com.example.mynewapplication.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    onConversationClick: (String) -> Unit = {},
    viewModel: MessagesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Bar
        TopAppBar(
            title = {
                Text(
                    text = "Messages",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = DarkBackground
            )
        )

        // Debug: Show if loading
        if (uiState.isLoading) {
            LoadingIndicator()
        } else if (uiState.error != null) {
            // Show error
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: ${uiState.error}", color = Color.Red)
            }
        } else if (uiState.conversations.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Message,
                title = "No messages yet",
                message = "Start a conversation by contacting someone about their lost item"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.conversations) { conversation ->
                    ConversationCard(
                        conversation = conversation,
                        onClick = {
                            viewModel.markAsRead(conversation.id)
                            onConversationClick(conversation.id)
                        }
                    )
                }
            }
        }
    }
}
@Composable
fun ConversationCard(
    conversation: ChatConversation,
    onClick: () -> Unit
) {
    val lastMessage = conversation.lastMessage
    val isUnread = lastMessage?.isRead == false && lastMessage.senderId != "current_user"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnread) DarkCard.copy(alpha = 1f) else DarkCard
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar
            UserAvatar(
                userName = lastMessage?.senderName ?: "User",
                size = 56.dp
            )

            // Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = lastMessage?.senderName ?: "Unknown",
                        fontSize = 16.sp,
                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Medium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = lastMessage?.getFormattedTime() ?: "",
                        fontSize = 12.sp,
                        color = if (isUnread) PrimaryBlue else TextSecondary
                    )
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = lastMessage?.text ?: "No messages yet",
                        fontSize = 14.sp,
                        color = if (isUnread) TextPrimary else TextSecondary,
                        fontWeight = if (isUnread) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (isUnread) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(PrimaryBlue, CircleShape)
                        )
                    }
                }
            }
        }
    }
}
