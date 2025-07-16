package com.pixesoj.freefirelike.utils

import android.content.Context
import android.content.pm.PackageManager

class AppUtils {
    companion object {
        fun getVersionName(context: Context): String? {
            try {
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                return pInfo.versionName
            } catch (e: PackageManager.NameNotFoundException) {
                return "N/A"
            }
        }

        fun getVersionCode(context: Context): Int {
            try {
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                return pInfo.versionCode
            } catch (_: PackageManager.NameNotFoundException) {
                return -1
            }
        }
    }
}