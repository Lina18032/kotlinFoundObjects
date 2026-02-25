package com.example.mynewapplication.data.remote

import android.content.Context
import com.example.mynewapplication.data.model.ChatConversation
import com.example.mynewapplication.data.model.ChatMessage
import com.example.mynewapplication.data.model.LostItem
import com.example.mynewapplication.data.model.User
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.runBlocking
import android.util.Log

class FirebaseService {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Auth Functions
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    /**
     * Get Google Sign-In client
     * You need to get the SHA-1 fingerprint from your app and add it to Firebase Console
     * The Web Client ID is automatically read from strings.xml (default_web_client_id)
     */
    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val clientId = try {
            context.getString(com.example.mynewapplication.R.string.default_web_client_id)
        } catch (e: Exception) {
            throw IllegalStateException(
                "Web Client ID not found in strings.xml. " +
                "Please add 'default_web_client_id' to strings.xml. " +
                "You can find it in google-services.json (client_type: 3) or " +
                "Firebase Console -> Project Settings -> Your App -> OAuth 2.0 Client IDs"
            )
        }
        
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(clientId)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    /**
     * Sign in with Google account
     * Call this after getting the GoogleSignInAccount from GoogleSignInClient
     */
    suspend fun signInWithGoogle(account: GoogleSignInAccount): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            val result = auth.signInWithCredential(credential).await()
            result.user?.let { user ->
                // Create or update user document in Firestore
                val userData = User(
                    id = user.uid,
                    email = user.email ?: "",
                    name = user.displayName ?: account.displayName ?: "",
                    profileImageUrl = user.photoUrl?.toString(),
                    createdAt = System.currentTimeMillis()
                )
                saveUser(userData)
                Result.success(user)
            } ?: Result.failure(Exception("Google sign in failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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

    fun signOut(context: Context? = null) {
        auth.signOut()
        if (context != null) {
            try {
                getGoogleSignInClient(context).signOut()
            } catch (e: Exception) {
                // Ignore
            }
        }
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
        limit: Int = 100
    ): Result<List<LostItem>> {
        return try {
            val snapshot = try {
                var orderedQuery: Query = firestore.collection("lostItems")
                if (category != null) {
                    orderedQuery = orderedQuery.whereEqualTo("category", category)
                }
                if (status != null) {
                    orderedQuery = orderedQuery.whereEqualTo("status", status)
                }
                orderedQuery.orderBy("timestamp", Query.Direction.DESCENDING).limit(limit.toLong()).get().await()
            } catch (e: Exception) {
                // Secondary fallback: omit orderBy to avoid FAILED_PRECONDITION
                var fallbackQuery: Query = firestore.collection("lostItems")
                if (category != null) fallbackQuery = fallbackQuery.whereEqualTo("category", category)
                if (status != null) fallbackQuery = fallbackQuery.whereEqualTo("status", status)
                fallbackQuery.limit(limit.toLong()).get().await()
            }

            var items = snapshot.documents
                .mapNotNull { it.toObject(LostItem::class.java) }
                .sortedByDescending { it.timestamp }
            Result.success(items)
        } catch (e: Exception) {
            // Absolute fallback: fetch all and filter/sort locally (good for small/medium collections)
            try {
                val allDocs = firestore.collection("lostItems").get().await()
                var mapped = allDocs.documents.mapNotNull { it.toObject(LostItem::class.java) }
                if (category != null) mapped = mapped.filter { it.category.name == category }
                if (status != null) mapped = mapped.filter { it.status.name == status }
                mapped = mapped.sortedByDescending { it.timestamp }.take(limit)
                if (mapped.isEmpty() && allDocs.documents.isEmpty()) {
                    Result.failure(e) // Original exception is more informative if completely empty
                } else {
                    Result.success(mapped)
                }
            } catch (inner: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getUserLostItems(userId: String): Result<List<LostItem>> {
        return try {
            val snapshot = try {
                firestore.collection("lostItems")
                    .whereEqualTo("userId", userId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()
            } catch (e: Exception) {
                firestore.collection("lostItems")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()
            }

            val items = snapshot.documents
                .mapNotNull { it.toObject(LostItem::class.java) }
                .sortedByDescending { it.timestamp }
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

    // Chat Functions
    /**
     * Get or create a conversation between current user and item owner
     */
    suspend fun getOrCreateConversation(itemId: String, otherUserId: String): Result<String> {
        return try {
            val currentUser = getCurrentUser() ?: return Result.failure(Exception("Not authenticated"))
            val currentUserId = currentUser.uid

            // Check if conversation already exists
            val existingConversations = firestore.collection("conversations")
                .whereArrayContains("participants", currentUserId)
                .get()
                .await()

            val existing = existingConversations.documents.firstOrNull { doc ->
                val participants = doc.get("participants") as? List<*>
                participants?.contains(currentUserId) == true && 
                participants?.contains(otherUserId) == true &&
                doc.getString("itemId") == itemId
            }

            if (existing != null) {
                Result.success(existing.id)
            } else {
                // Create new conversation
                val conversation = hashMapOf(
                    "itemId" to itemId,
                    "participants" to listOf(currentUserId, otherUserId),
                    "createdAt" to System.currentTimeMillis(),
                    "updatedAt" to System.currentTimeMillis()
                )
                val docRef = firestore.collection("conversations").document()
                docRef.set(conversation).await()
                Result.success(docRef.id)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all conversations for current user
     */
    suspend fun getUserConversations(userId: String): Result<List<ChatConversation>> {
        return try {
            val snapshot = try {
                firestore.collection("conversations")
                    .whereArrayContains("participants", userId)
                    .orderBy("updatedAt", Query.Direction.DESCENDING)
                    .get()
                    .await()
            } catch (e: Exception) {
                firestore.collection("conversations")
                    .whereArrayContains("participants", userId)
                    .get()
                    .await()
            }

            val conversations = mutableListOf<ChatConversation>()
            
            for (doc in snapshot.documents) {
                val itemId = doc.getString("itemId") ?: continue
                val participants = (doc.get("participants") as? List<*>)?.mapNotNull { it as? String } ?: continue
                
                // Get last message
                val lastMessageSnapshot = try {
                    firestore.collection("messages")
                        .whereEqualTo("conversationId", doc.id)
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .limit(1)
                        .get()
                        .await()
                } catch (e: Exception) {
                    firestore.collection("messages")
                        .whereEqualTo("conversationId", doc.id)
                        .limit(50)
                        .get()
                        .await()
                }
                
                val lastMessage = lastMessageSnapshot.documents
                    .mapNotNull { it.toObject(ChatMessage::class.java) }
                    .maxByOrNull { it.timestamp }
                
                // Get other participant info
                val otherUserId = participants.firstOrNull { it != userId }
                val otherUser = otherUserId?.let { getUser(it).getOrNull() }

                // Get item info
                val item = getLostItem(itemId).getOrNull()
                
                conversations.add(
                    ChatConversation(
                        id = doc.id,
                        itemId = itemId,
                        participants = participants,
                        lastMessage = lastMessage?.copy(
                            senderName = if (lastMessage.senderId == userId) "Me" 
                                         else (otherUser?.name ?: lastMessage.senderName)
                        ),
                        updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis(),
                        otherUserName = otherUser?.name ?: "User",
                        itemTitle = item?.title ?: "Item"
                    )
                )
            }
            
            Result.success(conversations.sortedByDescending { it.updatedAt })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun listenToConversations(userId: String): Flow<List<ChatConversation>> = callbackFlow {
        val listener = firestore.collection("conversations")
            .whereArrayContains("participants", userId)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    // We run this in runBlocking to reuse the suspend functions, 
                    // though it's not ideal for high frequency updates, it's consistent with existing logic.
                    // In a production app, we would cache user/item data more effectively.
                    val conversations = runBlocking {
                        snapshot.documents.mapNotNull { doc ->
                            try {
                                val itemId = doc.getString("itemId") ?: return@mapNotNull null
                                val participants = (doc.get("participants") as? List<*>)?.mapNotNull { it as? String } ?: return@mapNotNull null
                                
                                val lastMessageSnapshot = firestore.collection("messages")
                                    .whereEqualTo("conversationId", doc.id)
                                    .orderBy("timestamp", Query.Direction.DESCENDING)
                                    .limit(1)
                                    .get()
                                    .await()
                                
                                val lastMessage = lastMessageSnapshot.documents
                                    .mapNotNull { it.toObject(ChatMessage::class.java) }
                                    .firstOrNull()
                                
                                val otherUserId = participants.firstOrNull { it != userId }
                                val otherUser = otherUserId?.let { getUser(it).getOrNull() }
                                val item = getLostItem(itemId).getOrNull()
                                
                                ChatConversation(
                                    id = doc.id,
                                    itemId = itemId,
                                    participants = participants,
                                    lastMessage = lastMessage?.copy(
                                        senderName = if (lastMessage.senderId == userId) "Me" 
                                                     else (otherUser?.name ?: lastMessage.senderName)
                                    ),
                                    updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis(),
                                    otherUserName = otherUser?.name ?: "User",
                                    itemTitle = item?.title ?: "Item"
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }.sortedByDescending { it.updatedAt }
                    }
                    trySend(conversations)
                }
            }
        awaitClose { listener.remove() }
    }

    fun listenToMessages(conversationId: String): Flow<List<ChatMessage>> = callbackFlow {
        val listener = firestore.collection("messages")
            .whereEqualTo("conversationId", conversationId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { 
                        it.toObject(ChatMessage::class.java) 
                    }.sortedBy { it.timestamp }
                    trySend(messages)
                }
            }
        awaitClose { listener.remove() }
    }

    fun listenToNewItems(): Flow<LostItem> = callbackFlow {
        val listener = firestore.collection("lostItems")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                
                if (snapshot != null && !snapshot.isEmpty) {
                    val change = snapshot.documentChanges.firstOrNull { it.type == DocumentChange.Type.ADDED }
                    if (change != null) {
                        val item = change.document.toObject(LostItem::class.java)
                        trySend(item)
                    }
                }
            }
        awaitClose { listener.remove() }
    }

    /**
     * Get all messages for a conversation
     */
    suspend fun getConversationMessages(conversationId: String): Result<List<ChatMessage>> {
        return try {
            val snapshot = try {
                firestore.collection("messages")
                    .whereEqualTo("conversationId", conversationId)
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .get()
                    .await()
            } catch (e: Exception) {
                firestore.collection("messages")
                    .whereEqualTo("conversationId", conversationId)
                    .get()
                    .await()
            }

            val messages = snapshot.documents
                .mapNotNull { it.toObject(ChatMessage::class.java) }
                .sortedBy { it.timestamp }
            Result.success(messages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send a message
     */
    suspend fun sendMessage(
        conversationId: String,
        text: String,
        senderName: String
    ): Result<String> {
        return try {
            val currentUser = getCurrentUser() ?: return Result.failure(Exception("Not authenticated"))
            
            val message = hashMapOf(
                "conversationId" to conversationId,
                "senderId" to currentUser.uid,
                "senderName" to senderName,
                "text" to text,
                "timestamp" to System.currentTimeMillis(),
                "isRead" to false
            )
            
            val docRef = firestore.collection("messages").document()
            docRef.set(message).await()
            
            // Update conversation timestamp
            firestore.collection("conversations")
                .document(conversationId)
                .update("updatedAt", System.currentTimeMillis())
                .await()
            
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mark messages as read
     */
    suspend fun markMessagesAsRead(conversationId: String, userId: String): Result<Unit> {
        return try {
            val snapshot = firestore.collection("messages")
                .whereEqualTo("conversationId", conversationId)
                .whereEqualTo("isRead", false)
                .get()
                .await()

            val batch = firestore.batch()
            for (doc in snapshot.documents) {
                val message = doc.toObject(ChatMessage::class.java)
                if (message?.senderId != userId) {
                    batch.update(doc.reference, "isRead", true)
                }
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update user profile
     */
    suspend fun updateUserProfile(userId: String, name: String, phoneNumber: String?): Result<Unit> {
        return try {
            val updates = hashMapOf<String, Any>(
                "name" to name
            )
            if (phoneNumber != null) {
                updates["phoneNumber"] = phoneNumber
            }
            
            firestore.collection("users")
                .document(userId)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
