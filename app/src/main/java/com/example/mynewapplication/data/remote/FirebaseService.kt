package com.example.mynewapplication.data.remote

import com.example.mynewapplication.data.model.LostItem
import com.example.mynewapplication.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class FirebaseService {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Auth Functions
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    suspend fun signInWithEmailAndPassword(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.let {
                Result.success(it)
            } ?: Result.failure(Exception("Sign in failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createUserWithEmailAndPassword(
        email: String,
        password: String
    ): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let {
                // Create user document in Firestore
                val user = User(
                    id = it.uid,
                    email = it.email ?: "",
                    name = it.displayName ?: "",
                    createdAt = System.currentTimeMillis()
                )
                saveUser(user)
                Result.success(it)
            } ?: Result.failure(Exception("User creation failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }

    // User Functions
    suspend fun saveUser(user: User): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(user.id)
                .set(user)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUser(userId: String): Result<User> {
        return try {
            val document = firestore.collection("users")
                .document(userId)
                .get()
                .await()

            if (document.exists()) {
                val user = document.toObject(User::class.java)
                user?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("User data is null"))
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentUserData(): Result<User> {
        val currentUser = getCurrentUser()
        return if (currentUser != null) {
            getUser(currentUser.uid)
        } else {
            Result.failure(Exception("No user logged in"))
        }
    }

    // LostItem Functions
    suspend fun saveLostItem(item: LostItem): Result<String> {
        return try {
            val documentRef = if (item.id.isEmpty()) {
                firestore.collection("lostItems").document()
            } else {
                firestore.collection("lostItems").document(item.id)
            }

            val itemToSave = item.copy(id = documentRef.id)
            documentRef.set(itemToSave).await()
            Result.success(documentRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLostItem(itemId: String): Result<LostItem> {
        return try {
            val document = firestore.collection("lostItems")
                .document(itemId)
                .get()
                .await()

            if (document.exists()) {
                val item = document.toObject(LostItem::class.java)
                item?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Item data is null"))
            } else {
                Result.failure(Exception("Item not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllLostItems(
        category: String? = null,
        status: String? = null,
        limit: Int = 50
    ): Result<List<LostItem>> {
        return try {
            var query: Query = firestore.collection("lostItems")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())

            if (category != null) {
                query = query.whereEqualTo("category", category)
            }

            if (status != null) {
                query = query.whereEqualTo("status", status)
            }

            val snapshot = query.get().await()
            val items = snapshot.documents.mapNotNull { it.toObject(LostItem::class.java) }
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserLostItems(userId: String): Result<List<LostItem>> {
        return try {
            val snapshot = firestore.collection("lostItems")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val items = snapshot.documents.mapNotNull { it.toObject(LostItem::class.java) }
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateLostItem(item: LostItem): Result<Unit> {
        return try {
            firestore.collection("lostItems")
                .document(item.id)
                .set(item)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteLostItem(itemId: String): Result<Unit> {
        return try {
            firestore.collection("lostItems")
                .document(itemId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchLostItems(searchQuery: String): Result<List<LostItem>> {
        return try {
            val snapshot = firestore.collection("lostItems")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val items = snapshot.documents.mapNotNull { it.toObject(LostItem::class.java) }
                .filter { item ->
                    item.title.contains(searchQuery, ignoreCase = true) ||
                            item.description.contains(searchQuery, ignoreCase = true) ||
                            item.location.contains(searchQuery, ignoreCase = true)
                }
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

