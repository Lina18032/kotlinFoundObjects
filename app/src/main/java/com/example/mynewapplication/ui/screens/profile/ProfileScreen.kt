package com.example.mynewapplication.ui.screens.profile



import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
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
import com.example.mynewapplication.ui.components.EmptyState
import com.example.mynewapplication.ui.components.LoadingIndicator
import com.example.mynewapplication.ui.components.UserAvatar
import com.example.mynewapplication.ui.screens.home.components.ItemCard
import com.example.mynewapplication.ui.theme.*
import com.example.mynewapplication.utils.Constants

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onItemClick: (LostItem) -> Unit,
    onAdminClick: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isLoading,
        onRefresh = { viewModel.refreshProfile() }
    )

    // Show toast on password success
    LaunchedEffect(uiState.passwordSuccess) {
        uiState.passwordSuccess?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearPasswordMessages()
        }
    }

    // Show toast on password error
    LaunchedEffect(uiState.passwordError) {
        uiState.passwordError?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearPasswordMessages()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        if (uiState.isLoading && uiState.myLostItems.isEmpty() && uiState.myFoundItems.isEmpty()) {
            LoadingIndicator()
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                ProfileHeader(
                    user = uiState.user,
                    onEditClick = viewModel::showEditDialog,
                    onChangePasswordClick = viewModel::showPasswordDialog,
                    onAdminClick = onAdminClick,
                    onLogoutClick = {
                        viewModel.logout(context) {
                            onLogout()
                        }
                    }
                )

            // Tabs
            TabRow(
                selectedTabIndex = uiState.selectedTab,
                containerColor = DarkSurface,
                contentColor = PrimaryBlue
            ) {
                Tab(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.onTabSelected(0) },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Lost")
                            Text(
                                "(${uiState.myLostItems.size})",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.onTabSelected(1) },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Found")
                            Text(
                                "(${uiState.myFoundItems.size})",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                )
            }

            // Content
                when (uiState.selectedTab) {
                    0 -> ItemsList(
                        items = uiState.myLostItems,
                        emptyMessage = "You haven't reported any lost items yet",
                        onDeleteItem = viewModel::deleteItem,
                        onItemClick = onItemClick
                    )
                    1 -> ItemsList(
                        items = uiState.myFoundItems,
                        emptyMessage = "You haven't reported any found items yet",
                        onDeleteItem = viewModel::deleteItem,
                        onItemClick = onItemClick
                    )
                }
            }
        }
        PullRefreshIndicator(
            refreshing = uiState.isLoading,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }

    // Edit Dialog
    if (uiState.showEditDialog) {
        EditProfileDialog(
            currentName = uiState.user?.name ?: "",
            currentPhone = uiState.user?.phoneNumber ?: "",
            onDismiss = viewModel::hideEditDialog,
            onSave = { name, phone ->
                viewModel.updateProfile(name, phone)
            }
        )
    }

    // Change Password Dialog
    if (uiState.showPasswordDialog) {
        ChangePasswordDialog(
            onDismiss = viewModel::hidePasswordDialog,
            onSave = { current, new ->
                viewModel.changePassword(
                    if (uiState.hasPasswordProvider) current else null, 
                    new
                )
            },
            isLoading = uiState.isLoading,
            hasPasswordProvider = uiState.hasPasswordProvider
        )
    }
}

@Composable
fun ProfileHeader(
    user: com.example.mynewapplication.data.model.User?,
    onEditClick: () -> Unit,
    onChangePasswordClick: () -> Unit,
    onAdminClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(DarkCard, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                user?.let {
                    Text(
                        text = it.getInitials(),
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Name - fallback to email username if name is empty
            val displayName = when {
                user?.name?.isNotBlank() == true -> user.name
                user?.email?.isNotBlank() == true -> user.email.substringBefore("@")
                else -> "User"
            }
            Text(
                text = displayName,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(Modifier.height(4.dp))

            // Email
            Text(
                text = user?.email ?: "",
                fontSize = 14.sp,
                color = TextSecondary
            )

            // Phone
            user?.phoneNumber?.let { phone ->
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = TextSecondary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = phone,
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onEditClick,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = PrimaryBlue
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Edit Profile")
                }

                OutlinedButton(
                    onClick = onLogoutClick,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ErrorRed
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.ExitToApp,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Logout")
                }
            }
            
            Spacer(Modifier.height(12.dp))

            if (user?.role?.uppercase() == "ADMIN") {
                Button(
                    onClick = onAdminClick,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Settings, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Admin Panel")
                }
                Spacer(Modifier.height(12.dp))
            }

            TextButton(
                onClick = onChangePasswordClick,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = PrimaryBlue
                )
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Change Password")
            }
        }
    }
}

