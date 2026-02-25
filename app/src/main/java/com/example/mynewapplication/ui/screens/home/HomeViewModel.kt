package com.example.mynewapplication.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mynewapplication.data.model.Category
import com.example.mynewapplication.data.model.ItemStatus
import com.example.mynewapplication.data.model.LostItem
import com.example.mynewapplication.data.remote.FirebaseService
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

    private val firebaseService = FirebaseService()
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadItems()
    }

    private fun loadItems() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val result = firebaseService.getAllLostItems(limit = 100)
                result.fold(
                    onSuccess = { items ->
                        _uiState.value = _uiState.value.copy(
                            items = items,
                            filteredItems = items,
                            isLoading = false
                        )
                        filterItems()
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            items = emptyList(),
                            filteredItems = emptyList(),
                            isLoading = false,
                            error = error.message ?: "Failed to load items"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load items"
                )
            }
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
