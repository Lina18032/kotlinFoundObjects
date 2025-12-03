package com.example.mynewapplication.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mynewapplication.data.model.Category
import com.example.mynewapplication.data.model.ItemStatus
import com.example.mynewapplication.data.model.LostItem
import com.example.mynewapplication.utils.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val items: List<LostItem> = emptyList(),
    val filteredItems: List<LostItem> = emptyList(),
    val selectedCategory: Category? = null,
    val selectedStatus: ItemStatus? = null,
    val searchQuery: String = "",
    val showFilters: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadItems()
    }

    private fun loadItems() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Sample data - will be replaced with Firebase later
            val sampleItems = listOf(
                LostItem(
                    id = "1",
                    title = "Keys with blue keychain",
                    description = "Lost my keys near Block B, have a blue keychain with ESTIN logo",
                    category = Category.KEYS,
                    location = "Block B",
                    timestamp = System.currentTimeMillis() - 7200000, // 2 hours ago
                    status = ItemStatus.LOST,
                    userId = "user1",
                    userName = "Lina Lolem",
                    userEmail = "lina.lolem@estin.dz"
                ),
                LostItem(
                    id = "2",
                    title = "Student Card",
                    description = "Found a student card in Amphi 3. Name starts with 'A'",
                    category = Category.CARDS,
                    location = "Amphi 3",
                    timestamp = System.currentTimeMillis() - 18000000, // 5 hours ago
                    status = ItemStatus.FOUND,
                    userId = "user2",
                    userName = "Ahmed Kaci",
                    userEmail = "ahmed.kaci@estin.dz"
                ),
                LostItem(
                    id = "3",
                    title = "AirPods Case",
                    description = "Lost my AirPods case near the library. White color",
                    category = Category.ELECTRONICS,
                    location = "Library",
                    timestamp = System.currentTimeMillis() - 86400000, // 1 day ago
                    status = ItemStatus.LOST,
                    userId = "user3",
                    userName = "Sarah Mansouri",
                    userEmail = "sarah.mansouri@estin.dz"
                ),
                LostItem(
                    id = "4",
                    title = "Black Backpack",
                    description = "Found a black Nike backpack in the cafeteria",
                    category = Category.BAGS,
                    location = "Cafeteria",
                    timestamp = System.currentTimeMillis() - 3600000, // 1 hour ago
                    status = ItemStatus.FOUND,
                    userId = "user4",
                    userName = "Karim Benali",
                    userEmail = "karim.benali@estin.dz"
                ),
                LostItem(
                    id = "5",
                    title = "USB Flash Drive",
                    description = "Lost 32GB SanDisk USB drive with important projects",
                    category = Category.ELECTRONICS,
                    location = "Computer Lab 2",
                    timestamp = System.currentTimeMillis() - 172800000, // 2 days ago
                    status = ItemStatus.LOST,
                    userId = "user5",
                    userName = "Yasmine Abdallah",
                    userEmail = "yasmine.abdallah@estin.dz"
                )
            )

            _uiState.value = _uiState.value.copy(
                items = sampleItems,
                filteredItems = sampleItems,
                isLoading = false
            )
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        filterItems()
    }

    fun onCategorySelected(category: Category?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        filterItems()
    }

    fun onStatusSelected(status: ItemStatus?) {
        _uiState.value = _uiState.value.copy(selectedStatus = status)
        filterItems()
    }

    fun toggleFilters() {
        _uiState.value = _uiState.value.copy(
            showFilters = !_uiState.value.showFilters
        )
    }

    fun clearFilters() {
        _uiState.value = _uiState.value.copy(
            selectedCategory = null,
            selectedStatus = null,
            searchQuery = ""
        )
        filterItems()
    }

    private fun filterItems() {
        val query = _uiState.value.searchQuery.lowercase()
        val category = _uiState.value.selectedCategory
        val status = _uiState.value.selectedStatus

        val filtered = _uiState.value.items.filter { item ->
            val matchesQuery = query.isEmpty() ||
                    item.title.lowercase().contains(query) ||
                    item.description.lowercase().contains(query) ||
                    item.location.lowercase().contains(query)

            val matchesCategory = category == null || item.category == category
            val matchesStatus = status == null || item.status == status

            matchesQuery && matchesCategory && matchesStatus
        }

        _uiState.value = _uiState.value.copy(filteredItems = filtered)
    }

    fun refreshItems() {
        loadItems()
    }
}