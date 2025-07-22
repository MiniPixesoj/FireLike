package com.pixesoj.freefirelike.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.pixesoj.freefirelike.R
import com.pixesoj.freefirelike.config.GlobalConfig
import com.pixesoj.freefirelike.manager.AccountManager
import com.pixesoj.freefirelike.manager.ApiManager
import com.pixesoj.freefirelike.manager.ApiManager.ApiCallback
import com.pixesoj.freefirelike.manager.TokenManager
import com.pixesoj.freefirelike.model.Account
import com.pixesoj.freefirelike.proto.LikeCount
import com.pixesoj.freefirelike.utils.CryptoUtils
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue

class NotificationWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val context = appContext
    private var autoLikesAccounts: MutableList<Account>? = null
    private val tokenManager = TokenManager(context, true)

    override suspend fun doWork(): Result {
        autoLikesAccounts = AccountManager.getAutoLikeList()
        if (autoLikesAccounts.isNullOrEmpty()) return Result.success()

        val summaryLines = mutableListOf<String>()

        for (account in autoLikesAccounts!!) {
            try {
                val nowTimestamp = System.currentTimeMillis() / 1000
                val checkTimeUrl = GlobalConfig.API_BASE_URL + "check-time/${account.uid}/$nowTimestamp"
                val checkResponse = apiGetSuspend(checkTimeUrl)
                val passed = checkResponse?.asJsonObject?.get("passed_time")?.asBoolean == true

                if (passed) {
                    sendNotificationWithProgress(account, "Obteniendo tokens del servidor...")

                    val tokens = tokenManager.getTokens("BR")
                    if (tokens.isEmpty()) {
                        sendNotification(account, "No se pudieron obtener los tokens del servidor")
                        summaryLines.add("✗ ${account.username}: Error al obtener tokens")
                        continue
                    }

                    sendNotificationWithProgress(account, "Enviando likes...", "Procesando solicitud para ${account.username}")
                    val url = GlobalConfig.API_INFO_URL + "info?uid=${account.uid}"
                    val jsonBefore = apiGetSuspend(url)

                    val basicInfoBefore = jsonBefore?.asJsonObject?.getAsJsonObject("basicInfo")
                    if (basicInfoBefore == null) {
                        summaryLines.add("✗ ${account.username}: Error al leer likes previos")
                        continue
                    }
                    val likesBefore = basicInfoBefore.getSafeInt("liked")

                    val (_, _) = sendLikesSuspend(
                        account.uid,
                        tokens,
                        100,
                        onProgress = { _, _ -> },
                        sendProgressNotification = { current, total, message ->
                            sendNotificationWithProgress(
                                account,
                                title = "Enviando likes a ${account.username}...",
                                message = message,
                                isIndeterminate = false,
                                progress = current,
                                maxProgress = total
                            )
                        }
                    )

                    val jsonAfter = apiGetSuspend(url)
                    val basicInfoAfter = jsonAfter?.asJsonObject?.getAsJsonObject("basicInfo")
                    if (basicInfoAfter == null) {
                        summaryLines.add("✗ ${account.username}: Error al leer likes finales")
                        continue
                    }
                    val liked = basicInfoAfter.getSafeInt("liked")

                    val likesAdded = liked - likesBefore

                    ApiManager.get(context, GlobalConfig.API_BASE_URL + "add-likes/${account.uid}/$likesAdded/" + (System.currentTimeMillis() / 1000).toString(), null, 15, 1, object : ApiCallback {
                        override fun onSuccess(responseBody: String?, responseElement: JsonElement?) {
                            responseElement?.asJsonObject?.let { json ->
                                val status = json.get("status").asString
                                if (status == "ok") {
                                    sendNotification(
                                        account,
                                        message = "$likesAdded likes agregados a ${account.username}",
                                        title = "¡Likes enviados exitosamente!",
                                        lines = listOf(
                                            "Nickname: ${account.username}",
                                            "Likes agregados: $likesAdded",
                                            "Likes antes: $likesBefore",
                                            "Likes ahora: $liked"
                                        ),
                                        summary = "UID: ${account.uid}"
                                    )

                                    summaryLines.add("✓ ${account.username}: +$likesAdded likes")
                                }
                            }
                        }

                        override fun onError(e: java.lang.Exception?) { }
                    })
                }
            } catch (_: Exception) {
                summaryLines.add("✗ ${account.username}: Error inesperado")
            }
        }

        if (summaryLines.isNotEmpty()) {
            sendNotification(
                account = null,
                title = "Resumen AutoLike",
                message = "Proceso finalizado para ${summaryLines.size} cuentas.",
                lines = summaryLines,
                summary = "Free Fire Info"
            )
        }

        return Result.success()
    }

    private fun sendNotification(
        account: Account?,
        message: String,
        title: String = "Free Fire AutoLike Info",
        lines: List<String>? = null,
        summary: String? = null,
        actionText: String? = null,
        actionIcon: Int? = null,
        actionIntent: PendingIntent? = null
    ) {
        val context = applicationContext
        val canalId = "FF_AUTO_LIKE_INFO"
        val notifyID = generateNotificationId(account?.uid ?: "1")

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                canalId,
                "Free Fire Info",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            canal.description = "Información sobre AutoLike"
            notificationManager.createNotificationChannel(canal)
        }

        val builder = NotificationCompat.Builder(context, canalId)
            .setSmallIcon(R.drawable.ic_like)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        if (!lines.isNullOrEmpty()) {
            val inboxStyle = NotificationCompat.InboxStyle()
                .setBigContentTitle(title)
            lines.forEach { line -> inboxStyle.addLine(line) }
            summary?.let { inboxStyle.setSummaryText(it) }
            builder.setStyle(inboxStyle)
        }

        if (actionText != null && actionIcon != null && actionIntent != null) {
            builder.addAction(actionIcon, actionText, actionIntent as PendingIntent?)
        }

        notificationManager.notify(notifyID, builder.build())
    }

    private fun sendNotificationWithProgress(
        account: Account?,
        title: String = "Enviando Likes...",
        message: String = "Procesando solicitud",
        isIndeterminate: Boolean = true,
        progress: Int = 0,
        maxProgress: Int = 100
    ) {
        val context = applicationContext
        val canalId = "FF_AUTO_LIKE_INFO"
        val notifyID = generateNotificationId(account?.uid ?: "1")

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                canalId,
                "Free Fire Info",
                NotificationManager.IMPORTANCE_LOW
            )
            canal.description = "Información sobre AutoLike"
            notificationManager.createNotificationChannel(canal)
        }

        val builder = NotificationCompat.Builder(context, canalId)
            .setSmallIcon(R.drawable.ic_like)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)

        if (isIndeterminate) {
            builder.setProgress(0, 0, true)
        } else {
            builder.setProgress(maxProgress, progress, false)
        }

        notificationManager.notify(notifyID, builder.build())
    }

    suspend fun apiGetSuspend(
        url: String,
        headers: Map<String, String>? = null,
        timeout: Long = 15,
        retries: Int = 1
    ): JsonElement? = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .callTimeout(timeout, TimeUnit.SECONDS)
            .build()

        val requestBuilder = Request.Builder().url(url)
        headers?.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }

        var attempt = 0
        var lastException: Exception? = null

        while (attempt < retries) {
            try {
                val response = client.newCall(requestBuilder.build()).execute()
                val body = response.body?.string()
                response.close()
                if (!body.isNullOrEmpty()) {
                    return@withContext JsonParser().parse(body)
                }
            } catch (e: Exception) {
                lastException = e
            }
            attempt++
        }

        return@withContext null
    }

    suspend fun sendLikesSuspend(
        uid: String,
        tokens: List<String>,
        targetAmount: Int?,
        onProgress: (current: Int, total: Int) -> Unit,
        sendProgressNotification: (progress: Int, max: Int, message: String) -> Unit
    ): Pair<Int, Int> = withContext(Dispatchers.IO) {
        var added = 0
        var sent = 0

        if (tokens.isEmpty()) return@withContext Pair(0, 0)

        val protobuf = LikeCount.Request.newBuilder()
            .setUid(uid.toLong())
            .setType(1)
            .build()
            .toByteArray()

        val encryptedPayload = CryptoUtils.encryptAES(protobuf)
        val loopTokens = if (targetAmount == null) tokens else tokens.take(targetAmount)
        val total = loopTokens.size

        loopTokens.forEachIndexed { index, token ->
            try {
                val request = Request.Builder()
                    .url(GlobalConfig.API_FF_CLIENT_URL + "LikeProfile")
                    .post(encryptedPayload.toRequestBody("application/x-www-form-urlencoded".toMediaTypeOrNull()))
                    .header("User-Agent", "Dalvik/2.1.0 (Linux; U; Android 9; ASUS_Z01QD Build/PI)")
                    .header("Connection", "Keep-Alive")
                    .header("Accept-Encoding", "gzip")
                    .header("Authorization", "Bearer $token")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("X-Unity-Version", "2018.4.11f1")
                    .header("X-GA", "v1 1")
                    .header("ReleaseVersion", "OB49")
                    .build()

                val response = OkHttpClient().newCall(request).execute()
                if (response.isSuccessful) added++
                response.close()
            } catch (_: Exception) {
                // fall silently
            }

            sent++
            onProgress(sent, total)
            sendProgressNotification(sent, total, "Enviando likes... ($sent/$total)")
        }

        return@withContext Pair(sent, added)
    }

    fun generateNotificationId(uid: String): Int {
        return uid.hashCode().absoluteValue
    }

    fun JsonObject?.getSafeInt(key: String): Int {
        return this?.get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asInt ?: 0
    }
}