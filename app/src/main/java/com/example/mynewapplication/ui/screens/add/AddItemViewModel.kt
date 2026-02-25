package com.example.mynewapplication.ui.screens.add

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mynewapplication.data.model.Category
import com.example.mynewapplication.data.model.ItemStatus
import com.example.mynewapplication.data.model.LostItem
import com.example.mynewapplication.data.remote.CloudinaryService
import com.example.mynewapplication.data.remote.FirebaseService
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

class AddItemViewModel(application: Application) : AndroidViewModel(application) {

    private val firebaseService = FirebaseService()
    private val cloudinaryService = CloudinaryService(application)

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
                // Get current user
                val currentUser = firebaseService.getCurrentUser()
                if (currentUser == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Please log in to post items"
                    )
                    return@launch
                }

                // Get user data
                val userData = firebaseService.getCurrentUserData().getOrElse {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to get user data: ${it.message}"
                    )
                    return@launch
                }

                // Upload images to Cloudinary
                val imageUrls = if (_uiState.value.selectedImages.isNotEmpty()) {
                    val uploadResult = cloudinaryService.uploadImages(
                        _uiState.value.selectedImages,
                        folder = "lost-items"
                    )
                    uploadResult.getOrElse {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Failed to upload images: ${it.message}"
                        )
                        return@launch
                    }
                } else {
                    emptyList()
                }

                // Create item
                val newItem = LostItem(
                    id = "", // Will be generated by Firebase
                    title = _uiState.value.title,
                    description = _uiState.value.description,
                    category = _uiState.value.category,
                    location = _uiState.value.location,
                    status = _uiState.value.status,
                    timestamp = System.currentTimeMillis(),
                    userId = currentUser.uid,
                    userName = userData.name.ifEmpty { currentUser.displayName ?: "User" },
                    userEmail = userData.email.ifEmpty { currentUser.email ?: "" },
                    imageUrls = imageUrls
                )

                // Save to Firebase
                val saveResult = firebaseService.saveLostItem(newItem)
                saveResult.fold(
                    onSuccess = { itemId ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSuccess = true
                )
                // Reset form and navigate back
                resetForm()
                onSuccess()
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Failed to save item: ${error.message}"
                        )
                    }
                )

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