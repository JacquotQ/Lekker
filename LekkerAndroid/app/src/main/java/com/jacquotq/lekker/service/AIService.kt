package com.jacquotq.lekker.service

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.jacquotq.lekker.data.UserProfile
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class AIService {

    // Backend URL — update after deploying Cloudflare Worker
    private val backendUrl = "https://lekker-api.jonstark186.workers.dev/api/query"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    fun streamQuery(
        query: String,
        profile: UserProfile,
        language: String,
        deviceId: String,
        onToken: (String) -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) {
        val bodyMap = mapOf(
            "query" to query,
            "deviceId" to deviceId,
            "language" to language,
            "profile" to mapOf(
                "etymologyWeight" to profile.etymologyWeight,
                "storiesWeight" to profile.storiesWeight,
                "connectionsWeight" to profile.connectionsWeight,
                "formalWeight" to profile.formalWeight
            )
        )
        val bodyJson = gson.toJson(bodyMap)

        val request = Request.Builder()
            .url(backendUrl)
            .addHeader("content-type", "application/json")
            .post(bodyJson.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError(e.message ?: "Network error")
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    if (response.code == 429) {
                        onError("Daily query limit reached. Try again tomorrow.")
                    } else {
                        onError("HTTP ${response.code}: $errBody")
                    }
                    return
                }
                // Parse OpenAI-compatible SSE stream
                response.body?.source()?.use { source ->
                    while (!source.exhausted()) {
                        val line = source.readUtf8Line() ?: break
                        if (!line.startsWith("data: ")) continue
                        val data = line.removePrefix("data: ").trim()
                        if (data == "[DONE]") { onDone(); return }
                        try {
                            val json = JsonParser.parseString(data).asJsonObject
                            val token = json.getAsJsonArray("choices")
                                ?.get(0)?.asJsonObject
                                ?.getAsJsonObject("delta")
                                ?.get("content")?.asString
                            token?.let { onToken(it) }
                        } catch (_: Exception) {}
                    }
                    onDone()
                }
            }
        })
    }
}
