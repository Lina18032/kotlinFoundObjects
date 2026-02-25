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
import com.example.mynewapplication.data.remote.MatchingApiService
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
    val existingImageUrls: List<String> = emptyList(), // Store URLs of existing images when editing

    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,

    val showCategoryDialog: Boolean = false,
    val itemToEdit: LostItem? = null
) {
    val isValid: Boolean
        get() = title.isNotBlank() &&
                description.isNotBlank() &&
                location.isNotBlank() &&
                titleError == null &&
                descriptionError == null &&
                locationError == null

    val canAddMoreImages: Boolean
        get() = (selectedImages.size + existingImageUrls.size) < Constants.MAX_IMAGES

    val isEditMode: Boolean
        get() = itemToEdit != null
}

class AddItemViewModel(application: Application) : AndroidViewModel(application) {

    private val firebaseService = FirebaseService()
    private val cloudinaryService = CloudinaryService(application)
    private val matchingApiService = MatchingApiService()

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

    fun onExistingImageRemoved(imageUrl: String) {
        _uiState.value = _uiState.value.copy(
            existingImageUrls = _uiState.value.existingImageUrls.filter { it != imageUrl }
        )
    }

    fun setItemToEdit(item: LostItem) {
        _uiState.value = _uiState.value.copy(
            itemToEdit = item,
            title = item.title,
            description = item.description,
            location = item.location,
            category = item.category,
            status = item.status,
            existingImageUrls = item.imageUrls
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

    fun submitItem(onSuccess: (List<LostItem>, ItemStatus) -> Unit) {
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

                // Upload NEW images to Cloudinary
                val newImageUrls = if (_uiState.value.selectedImages.isNotEmpty()) {
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

                // Combine existing and new images
                val allImageUrls = _uiState.value.existingImageUrls + newImageUrls

                if (_uiState.value.isEditMode) {
                    val editedItem = _uiState.value.itemToEdit!!.copy(
                        title = _uiState.value.title,
                        description = _uiState.value.description,
                        category = _uiState.value.category,
                        location = _uiState.value.location,
                        status = _uiState.value.status,
                        imageUrls = allImageUrls
                    )
                    
                    val updateResult = firebaseService.updateLostItem(editedItem)
                    updateResult.fold(
                        onSuccess = {
                            val matches = findPotentialMatches(editedItem)
                            val postedStatus = editedItem.status
                            _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                            resetForm()
                            onSuccess(matches, postedStatus)
                        },
                        onFailure = { error ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = "Failed to update item: ${error.message}"
                            )
                        }
                    )
                } else {
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
                        imageUrls = allImageUrls
                    )

                    // Save to Firebase
                    val saveResult = firebaseService.saveLostItem(newItem)
                    saveResult.fold(
                        onSuccess = { itemId ->
                            val savedItem = newItem.copy(id = itemId)
                            val matches = findPotentialMatches(savedItem)
                            val postedStatus = savedItem.status
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isSuccess = true
                            )
                            resetForm()
                            onSuccess(matches, postedStatus)
                        },
                        onFailure = { error ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = "Failed to save item: ${error.message}"
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to process item"
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

    private suspend fun findPotentialMatches(item: LostItem): List<LostItem> {
        return when (item.status) {
            ItemStatus.LOST -> {
                val aiMatches = matchingApiService.findMatchesForLostItem(item).getOrNull().orEmpty()
                if (aiMatches.isNotEmpty()) {
                    aiMatches
                } else {
                    findMatchesLocally(item, targetStatus = ItemStatus.FOUND)
                }
            }
            ItemStatus.FOUND -> findMatchesLocally(item, targetStatus = ItemStatus.LOST)
        }
    }

    private suspend fun findMatchesLocally(item: LostItem, targetStatus: ItemStatus): List<LostItem> {
        val candidates = firebaseService.getAllLostItems(
            status = targetStatus.name,
            limit = 100
        ).getOrDefault(emptyList())
            .filter { it.id != item.id && it.userId != item.userId }

        return candidates
            .map { candidate -> candidate to quickSimilarity(item, candidate) }
            .filter { (_, score) -> score >= 40 }
            .sortedByDescending { (_, score) -> score }
            .map { (candidate, _) -> candidate }
            .take(5)
    }

    private fun quickSimilarity(source: LostItem, candidate: LostItem): Int {
        var score = 0
        if (source.category == candidate.category) score += 40
        if (source.location.equals(candidate.location, ignoreCase = true)) score += 25

        val sourceTokens = tokenize(source.title + " " + source.description)
        val candidateTokens = tokenize(candidate.title + " " + candidate.description)
        val overlap = sourceTokens.intersect(candidateTokens).size
        if (overlap > 0) {
            score += minOf(35, overlap * 8)
        }

        return score.coerceIn(0, 100)
    }

    private fun tokenize(text: String): Set<String> {
        return text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 2 }
            .toSet()
    }
}
