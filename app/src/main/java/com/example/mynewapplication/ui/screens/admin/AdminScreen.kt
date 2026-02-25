package com.example.mynewapplication.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mynewapplication.data.model.LostItem
import com.example.mynewapplication.data.model.User
import com.example.mynewapplication.data.model.ChatConversation
import com.example.mynewapplication.ui.components.EmptyState
import com.example.mynewapplication.ui.components.LoadingIndicator
import com.example.mynewapplication.ui.components.UserAvatar
import com.example.mynewapplication.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onBackClick: () -> Unit,
    onItemClick: (LostItem) -> Unit,
    viewModel: AdminViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Panel", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = DarkSurface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tabs
            TabRow(
                selectedTabIndex = uiState.selectedTab,
                containerColor = DarkSurface,
                contentColor = PrimaryBlue,
                divider = {}
            ) {
                Tab(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.onTabSelected(0) },
                    text = { Text("Items (${uiState.items.size})") }
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.onTabSelected(1) },
                    text = { Text("Users (${uiState.users.size})") }
                )
                Tab(
                    selected = uiState.selectedTab == 2,
                    onClick = { viewModel.onTabSelected(2) },
                    text = { Text("Messages (${uiState.conversations.size})") }
                )
            }

            if (uiState.isLoading && uiState.items.isEmpty() && uiState.users.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
            } else {
                when (uiState.selectedTab) {
                    0 -> AdminItemsList(
                        items = uiState.items,
                        onItemClick = onItemClick,
                        onDeleteItem = viewModel::deleteItem
                    )
                    1 -> AdminUsersList(
                        users = uiState.users,
                        onBlockToggle = viewModel::toggleBlockUser
                    )
                    2 -> AdminConversationsList(
                        conversations = uiState.conversations,
                        onDeleteConversation = viewModel::deleteConversation
                    )
                }
            }
        }
    }

    // Error Snackbar or Toast handled by ViewModel/UI interaction
}

@Composable
fun AdminItemsList(
    items: List<LostItem>,
    onItemClick: (LostItem) -> Unit,
    onDeleteItem: (String) -> Unit
) {
    if (items.isEmpty()) {
        EmptyState(
            icon = Icons.Default.List,
            title = "No Items",
            message = "No items reported yet"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items) { item ->
                AdminItemCard(
                    item = item,
                    onClick = { onItemClick(item) },
                    onDelete = { onDeleteItem(item.id) }
                )
            }
        }
    }
}

@Composable
fun AdminItemCard(
    item: LostItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Item") },
            text = { Text("Are you sure you want to delete this item? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
                    Text("Delete", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkCard
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "By ${item.userName}",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
                
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed)
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = item.description,
                color = TextSecondary,
                fontSize = 14.sp,
                maxLines = 2
            )
        }
    }
}

@Composable
fun AdminUsersList(
    users: List<User>,
    onBlockToggle: (String, Boolean) -> Unit
) {
    if (users.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Person,
            title = "No Users",
            message = "No users found"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(users) { user ->
                AdminUserCard(
                    user = user,
                    onBlockToggle = { onBlockToggle(user.id, user.isBlocked) }
                )
            }
        }
    }
}

@Composable
fun AdminUserCard(
    user: User,
    onBlockToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(userName = user.name, imageUrl = user.profileImageUrl, size = 48.dp)
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(text = user.name, color = Color.White, fontWeight = FontWeight.Bold)
                Text(text = user.email, color = TextSecondary, fontSize = 12.sp)
                if (user.role.uppercase() == "ADMIN") {
                    Text(text = "ADMIN", color = PrimaryBlue, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
            
            if (user.role.uppercase() != "ADMIN") {
                Button(
                    onClick = onBlockToggle,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (user.isBlocked) Color.Gray else ErrorRed
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(if (user.isBlocked) "Unblock" else "Block", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun AdminConversationsList(
    conversations: List<ChatConversation>,
    onDeleteConversation: (String) -> Unit
) {
    if (conversations.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Chat,
            title = "No Conversations",
            message = "No messages have been sent yet"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(conversations) { conversation ->
                AdminConversationCard(
                    conversation = conversation,
                    onDelete = { onDeleteConversation(conversation.id) }
                )
            }
        }
    }
}

@Composable
fun AdminConversationCard(
    conversation: ChatConversation,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Conversation") },
            text = { Text("Are you sure you want to delete this conversation and all its messages? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
                    Text("Delete", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkCard
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Forum, null, tint = PrimaryBlue, modifier = Modifier.size(32.dp))
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Conversation ID: ${conversation.id.take(8)}...",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Participants: ${conversation.participants.size}",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed)
            }
        }
    }
}
