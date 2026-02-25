package com.example.mynewapplication.ui.screens.add

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.mynewapplication.data.model.LostItem
import com.example.mynewapplication.data.model.Category
import com.example.mynewapplication.data.model.ItemStatus
import com.example.mynewapplication.ui.components.LguinahTextField
import com.example.mynewapplication.ui.components.PrimaryButton
import com.example.mynewapplication.ui.components.SecondaryButton
import com.example.mynewapplication.ui.theme.*
import com.example.mynewapplication.utils.Constants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemScreen(
    onBack: () -> Unit,
    onItemPosted: (List<LostItem>) -> Unit,
    itemToEdit: LostItem? = null,
    viewModel: AddItemViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showMatchDialog by remember { mutableStateOf(false) }
    var matchedItems by remember { mutableStateOf<List<LostItem>>(emptyList()) }
    var postedStatus by remember { mutableStateOf(ItemStatus.LOST) }

    // Initialize if editing
    LaunchedEffect(itemToEdit) {
        if (itemToEdit != null) {
            viewModel.setItemToEdit(itemToEdit)
        }
    }

    // Image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onImageSelected(it) }
    }

    // Show error message
    uiState.errorMessage?.let { error ->
        LaunchedEffect(error) {
            // Show snackbar or toast
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "Edit Item" else "Add Item") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            // Status Toggle
            item {
                StatusToggle(
                    selectedStatus = uiState.status,
                    onStatusChange = viewModel::onStatusChanged
                )
            }

            // Title
            item {
                LguinahTextField(
                    value = uiState.title,
                    onValueChange = viewModel::onTitleChange,
                    label = "Title",
                    placeholder = "e.g., Keys with blue keychain",
                    leadingIcon = Icons.Default.Title,
                    isError = uiState.titleError != null,
                    errorMessage = uiState.titleError
                )
            }

            // Description
            item {
                LguinahTextField(
                    value = uiState.description,
                    onValueChange = viewModel::onDescriptionChange,
                    label = "Description",
                    placeholder = "Provide more details...",
                    leadingIcon = Icons.Default.Description,
                    singleLine = false,
                    maxLines = 5,
                    isError = uiState.descriptionError != null,
                    errorMessage = uiState.descriptionError
                )
                Text(
                    text = "${uiState.description.length}/${Constants.MAX_DESCRIPTION_LENGTH}",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }

            // Category
            item {
                CategorySelector(
                    selectedCategory = uiState.category,
                    onClick = viewModel::showCategoryDialog
                )
            }

            // Location
            item {
                LguinahTextField(
                    value = uiState.location,
                    onValueChange = viewModel::onLocationChange,
                    label = "Location",
                    placeholder = "e.g., Block B, Amphi 3",
                    leadingIcon = Icons.Default.LocationOn,
                    isError = uiState.locationError != null,
                    errorMessage = uiState.locationError
                )
            }

            // Images
            item {
                ImageSection(
                    selectedImages = uiState.selectedImages,
                    existingImageUrls = uiState.existingImageUrls,
                    canAddMore = uiState.canAddMoreImages,
                    onAddImage = { imagePickerLauncher.launch("image/*") },
                    onRemoveSelectedImage = viewModel::onImageRemoved,
                    onRemoveExistingImage = viewModel::onExistingImageRemoved
                )
            }

            // Submit Button
            item {
                PrimaryButton(
                    text = if (uiState.isLoading) {
                        if (uiState.isEditMode) "Updating..." else "Posting..."
                    } else {
                        if (uiState.isEditMode) "Save Changes" else "Post Item"
                    },
                    onClick = {
                        viewModel.submitItem { matches, status ->
                            postedStatus = status
                            if (matches.isNotEmpty()) {
                                matchedItems = matches
                                showMatchDialog = true
                            } else {
                                onItemPosted(emptyList())
                            }
                        }
                    },
                    enabled = uiState.isValid && !uiState.isLoading,
                    icon = Icons.Default.Send
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    // Category Dialog
    if (uiState.showCategoryDialog) {
        CategoryDialog(
            selectedCategory = uiState.category,
            onCategorySelected = viewModel::onCategorySelected,
            onDismiss = viewModel::hideCategoryDialog
        )
    }

    if (showMatchDialog) {
        AlertDialog(
            onDismissRequest = {
                showMatchDialog = false
                onItemPosted(emptyList())
            },
            title = { Text("Potential Match Found") },
            text = {
                Text(
                    if (postedStatus == ItemStatus.LOST) {
                        "Someone may have found your item. Check similar posts now?"
                    } else {
                        "Someone may have lost a similar item. Check similar posts now?"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showMatchDialog = false
                        onItemPosted(matchedItems)
                    }
                ) {
                    Text("Check")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showMatchDialog = false
                        onItemPosted(emptyList())
                    }
                ) {
                    Text("Later")
                }
            },
            containerColor = DarkCard
        )
    }
}

@Composable
fun StatusToggle(
    selectedStatus: ItemStatus,
    onStatusChange: (ItemStatus) -> Unit
) {
    Column {
        Text(
            text = "What happened?",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ItemStatus.values().forEach { status ->
                val isSelected = selectedStatus == status
                val backgroundColor = when {
                    isSelected && status == ItemStatus.LOST -> ErrorRed
                    isSelected && status == ItemStatus.FOUND -> AccentGreen
                    else -> DarkCard
                }
                val textColor = if (isSelected) Color.White else TextSecondary

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .background(backgroundColor, RoundedCornerShape(12.dp))
                        .clickable { onStatusChange(status) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "I ${status.displayName} something",
                        color = textColor,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CategorySelector(
    selectedCategory: Category,
    onClick: () -> Unit
) {
    Column {
        Text(
            text = "Category",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(DarkCard, RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        selectedCategory.icon,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = selectedCategory.displayName,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextSecondary
                )
            }
        }
    }
}

@Composable
fun ImageSection(
    selectedImages: List<Uri>,
    existingImageUrls: List<String>,
    canAddMore: Boolean,
    onAddImage: () -> Unit,
    onRemoveSelectedImage: (Uri) -> Unit,
    onRemoveExistingImage: (String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val totalImages = selectedImages.size + existingImageUrls.size
            Text(
                text = "Photos",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "$totalImages/${Constants.MAX_IMAGES}",
                fontSize = 14.sp,
                color = TextSecondary
            )
        }

        Spacer(Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Add Image Button
            if (canAddMore) {
                item {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .border(2.dp, DarkCard, RoundedCornerShape(12.dp))
                            .clickable(onClick = onAddImage),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add Photo",
                                tint = TextSecondary,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "Add Photo",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Display Existing Images (from URLs)
            items(existingImageUrls) { url ->
                Box(
                    modifier = Modifier.size(100.dp)
                ) {
                    AsyncImage(
                        model = url,
                        contentDescription = "Existing Image",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )

                    // Remove button
                    IconButton(
                        onClick = { onRemoveExistingImage(url) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(28.dp)
                            .background(ErrorRed, RoundedCornerShape(14.dp))
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Display Newly Selected Images
            items(selectedImages) { uri ->
                Box(
                    modifier = Modifier.size(100.dp)
                ) {
                    AsyncImage(
                        model = uri,
                        contentDescription = "Selected Image",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )

                    // Remove button
                    IconButton(
                        onClick = { onRemoveSelectedImage(uri) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(28.dp)
                            .background(ErrorRed, RoundedCornerShape(14.dp))
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryDialog(
    selectedCategory: Category,
    onCategorySelected: (Category) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Category",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(Category.values().toList()) { category ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCategorySelected(category) }
                            .background(
                                if (category == selectedCategory) PrimaryBlue.copy(alpha = 0.2f)
                                else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            category.icon,
                            contentDescription = null,
                            tint = if (category == selectedCategory) PrimaryBlue else TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = category.displayName,
                            color = if (category == selectedCategory) PrimaryBlue else Color.White
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = PrimaryBlue)
            }
        },
        containerColor = DarkCard
    )
}
