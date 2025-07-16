package com.pixesoj.freefirelike.manager

import android.content.Context
import android.util.Log
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

class TokenManager(context: Context, private val useRedis: Boolean = true) {

    private val authUrl = "https://jwtxthug.up.railway.app/token"
    private val cacheDuration = 7 * 60 * 60 // 7 horas en segundos

    private val redisBaseUrl = "https://quick-doe-23866.upstash.io"
    private val redisToken = "Bearer AV06AAIjcDFkNzE5MTUxNzM0ZTM0YmQ1OTIyN2M0ZjU5ZjBiNzVhZXAxMA"

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

        // ✅ 1. Primero intentamos leer desde cache local (bulk)
        val localCache = getFromPrefs(cacheKey)
        if (localCache != null) {
            val (tokensJson, timestamp) = localCache
            if (now - timestamp < cacheDuration) {
                val jsonArray = JSONArray(tokensJson)
                val tokens = (0 until jsonArray.length()).map { jsonArray.getString(it) }
                Log.d("TokenManager", "🔁 Tokens obtenidos desde SharedPrefs")
                return@withContext tokens
            } else {
                Log.d("TokenManager", "⏰ Cache local expiró, se usará Redis o fetch")
            }
        }

        val tokens = mutableListOf<String>()
        var oldestTimestamp = Long.MAX_VALUE

        // ✅ 2. Leer cada token desde Redis (u obtener nuevo si no existe o expiró)
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

            // Token no válido o no existe, obtener uno nuevo
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

        // ✅ 3. Guardar todos los tokens en una entrada local (SharedPrefs)
        if (tokens.isNotEmpty() && oldestTimestamp != Long.MAX_VALUE) {
            val arrayJson = JSONArray(tokens)
            setToPrefs(cacheKey, arrayJson.toString(), oldestTimestamp)
        }

        return@withContext tokens
    }

    suspend fun getOneToken(serverKey: String): String? = withContext(Dispatchers.IO) {
        val users = ServerConfigs.SERVERS[serverKey.uppercase()] ?: return@withContext null
        for (user in users) {
            val token = getTokenForUser(user, serverKey)
            if (!token.isNullOrEmpty()) return@withContext token
        }
        return@withContext null
    }

    private fun getTokenForUser(user: UserCredential, serverKey: String): String? {
        val now = System.currentTimeMillis() / 1000
        val key = "${serverKey}_${user.uid}"

        val cached: Pair<String, Long>? = if (useRedis) getFromRedis(key) else getFromPrefs(key)

        cached?.let { (token, timestamp) ->
            if (now - timestamp < cacheDuration && token.isNotEmpty()) {
                Log.d("TokenManager", "🔁 Token válido desde ${if (useRedis) "Redis" else "Prefs"} para ${user.uid}")
                return token
            }
        }

        val newToken = fetchNewToken(user)
        if (!newToken.isNullOrEmpty()) {
            if (useRedis) setToRedis(key, newToken, now)
            else setToPrefs(key, newToken, now)
        }

        return newToken
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
                        Log.i("TokenManager", "✅ Nuevo token para ${user.uid}")
                        return token
                    } else {
                        Log.w("TokenManager", "⚠️ Token vacío para ${user.uid}")
                    }
                } else {
                    Log.w("TokenManager", "⛔ Error ${response.code} para ${user.uid}")
                }
            } catch (e: Exception) {
                Log.e("TokenManager", "❌ Error al obtener token para ${user.uid}: $e")
            }
        }
        return null
    }

    // ----- Redis Storage -----

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
            Log.d("Redis", "📥 Respuesta bruta para $key: $bodyStr")

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
            Log.e("Redis", "❌ Error en get Redis: $e")
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

            client.newCall(request).execute().use { res ->
                if (res.isSuccessful) {
                    Log.d("Redis", "💾 Token guardado en Redis: $key")
                } else {
                    Log.w("Redis", "SET fallido (${res.code}) para $key")
                }
            }
        } catch (e: Exception) {
            Log.e("Redis", "❌ Error en set Redis: $e")
        }
    }

    // ----- SharedPreferences Storage -----

    private fun getFromPrefs(key: String): Pair<String, Long>? {
        return try {
            val jsonStr = sharedPrefs.getString(key, null) ?: return null
            val json = JSONObject(jsonStr)
            val value = json.optString("value", "")
            val timestamp = json.optLong("timestamp", 0)
            if (value.isNotEmpty() && timestamp > 0) Pair(value, timestamp) else null
        } catch (e: Exception) {
            Log.e("Prefs", "⚠️ Error leyendo SharedPreferences: $e")
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
            Log.d("Prefs", "💾 Datos guardados en SharedPreferences: $key")
        } catch (e: Exception) {
            Log.e("Prefs", "⚠️ Error guardando en SharedPreferences: $e")
        }
    }
}