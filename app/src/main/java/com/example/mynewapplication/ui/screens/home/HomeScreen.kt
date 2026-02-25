package com.example.mynewapplication.ui.screens.home


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mynewapplication.data.model.LostItem
import com.example.mynewapplication.ui.screens.home.components.CategoryFilterSection
import com.example.mynewapplication.ui.screens.home.components.ItemCard
import com.example.mynewapplication.ui.screens.home.components.SearchTopBar
import com.example.mynewapplication.ui.components.LoadingIndicator
import com.example.mynewapplication.ui.components.EmptyState

@Composable
fun HomeScreen(
    onAddClick: () -> Unit,
    onItemClick: (LostItem) -> Unit = {},
    onContactClick: (LostItem) -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        SearchTopBar(
            searchQuery = uiState.searchQuery,
            onSearchChange = viewModel::onSearchQueryChange,
            onFilterClick = viewModel::toggleFilters
        )

        if (uiState.showFilters) {
            CategoryFilterSection(
                selectedCategory = uiState.selectedCategory,
                selectedStatus = uiState.selectedStatus,
                onCategorySelect = viewModel::onCategorySelected,
                onStatusSelect = viewModel::onStatusSelected,
                onClearFilters = viewModel::clearFilters
            )
        }

        when {
            uiState.isLoading -> LoadingIndicator()
            uiState.error != null && uiState.filteredItems.isEmpty() -> EmptyState(
                icon = Icons.Default.Search,
                title = "Couldn't load feed",
                message = uiState.error ?: "Failed to load items"
            )
            uiState.filteredItems.isEmpty() -> EmptyState(
                icon = Icons.Default.Search,
                title = "No items found",
                message = "Try adjusting your filters or search query"
            )
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.filteredItems) { item ->
                        ItemCard(
                            item = item,
                            onClick = { onItemClick(item) },
                            onContactClick = { onContactClick(item) }
                        )
                    }
                }
            }
        }
    }
}
