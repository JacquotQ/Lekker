package com.jacquotq.lekker.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Locale

class AppStorage(context: Context) {

    private val gson = Gson()
    private val context = context

    private val prefs: SharedPreferences =
        context.getSharedPreferences("lekker_prefs", Context.MODE_PRIVATE)

    // ── Device ID ────────────────────────────────────────
    fun getDeviceId(): String {
        val existing = prefs.getString("device_id", null)
        if (existing != null) return existing
        val newId = java.util.UUID.randomUUID().toString()
        prefs.edit().putString("device_id", newId).apply()
        return newId
    }

    // ── Get default language based on system locale ──────
    private fun getDefaultLanguage(): String {
        val locale = Locale.getDefault()
        val lang = locale.language
        // Check if system language is Chinese
        return if (lang == "zh") "zh" else "en"
    }

    // ── Settings ──────────────────────────────────────────
    fun saveSettings(s: AppSettings) {
        prefs.edit()
            .putBoolean("use_cache", s.useCache)
            .putString("language", s.language)
            .putString("theme", s.theme)
            .apply()
    }

    fun loadSettings(): AppSettings {
        // If language has never been set, use system default
        val storedLanguage = prefs.getString("language", null)
        val language = storedLanguage ?: getDefaultLanguage()
        
        return AppSettings(
            useCache = prefs.getBoolean("use_cache", true),
            language = language,
            theme    = prefs.getString("theme", "peach") ?: "peach"
        )
    }

    // ── History ───────────────────────────────────────────
    fun saveHistory(entries: List<HistoryEntry>) {
        prefs.edit().putString("history", gson.toJson(entries)).apply()
    }

    fun loadHistory(): MutableList<HistoryEntry> {
        val json = prefs.getString("history", null) ?: return mutableListOf()
        return try {
            gson.fromJson(json, object : TypeToken<MutableList<HistoryEntry>>() {}.type)
        } catch (e: Exception) { mutableListOf() }
    }

    // ── Favorites ─────────────────────────────────────────
    fun saveFavorites(entries: List<FavoriteEntry>) {
        prefs.edit().putString("favorites", gson.toJson(entries)).apply()
    }

    fun loadFavorites(): MutableList<FavoriteEntry> {
        val json = prefs.getString("favorites", null) ?: return mutableListOf()
        return try {
            gson.fromJson(json, object : TypeToken<MutableList<FavoriteEntry>>() {}.type)
        } catch (e: Exception) { mutableListOf() }
    }

    // ── Cache ─────────────────────────────────────────────
    fun getCached(key: String): String? = prefs.getString("cache_$key", null)
    fun putCache(key: String, value: String) {
        prefs.edit().putString("cache_$key", value).apply()
    }

    // ── User Profile ──────────────────────────────────────
    fun saveProfile(p: UserProfile) {
        prefs.edit()
            .putFloat("w_etymology", p.etymologyWeight.toFloat())
            .putFloat("w_stories", p.storiesWeight.toFloat())
            .putFloat("w_connections", p.connectionsWeight.toFloat())
            .putFloat("w_formal", p.formalWeight.toFloat())
            .apply()
    }

    fun loadProfile() = UserProfile(
        etymologyWeight   = prefs.getFloat("w_etymology", 1f).toDouble(),
        storiesWeight     = prefs.getFloat("w_stories", 1f).toDouble(),
        connectionsWeight = prefs.getFloat("w_connections", 1f).toDouble(),
        formalWeight      = prefs.getFloat("w_formal", 1f).toDouble()
    )

    fun resetProfile() {
        prefs.edit()
            .remove("w_etymology").remove("w_stories")
            .remove("w_connections").remove("w_formal")
            .apply()
    }
}
