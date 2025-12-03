// File: ui/screens/detail/ItemDetailScreen.kt
package com.example.mynewapplication.ui.screens.detail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import coil.compose.AsyncImage
import com.example.mynewapplication.data.model.LostItem
import com.example.mynewapplication.data.model.ItemStatus
import com.example.mynewapplication.ui.components.InfoChip
import com.example.mynewapplication.ui.components.PrimaryButton
import com.example.mynewapplication.ui.components.StatusChip
import com.example.mynewapplication.ui.components.UserAvatar
import com.example.mynewapplication.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ItemDetailScreen(
    item: LostItem,
    onBack: () -> Unit,
    onContactClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Item Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Share */ }) {
                        Icon(Icons.Default.Share, "Share")
                    }
                    IconButton(onClick = { /* More options */ }) {
                        Icon(Icons.Default.MoreVert, "More")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Image Gallery
            if (item.imageUrls.isNotEmpty()) {
                ImageGallery(images = item.imageUrls)
            } else {
                PlaceholderImage(category = item.category.displayName)
            }

            // Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Status Badge
                StatusChip(status = item.status)

                // Title
                Text(
                    text = item.title,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    lineHeight = 36.sp
                )

                // Meta Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InfoChip(
                        icon = Icons.Default.LocationOn,
                        text = item.location
                    )
                    InfoChip(
                        icon = Icons.Default.Category,
                        text = item.category.displayName
                    )
                    InfoChip(
                        icon = Icons.Default.AccessTime,
                        text = item.getFormattedDate()
                    )
                }

                Divider(color = DarkCard)

                // Description Section
                SectionTitle(text = "Description")
                Text(
                    text = item.description,
                    fontSize = 16.sp,
                    color = TextSecondary,
                    lineHeight = 24.sp
                )

                Divider(color = DarkCard)

                // Posted By Section
                SectionTitle(text = "Posted By")
                UserInfoCard(
                    userName = item.userName,
                    userEmail = item.userEmail,
                    timestamp = item.getFormattedDate()
                )

                Divider(color = DarkCard)

                // Location Details
                SectionTitle(text = "Last Seen Location")
                LocationCard(location = item.location)

                Spacer(Modifier.height(16.dp))

                // Contact Button
                PrimaryButton(
                    text = if (item.status == ItemStatus.FOUND) "Contact Finder" else "Contact Owner",
                    onClick = onContactClick,
                    icon = Icons.Default.Chat
                )

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageGallery(images: List<String>) {
    val pagerState = rememberPagerState(initialPage = 0)


    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        HorizontalPager(
            pageCount = images.size,
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            AsyncImage(
                model = images[page],
                contentDescription = "Item image ${page + 1}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Page Indicator
        if (images.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(images.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (pagerState.currentPage == index) Color.White else Color.White.copy(alpha = 0.4f),
                                CircleShape
                            )
                    )
                }
            }
        }

        // Image Counter
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "${pagerState.currentPage + 1}/${images.size}",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun PlaceholderImage(category: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(DarkCard),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Image,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = TextTertiary
            )
            Text(
                text = "No image available",
                color = TextSecondary,
                fontSize = 16.sp
            )
            Text(
                text = category,
                color = TextTertiary,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )
}

@Composable
fun UserInfoCard(
    userName: String,
    userEmail: String,
    timestamp: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                userName = userName,
                size = 50.dp
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = userName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = userEmail,
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Posted $timestamp",
                    fontSize = 12.sp,
                    color = TextTertiary
                )
            }

            Icon(
                Icons.Default.Verified,
                contentDescription = "Verified ESTIN student",
                tint = PrimaryBlue,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun LocationCard(location: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(PrimaryBlue.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = location,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "ESTIN Campus",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }

            IconButton(
                onClick = { /* Open in maps */ }
            ) {
                Icon(
                    Icons.Default.Map,
                    contentDescription = "View on map",
                    tint = PrimaryBlue
                )
            }
        }
    }
}

// Helper function to create sample item for testing
fun getSampleItemForDetail(): LostItem {
    return LostItem(
        id = "detail_1",
        title = "Black Nike Backpack with Laptop",
        description = "Lost my black Nike backpack in the cafeteria on Tuesday around 2 PM. It contains my laptop, notebooks, and some personal items. The backpack has a small tear on the left side pocket. Please contact me if you've seen it!",
        category = com.example.mynewapplication.data.model.Category.BAGS,
        location = "University Cafeteria",
        timestamp = System.currentTimeMillis() - 86400000,
        status = ItemStatus.LOST,
        userId = "user123",
        userName = "Ahmed Benali",
        userEmail = "ahmed.benali@estin.dz",
        imageUrls = emptyList() // Add image URLs when available
    )
}