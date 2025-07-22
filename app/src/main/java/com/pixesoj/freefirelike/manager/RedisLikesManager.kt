package com.pixesoj.freefirelike.manager

import android.util.Log
import com.pixesoj.freefirelike.config.GlobalConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object RedisLikesManager {

    private const val redisBaseUrl = GlobalConfig.REDIS_URL
    private const val redisToken = "Bearer ${GlobalConfig.REDIS_KEY}"
    private val client = OkHttpClient()
    private val jsonMedia = "application/json".toMediaType()

    private fun keyToday(uid: String) = "likes_today_$uid"
    private fun keyDate(uid: String) = "likes_today_date_$uid"
    private fun keyAll(uid: String) = "all_likes_$uid"

    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }

    suspend fun getLikesToday(uid: String): Int = withContext(Dispatchers.IO) {
        val today = getCurrentDate()
        val storedDate = getFromRedis(keyDate(uid))

        return@withContext if (storedDate == today) {
            getFromRedis(keyToday(uid)).toIntOrNull() ?: 0
        } else {
            setToRedis(keyDate(uid), today)
            setToRedis(keyToday(uid), "0")
            0
        }
    }

    suspend fun addLikeToday(uid: String, amount: Int = 1) = withContext(Dispatchers.IO) {
        val today = getCurrentDate()
        val storedDate = getFromRedis(keyDate(uid))

        if (storedDate != today) {
            setToRedis(keyDate(uid), today)
            setToRedis(keyToday(uid), amount.toString())
        } else {
            val current = getFromRedis(keyToday(uid)).toIntOrNull() ?: 0
            setToRedis(keyToday(uid), (current + amount).toString())
        }
    }

    suspend fun getAllLikes(uid: String): Int = withContext(Dispatchers.IO) {
        return@withContext getFromRedis(keyAll(uid)).toIntOrNull() ?: 0
    }

    suspend fun addAllLikes(uid: String, amount: Int = 1) = withContext(Dispatchers.IO) {
        val current = getFromRedis(keyAll(uid)).toIntOrNull() ?: 0
        setToRedis(keyAll(uid), (current + amount).toString())
    }

    private fun getFromRedis(key: String): String {
        return try {
            val url = "$redisBaseUrl/get/$key"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", redisToken)
                .get()
                .build()

            val response = client.newCall(request).execute()
            val raw = response.body?.string()
            if (!response.isSuccessful || raw.isNullOrEmpty() || raw == "null") return ""

            val json = JSONObject(raw)
            json.optString("result") ?: ""
        } catch (e: Exception) {
            Log.e("RedisLikes", "❌ Error getFromRedis($key): $e")
            ""
        }
    }

    private fun setToRedis(key: String, value: String) {
        try {
            val body = JSONObject().put("value", value).toString().toRequestBody(jsonMedia)
            val request = Request.Builder()
                .url("$redisBaseUrl/set/$key")
                .addHeader("Authorization", redisToken)
                .post(body)
                .build()

            client.newCall(request).execute().use { res ->
                if (res.isSuccessful) {
                    Log.d("RedisLikes", "✅ Guardado $key = $value")
                } else {
                    Log.w("RedisLikes", "⚠️ Fallo al guardar $key (${res.code})")
                }
            }
        } catch (e: Exception) {
            Log.e("RedisLikes", "❌ Error setToRedis($key): $e")
        }
    }
}