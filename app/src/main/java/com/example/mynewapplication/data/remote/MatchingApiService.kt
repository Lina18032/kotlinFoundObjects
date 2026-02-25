package com.example.mynewapplication.data.remote

import com.example.mynewapplication.data.model.Category
import com.example.mynewapplication.data.model.ItemStatus
import com.example.mynewapplication.data.model.LostItem
import com.example.mynewapplication.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MatchingApiService {

    suspend fun findMatchesForLostItem(item: LostItem): Result<List<LostItem>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${Constants.MATCHER_API_BASE_URL}/api/v1/match")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 10000
                readTimeout = 15000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("x-api-key", Constants.MATCHER_API_KEY)
            }

            val body = JSONObject().apply {
                put("id", item.id)
                put("userId", item.userId)
                put("userName", item.userName)
                put("userEmail", item.userEmail)
                put("title", item.title)
                put("description", item.description)
                put("category", mapCategoryToApi(item.category))
                put("location", item.location)
                put("timestamp", item.timestamp)
                put("imageURLs", JSONArray(item.imageUrls))
                put("status", "LOST")
                put("resolved", false)
            }.toString()

            connection.outputStream.use { it.write(body.toByteArray()) }
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                return@withContext Result.failure(Exception("Matcher API failed: HTTP $responseCode"))
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            val matches = json.optJSONArray("matches") ?: JSONArray()

            Result.success(parseMatches(matches))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseMatches(matches: JSONArray): List<LostItem> {
        val items = mutableListOf<LostItem>()
        for (i in 0 until matches.length()) {
            val match = matches.optJSONObject(i) ?: continue
            val imageUrls = mutableListOf<String>()
            val imagesArray = match.optJSONArray("imageURLs") ?: JSONArray()
            for (j in 0 until imagesArray.length()) {
                imageUrls.add(imagesArray.optString(j))
            }

            items.add(
                LostItem(
                    id = match.optString("id"),
                    title = match.optString("title"),
                    description = match.optString("description"),
                    category = mapApiCategoryToApp(match.optString("category")),
                    location = match.optString("location"),
                    timestamp = match.optLong("timestamp"),
                    status = ItemStatus.FOUND,
                    userId = match.optString("userId"),
                    userName = match.optString("userName"),
                    userEmail = match.optString("userEmail"),
                    imageUrls = imageUrls
                )
            )
        }
        return items
    }

    private fun mapCategoryToApi(category: Category): String = when (category) {
        Category.KEYS -> "KEYS"
        Category.CARDS -> "STUDENT_CARD"
        Category.ELECTRONICS -> "ELECTRONICS"
        Category.BAGS -> "BAG"
        Category.DOCUMENTS -> "DOCUMENTS"
        Category.CLOTHING -> "CLOTHING"
        Category.PHONE -> "PHONE"
        else -> "OTHER"
    }

    private fun mapApiCategoryToApp(category: String): Category = when (category.uppercase()) {
        "KEYS" -> Category.KEYS
        "STUDENT_CARD" -> Category.CARDS
        "PHONE" -> Category.PHONE
        "BAG" -> Category.BAGS
        "DOCUMENTS" -> Category.DOCUMENTS
        "ELECTRONICS" -> Category.ELECTRONICS
        "CLOTHING" -> Category.CLOTHING
        else -> Category.OTHER
    }
}
