package com.pixesoj.freefirelike.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.gson.JsonElement
import com.pixesoj.freefirelike.manager.ApiManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HelperUtils {
    companion object {
        fun getStatusBarHeight(activity: Activity): Int {
            var result = 0
            @SuppressLint("InternalInsetResource")
            val resourceId = activity.resources.getIdentifier("status_bar_height", "dimen", "android")
            if (resourceId > 0) {
                result = activity.resources.getDimensionPixelSize(resourceId)
            }
            return result
        }

        fun getRegionName(regionCode: String): String {
            return when (regionCode.uppercase()) {
                "SAC", "LATAM", "MX" -> "Sudamérica"
                "NA", "NORTH_AMERICA" -> "Norteamérica"
                "IND", "IN" -> "India"
                "BR" -> "Brasil"
                "SG" -> "Singapur"
                "RU" -> "Rusia"
                "ID" -> "Indonesia"
                "TW" -> "Taiwán"
                "VN" -> "Vietnam"
                "TH" -> "Tailandia"
                "ME", "MEA" -> "Medio Oriente"
                "PK" -> "Pakistán"
                "CIS" -> "Comunidad de Estados Independientes"
                "BD" -> "Bangladés"
                "US" -> "EE.UU"
                else -> regionCode
            }
        }

        fun timestampToDateSafe(timestampStr: String): String {
            val TAG = "TimestampDebug"

            Log.d(TAG, "🟡 Timestamp recibido como String: $timestampStr")

            return try {
                val timestamp = timestampStr.toLongOrNull()

                if (timestamp == null) {
                    Log.e(TAG, "⛔ No se pudo convertir el timestamp a Long.")
                    return ""
                }

                if (timestamp <= 0L || timestamp > 9999999999L) {
                    Log.e(TAG, "⛔ Timestamp fuera de rango esperado: $timestamp")
                    return ""
                }

                val millis = timestamp * 1000L
                Log.d(TAG, "🔄 Convertido a milisegundos: $millis")

                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                val formattedDate = sdf.format(Date(millis))

                Log.d(TAG, "📅 Fecha formateada: $formattedDate")
                formattedDate
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error formateando timestamp", e)
                ""
            }
        }

        fun timestampToRelativeTimeSafe(timestampStr: String?): String {
            val TAG = "RelativeTime"

            return try {
                if (timestampStr.isNullOrEmpty()) return ""

                val timestamp = timestampStr.toLongOrNull() ?: return ""

                val now = System.currentTimeMillis()
                val timeMillis = timestamp * 1000
                val diff = now - timeMillis

                if (diff < 0) return "En el futuro"

                val seconds = diff / 1000
                val minutes = seconds / 60
                val hours = minutes / 60
                val days = hours / 24
                val weeks = days / 7
                val months = days / 30
                val years = days / 365

                when {
                    seconds < 60 -> "Hace unos segundos"
                    minutes < 60 -> "Hace $minutes minuto${if (minutes == 1L) "" else "s"}"
                    hours < 24 -> "Hace $hours hora${if (hours == 1L) "" else "s"}"
                    days < 7 -> "Hace $days día${if (days == 1L) "" else "s"}"
                    weeks < 4 -> "Hace $weeks semana${if (weeks == 1L) "" else "s"}"
                    months < 12 -> "Hace $months mes${if (months == 1L) "" else "es"}"
                    else -> "Hace $years año${if (years == 1L) "" else "s"}"
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error procesando timestamp: $timestampStr", e)
                ""
            }
        }

        fun mapRegionToGeneral(region: String): String {
            return when (region.uppercase()) {
                "BR", "EU", "SAC", "US", "ID", "TH", "VN", "TW" -> "BR"
                "RU", "CIS", "ME" -> "EUROPE"
                "IND", "PK", "BD", "SG" -> "IND"
                else -> "UNKNOWN"
            }
        }

        fun getLanguageName(code: String?): String {
            if (code.isNullOrBlank()) return "Desconocido"

            val upperCode = code.uppercase()

            val knownLanguages = mapOf(
                "LANGUAGE_SPANISH" to "Español",
                "LANGUAGE_ENGLISH" to "Inglés",
                "LANGUAGE_PORTUGUESE" to "Portugués",
                "LANGUAGE_FRENCH" to "Francés",
                "LANGUAGE_GERMAN" to "Alemán",
                "LANGUAGE_RUSSIAN" to "Ruso",
                "LANGUAGE_ARABIC" to "Árabe",
                "LANGUAGE_THAI" to "Tailandés",
                "LANGUAGE_INDONESIAN" to "Indonesio",
                "LANGUAGE_VIETNAMESE" to "Vietnamita",
                "LANGUAGE_HINDI" to "Hindi",
                "LANGUAGE_TURKISH" to "Turco",
                "LANGUAGE_CHINESE" to "Chino",
                "LANGUAGE_JAPANESE" to "Japonés",
                "LANGUAGE_KOREAN" to "Coreano",
                "LANGUAGE_MALAY" to "Malayo",
                "LANGUAGE_ITALIAN" to "Italiano",
                "LANGUAGE_POLISH" to "Polaco",
                "LANGUAGE_BENGALI" to "Bengalí",
                "LANGUAGE_TAMIL" to "Tamil",
                "LANGUAGE_URDU" to "Urdu"
            )

            return knownLanguages[upperCode] ?: run {
                val fallback = code.substringAfterLast("_").lowercase().replaceFirstChar { it.uppercaseChar() }
                fallback
            }
        }

        fun getGenderName(code: String?): String {
            if (code.isNullOrBlank()) return "Confidencial"

            return when (code.uppercase()) {
                "GENDER_MALE" -> "Masculino"
                "GENDER_FEMALE" -> "Femenino"
                else -> "Confidencial"
            }
        }

        fun getGameModeName(mode: String?): String {
            if (mode.isNullOrBlank()) return "Sin preferencia"

            return when (mode.uppercase()) {
                "MODEPREFER_BR" -> "Battle Royale"
                "MODEPREFER_DE" -> "Duelo de Escuadras"
                "MODEPREFER_ENTERTAINMENT" -> "Casuales"
                else -> "Sin preferencia"
            }
        }
    }
}