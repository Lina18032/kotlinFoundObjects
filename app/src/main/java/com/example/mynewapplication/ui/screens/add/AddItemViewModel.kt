package com.example.mynewapplication.ui.screens.add

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mynewapplication.data.model.Category
import com.example.mynewapplication.data.model.ItemStatus
import com.example.mynewapplication.data.model.LostItem
import com.example.mynewapplication.utils.Constants
import com.example.mynewapplication.utils.ValidationUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AddItemUiState(
    val title: String = "",
    val titleError: String? = null,

    val description: String = "",
    val descriptionError: String? = null,

    val location: String = "",
    val locationError: String? = null,

    val category: Category = Category.OTHER,
    val status: ItemStatus = ItemStatus.LOST,

    val selectedImages: List<Uri> = emptyList(),

    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,

    val showCategoryDialog: Boolean = false
) {
    val isValid: Boolean
        get() = title.isNotBlank() &&
                description.isNotBlank() &&
                location.isNotBlank() &&
                titleError == null &&
                descriptionError == null &&
                locationError == null

    val canAddMoreImages: Boolean
        get() = selectedImages.size < Constants.MAX_IMAGES
}

class AddItemViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AddItemUiState())
    val uiState: StateFlow<AddItemUiState> = _uiState.asStateFlow()

    fun onTitleChange(title: String) {
        _uiState.value = _uiState.value.copy(
            title = title,
            titleError = if (title.isNotEmpty()) {
                ValidationUtils.validateTitle(title).errorMessage
            } else null
        )
    }

    fun onDescriptionChange(description: String) {
        _uiState.value = _uiState.value.copy(
            description = description,
            descriptionError = if (description.isNotEmpty()) {
                ValidationUtils.validateDescription(description).errorMessage
            } else null
        )
    }

    fun onLocationChange(location: String) {
        _uiState.value = _uiState.value.copy(
            location = location,
            locationError = if (location.isNotEmpty()) {
                ValidationUtils.validateLocation(location).errorMessage
            } else null
        )
    }

    fun onCategorySelected(category: Category) {
        _uiState.value = _uiState.value.copy(
            category = category,
            showCategoryDialog = false
        )
    }

    fun onStatusChanged(status: ItemStatus) {
        _uiState.value = _uiState.value.copy(status = status)
    }

    fun onImageSelected(uri: Uri) {
        val currentImages = _uiState.value.selectedImages
        if (currentImages.size < Constants.MAX_IMAGES) {
            _uiState.value = _uiState.value.copy(
                selectedImages = currentImages + uri
            )
        }
    }

    fun onImageRemoved(uri: Uri) {
        _uiState.value = _uiState.value.copy(
            selectedImages = _uiState.value.selectedImages.filter { it != uri }
        )
    }

    fun showCategoryDialog() {
        _uiState.value = _uiState.value.copy(showCategoryDialog = true)
    }

    fun hideCategoryDialog() {
        _uiState.value = _uiState.value.copy(showCategoryDialog = false)
    }

    fun validateAllFields(): Boolean {
        val titleValidation = ValidationUtils.validateTitle(_uiState.value.title)
        val descriptionValidation = ValidationUtils.validateDescription(_uiState.value.description)
        val locationValidation = ValidationUtils.validateLocation(_uiState.value.location)

        _uiState.value = _uiState.value.copy(
            titleError = titleValidation.errorMessage,
            descriptionError = descriptionValidation.errorMessage,
            locationError = locationValidation.errorMessage
        )

        return titleValidation.isValid &&
                descriptionValidation.isValid &&
                locationValidation.isValid
    }

    fun submitItem(onSuccess: () -> Unit) {
        if (!validateAllFields()) {
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // TODO: Replace with actual Firebase call
                // Simulate network delay
                kotlinx.coroutines.delay(1500)

                // Create item
                val newItem = LostItem(
                    id = System.currentTimeMillis().toString(),
                    title = _uiState.value.title,
                    description = _uiState.value.description,
                    category = _uiState.value.category,
                    location = _uiState.value.location,
                    status = _uiState.value.status,
                    timestamp = System.currentTimeMillis(),
                    userId = "current_user_id", // TODO: Get from auth
                    userName = "Current User", // TODO: Get from auth
                    userEmail = "user@estin.dz", // TODO: Get from auth
                    imageUrls = emptyList() // TODO: Upload images to Firebase Storage
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSuccess = true
                )

                // Reset form and navigate back
                kotlinx.coroutines.delay(500)
                resetForm()
                onSuccess()

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to create item"
                )
            }
        }
    }

    fun resetForm() {
        _uiState.value = AddItemUiState()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}