@Composable
fun ItemsList(
    items: List<LostItem>,
    emptyMessage: String,
    onDeleteItem: (String) -> Unit,
    onItemClick: (LostItem) -> Unit
) {
    if (items.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Info,
            title = "No items",
            message = emptyMessage
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { item ->
                MyItemCard(
                    item = item,
                    onDeleteClick = { onDeleteItem(item.id) },
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}

@Composable
fun MyItemCard(
    item: LostItem,
    onDeleteClick: () -> Unit,
    onClick: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

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
                Text(
                    text = item.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = ErrorRed
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = item.description,
                fontSize = 14.sp,
                color = TextSecondary,
                maxLines = 2
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "${item.location} • ${item.getFormattedDate()}",
                fontSize = 12.sp,
                color = TextTertiary
            )
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Item?") },
            text = { Text("Are you sure you want to delete this item? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteClick()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = PrimaryBlue)
                }
            },
            containerColor = DarkCard
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileDialog(
    currentName: String,
    currentPhone: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var phone by remember { mutableStateOf(currentPhone) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Edit Profile",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = DarkCard,
                        containerColor = DarkCard,
                        cursorColor = PrimaryBlue
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = DarkCard,
                        containerColor = DarkCard,
                        cursorColor = PrimaryBlue
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, phone) },
                enabled = name.isNotBlank()
            ) {
                Text("Save", color = PrimaryBlue)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = DarkCard
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onSave: (String?, String) -> Unit,
    isLoading: Boolean,
    hasPasswordProvider: Boolean
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var currentVisible by remember { mutableStateOf(false) }
    var newVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Password", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (hasPasswordProvider) {
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text("Current Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (currentVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { currentVisible = !currentVisible }) {
                                Icon(if (currentVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                            }
                        },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = PrimaryBlue,
                            containerColor = DarkCard
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (newVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { newVisible = !newVisible }) {
                            Icon(if (newVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                        }
                    },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = PrimaryBlue,
                        containerColor = DarkCard
                    ),
                    shape = RoundedCornerShape(12.dp),
                    isError = newPassword.isNotEmpty() && newPassword.length < Constants.MIN_PASSWORD_LENGTH,
                    supportingText = {
                        val color = if (newPassword.isNotEmpty() && newPassword.length < Constants.MIN_PASSWORD_LENGTH) ErrorRed else TextSecondary
                        Text(
                            text = "Must be at least ${Constants.MIN_PASSWORD_LENGTH} characters",
                            color = color,
                            fontSize = 12.sp
                        )
                    }
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm New Password") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (confirmVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { confirmVisible = !confirmVisible }) {
                            Icon(if (confirmVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                        }
                    },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = PrimaryBlue,
                        containerColor = DarkCard
                    ),
                    shape = RoundedCornerShape(12.dp),
                    isError = confirmPassword.isNotEmpty() && confirmPassword != newPassword
                )
                
                if (confirmPassword.isNotEmpty() && confirmPassword != newPassword) {
                    Text("Passwords do not match", color = ErrorRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    onSave(if (hasPasswordProvider) currentPassword else null, newPassword) 
                },
                enabled = !isLoading && 
                          (!hasPasswordProvider || currentPassword.isNotBlank()) && 
                          newPassword.length >= Constants.MIN_PASSWORD_LENGTH && confirmPassword == newPassword
            ) {
                Text(if (isLoading) "Updating..." else "Update", color = PrimaryBlue)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = DarkCard
    )
}
