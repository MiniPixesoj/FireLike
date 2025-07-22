package com.pixesoj.freefirelike.manager

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONObject
import com.pixesoj.freefirelike.config.ServerConfigs
import com.pixesoj.freefirelike.config.UserCredential
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import androidx.core.content.edit
import com.pixesoj.freefirelike.config.GlobalConfig

class TokenManager(context: Context, private val useRedis: Boolean = true) {

    private val authUrl = GlobalConfig.API_TOKENS2_URL
    private val cacheDuration = 7 * 60 * 60

    private val redisBaseUrl = GlobalConfig.REDIS_URL
    private val redisToken = "Bearer ${GlobalConfig.REDIS_KEY}"

    private val client = OkHttpClient()
    private val jsonMedia = "application/json".toMediaType()
    private val sharedPrefs = context.getSharedPreferences("token_cache", Context.MODE_PRIVATE)

    suspend fun getTokens(
        serverKey: String,
        onTokenFetched: ((user: UserCredential, token: String) -> Unit)? = null
    ): List<String> = withContext(Dispatchers.IO) {
        val users = ServerConfigs.SERVERS[serverKey.uppercase()] ?: return@withContext emptyList()

        val now = System.currentTimeMillis() / 1000
        val cacheKey = "${serverKey}_BULK"
        val localCache = getFromPrefs(cacheKey)
        if (localCache != null) {
            val (tokensJson, timestamp) = localCache
            if (now - timestamp < cacheDuration) {
                val jsonArray = JSONArray(tokensJson)
                val tokens = (0 until jsonArray.length()).map { jsonArray.getString(it) }
                return@withContext tokens
            }
        }

        val tokens = mutableListOf<String>()
        var oldestTimestamp = Long.MAX_VALUE

        for (user in users) {
            val key = "${serverKey}_${user.uid}"
            val tokenData = if (useRedis) getFromRedis(key) else getFromPrefs(key)

            if (tokenData != null) {
                val (token, timestamp) = tokenData
                if (now - timestamp < cacheDuration && token.isNotEmpty()) {
                    tokens.add(token)
                    oldestTimestamp = minOf(oldestTimestamp, timestamp)
                    onTokenFetched?.invoke(user, token)
                    continue
                }
            }

            val newToken = fetchNewToken(user)
            if (!newToken.isNullOrEmpty()) {
                val timestamp = System.currentTimeMillis() / 1000
                tokens.add(newToken)
                oldestTimestamp = minOf(oldestTimestamp, timestamp)
                if (useRedis) setToRedis(key, newToken, timestamp)
                else setToPrefs(key, newToken, timestamp)
                onTokenFetched?.invoke(user, newToken)
            }
        }

        if (tokens.isNotEmpty() && oldestTimestamp != Long.MAX_VALUE) {
            val arrayJson = JSONArray(tokens)
            setToPrefs(cacheKey, arrayJson.toString(), oldestTimestamp)
        }

        return@withContext tokens
    }

    private fun fetchNewToken(user: UserCredential): String? {
        repeat(3) { attempt ->
            try {
                val url = authUrl.toHttpUrlOrNull()?.newBuilder()
                    ?.addQueryParameter("uid", user.uid)
                    ?.addQueryParameter("password", user.password)
                    ?.build() ?: return null

                val request = Request.Builder().url(url).get().build()
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val body = response.body?.use { it.string() } ?: return null
                    val json = JSONObject(body)
                    val token = json.optString("token")

                    if (token.isNotEmpty()) {
                        return token
                    }
                }
            } catch (_: Exception) { }
        }
        return null
    }

    private fun getFromRedis(key: String): Pair<String, Long>? {
        return try {
            val url = "$redisBaseUrl/get/$key"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", redisToken)
                .get()
                .build()

            val response = client.newCall(request).execute()
            val bodyStr = response.body?.string()

            if (!response.isSuccessful || bodyStr.isNullOrBlank() || bodyStr == "null") return null

            val json = JSONObject(bodyStr)
            val result = json.optString("result")

            if (result.isNotEmpty() && result != "null") {
                val resultJson = JSONObject(result)
                val value = resultJson.optJSONObject("value") ?: return null
                val token = value.optString("token", "")
                val timestamp = value.optLong("timestamp", 0)
                if (token.isNotEmpty() && timestamp > 0) Pair(token, timestamp) else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun setToRedis(key: String, token: String, timestamp: Long) {
        try {
            val valueJson = JSONObject().apply {
                put("token", token)
                put("timestamp", timestamp)
            }
            val fullBody = JSONObject().apply {
                put("value", valueJson)
            }.toString().toRequestBody(jsonMedia)

            val request = Request.Builder()
                .url("$redisBaseUrl/set/$key")
                .addHeader("Authorization", redisToken)
                .post(fullBody)
                .build()

            client.newCall(request).execute().use { res -> }
        } catch (_: Exception) { }
    }

    private fun getFromPrefs(key: String): Pair<String, Long>? {
        return try {
            val jsonStr = sharedPrefs.getString(key, null) ?: return null
            val json = JSONObject(jsonStr)
            val value = json.optString("value", "")
            val timestamp = json.optLong("timestamp", 0)
            if (value.isNotEmpty() && timestamp > 0) Pair(value, timestamp) else null
        } catch (e: Exception) {
            null
        }
    }

    private fun setToPrefs(key: String, value: String, timestamp: Long) {
        try {
            val json = JSONObject().apply {
                put("value", value)
                put("timestamp", timestamp)
            }
            sharedPrefs.edit { putString(key, json.toString()) }
        } catch (_: Exception) { }
    }
}