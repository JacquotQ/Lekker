package com.jacquotq.lekker.data

import java.util.UUID

data class HistoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val query: String,
    val response: String,
    val model: String = "qwen-turbo",
    val timestamp: Long = System.currentTimeMillis(),
    val cached: Boolean = false
)

data class FavoriteEntry(
    val id: String,
    val query: String,
    val response: String,
    val timestamp: Long,
    val nextReview: Long = System.currentTimeMillis(),
    val interval: Int = 1,
    val easeFactor: Double = 2.5
)

data class AppSettings(
    val useCache: Boolean = true,
    val language: String = "en",   // "zh" or "en"
    val theme: String = "peach"    // "peach" or "bw"
)

data class UserProfile(
    val etymologyWeight: Double = 1.0,
    val storiesWeight: Double = 1.0,
    val connectionsWeight: Double = 1.0,
    val formalWeight: Double = 1.0
)
