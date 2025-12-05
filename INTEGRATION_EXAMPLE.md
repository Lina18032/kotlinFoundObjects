# Firebase and Cloudinary Integration Examples

This document shows how to use the Firebase and Cloudinary services in your ViewModels and screens.

## FirebaseService Usage

### Authentication

```kotlin
// Sign in
val result = firebaseService.signInWithEmailAndPassword(email, password)
result.fold(
    onSuccess = { user -> 
        // User signed in successfully
    },
    onFailure = { error -> 
        // Handle error
    }
)

// Sign up
val result = firebaseService.createUserWithEmailAndPassword(email, password)
result.fold(
    onSuccess = { user -> 
        // User created successfully
    },
    onFailure = { error -> 
        // Handle error
    }
)

// Get current user
val currentUser = firebaseService.getCurrentUser()
if (currentUser != null) {
    // User is logged in
}

// Sign out
firebaseService.signOut()
```

### Firestore Operations

```kotlin
// Save a lost item
val item = LostItem(
    title = "Lost Phone",
    description = "iPhone 13",
    // ... other fields
)
val result = firebaseService.saveLostItem(item)
result.fold(
    onSuccess = { itemId -> 
        // Item saved with ID
    },
    onFailure = { error -> 
        // Handle error
    }
)

// Get all items
val result = firebaseService.getAllLostItems()
result.fold(
    onSuccess = { items -> 
        // Use items list
    },
    onFailure = { error -> 
        // Handle error
    }
)

// Get items by category
val result = firebaseService.getAllLostItems(
    category = "ELECTRONICS",
    status = "LOST"
)

// Search items
val result = firebaseService.searchLostItems("phone")
```

## CloudinaryService Usage

### Image Upload

```kotlin
// Upload single image
val result = cloudinaryService.uploadImage(imageUri, folder = "lost-items")
result.fold(
    onSuccess = { imageUrl -> 
        // Use imageUrl
    },
    onFailure = { error -> 
        // Handle error
    }
)

// Upload multiple images
val imageUris = listOf(uri1, uri2, uri3)
val result = cloudinaryService.uploadImages(imageUris, folder = "lost-items")
result.fold(
    onSuccess = { imageUrls -> 
        // Use imageUrls list
    },
    onFailure = { error -> 
        // Handle error
    }
)

// Get optimized image URL
val optimizedUrl = cloudinaryService.getOptimizedImageUrl(
    originalUrl = imageUrl,
    width = 400,
    height = 300
)
```

## Example: Complete Item Creation Flow

```kotlin
viewModelScope.launch {
    // 1. Get current user
    val currentUser = firebaseService.getCurrentUser()
        ?: return@launch // User not logged in
    
    // 2. Get user data
    val userData = firebaseService.getCurrentUserData().getOrElse {
        // Handle error
        return@launch
    }
    
    // 3. Upload images
    val imageUrls = if (imageUris.isNotEmpty()) {
        cloudinaryService.uploadImages(imageUris).getOrElse {
            // Handle upload error
            return@launch
        }
    } else {
        emptyList()
    }
    
    // 4. Create and save item
    val item = LostItem(
        title = title,
        description = description,
        imageUrls = imageUrls,
        userId = currentUser.uid,
        userName = userData.name,
        userEmail = userData.email,
        // ... other fields
    )
    
    // 5. Save to Firestore
    firebaseService.saveLostItem(item).fold(
        onSuccess = { itemId ->
            // Success!
        },
        onFailure = { error ->
            // Handle error
        }
    )
}
```

## ViewModel Pattern

```kotlin
class MyViewModel(application: Application) : AndroidViewModel(application) {
    private val firebaseService = FirebaseService()
    private val cloudinaryService = CloudinaryService(application)
    
    // Use services in your functions
    fun doSomething() {
        viewModelScope.launch {
            // Your code here
        }
    }
}
```

## Error Handling Best Practices

Always handle both success and failure cases:

```kotlin
result.fold(
    onSuccess = { data -> 
        // Handle success
    },
    onFailure = { error -> 
        // Log error
        Log.e("TAG", "Error: ${error.message}", error)
        // Update UI state
        _uiState.value = _uiState.value.copy(
            errorMessage = error.message ?: "An error occurred"
        )
    }
)
```

## Notes

- All Firebase and Cloudinary operations are suspend functions
- Use `viewModelScope.launch` to call them from ViewModels
- Always check for null current user before operations
- Handle errors gracefully and show user-friendly messages
- Consider adding loading states for better UX

