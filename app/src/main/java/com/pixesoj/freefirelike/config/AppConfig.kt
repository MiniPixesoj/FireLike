package com.pixesoj.freefirelike.config

import com.orhanobut.hawk.Hawk

class AppConfig {
    companion object {
        fun isEnabledNotifications(): Boolean {
            return Hawk.get<Boolean?>("APP_CONFIG_NOTIFICATIONS", true)
        }

        fun setEnabledNotifications(value: Boolean) {
            Hawk.put<Boolean?>("APP_CONFIG_NOTIFICATIONS", value)
        }

        fun getUpdateVersionName(): String? {
            return Hawk.get<String?>("APP_VALUE_UPDATE_VERSION_NAME", "")
        }

        fun setUpdateVersionName(value: String?) {
            Hawk.put<String?>("APP_VALUE_UPDATE_VERSION_NAME", value)
        }

        fun getUpdateUrl(): String? {
            return Hawk.get<String?>("APP_VALUE_UPDATE_URL", "")
        }

        fun setUpdateUrl(value: String?) {
            Hawk.put<String?>("APP_VALUE_UPDATE_URL", value)
        }

        fun isUpdateAvailable(): Boolean {
            return Hawk.get<Boolean?>("APP_VALUE_UPDATE_AVAILABLE", false)
        }

        fun setUpdateAvailable(value: Boolean) {
            Hawk.put<Boolean?>("APP_VALUE_UPDATE_AVAILABLE", value)
        }
    }
}