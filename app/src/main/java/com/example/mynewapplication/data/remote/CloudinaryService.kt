package com.example.mynewapplication.data.remote

import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CloudinaryService(private val context: Context) {
    
    companion object {
        // These will need to be configured with your Cloudinary credentials
        // You can get these from https://cloudinary.com/console
        private const val CLOUD_NAME = "dgyide6df"
        private const val API_KEY = "552236843731885"
        private const val API_SECRET = "GjrVf36yWMTWTM7mvtvbl4TJmZU"
        
        private var isInitialized = false
        
        fun initialize(context: Context) {
            if (!isInitialized) {
                val config = mapOf(
                    "cloud_name" to CLOUD_NAME,
                    "api_key" to API_KEY,
                    "api_secret" to API_SECRET
                )
                MediaManager.init(context, config)
                isInitialized = true
            }
        }
    }

    init {
        initialize(context)
    }

    /**
     * Upload a single image to Cloudinary
     * @param imageUri The URI of the image to upload
     * @param folder Optional folder path in Cloudinary (e.g., "lost-items")
     * @return The uploaded image URL
     */
    suspend fun uploadImage(imageUri: Uri, folder: String = "lost-items"): Result<String> {
        return suspendCancellableCoroutine { continuation ->
            val requestId = MediaManager.get().upload(imageUri)
                .option("folder", folder)
                .option("resource_type", "image")
                .option("transformation", "w_800,h_600,c_fill,q_auto,f_auto") // Optimize image
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        // Upload started
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        // Upload progress
                    }

                    override fun onSuccess(requestId: String, resultData: Map<Any?, Any?>) {
                        val url = resultData["url"] as? String
                        if (url != null) {
                            continuation.resume(Result.success(url))
                        } else {
                            continuation.resume(Result.failure(Exception("Upload succeeded but no URL returned")))
                        }
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        continuation.resume(Result.failure(Exception(error.description)))
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        continuation.resume(Result.failure(Exception("Upload rescheduled: ${error.description}")))
                    }
                })
                .dispatch()
            
            continuation.invokeOnCancellation {
                MediaManager.get().cancelRequest(requestId)
            }
        }
    }

    /**
     * Upload multiple images to Cloudinary
     * @param imageUris List of image URIs to upload
     * @param folder Optional folder path in Cloudinary
     * @return List of uploaded image URLs in the same order
     */
    suspend fun uploadImages(
        imageUris: List<Uri>,
        folder: String = "lost-items"
    ): Result<List<String>> {
        return try {
            val uploadResults = imageUris.map { uri ->
                uploadImage(uri, folder)
            }
            
            val urls = mutableListOf<String>()
            val errors = mutableListOf<Exception>()
            
            uploadResults.forEach { result ->
                result.fold(
                    onSuccess = { url -> urls.add(url) },
                    onFailure = { error -> errors.add(error) }
                )
            }
            
            if (errors.isNotEmpty()) {
                Result.failure(Exception("Some uploads failed: ${errors.joinToString { it.message }}"))
            } else {
                Result.success(urls)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete an image from Cloudinary
     * Note: This requires the public_id of the image
     * @param publicId The public ID of the image to delete
     */
    suspend fun deleteImage(publicId: String): Result<Unit> {
        return suspendCancellableCoroutine { continuation ->
            // Note: Cloudinary Android SDK doesn't have a direct delete method
            // You would need to use the REST API or server-side implementation
            // For now, we'll return a placeholder
            continuation.resume(Result.failure(Exception("Delete functionality requires server-side implementation")))
        }
    }

    /**
     * Get optimized image URL with transformations
     * @param originalUrl The original Cloudinary URL
     * @param width Desired width
     * @param height Desired height
     * @return Optimized image URL
     */
    fun getOptimizedImageUrl(
        originalUrl: String,
        width: Int = 400,
        height: Int = 300
    ): String {
        // If it's already a Cloudinary URL, we can add transformations
        // Otherwise, return the original URL
        return if (originalUrl.contains("cloudinary.com")) {
            originalUrl.replace("/upload/", "/upload/w_$width,h_$height,c_fill,q_auto,f_auto/")
        } else {
            originalUrl
        }
    }
}

