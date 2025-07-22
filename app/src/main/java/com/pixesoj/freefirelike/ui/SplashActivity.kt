package com.pixesoj.freefirelike.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.JsonElement
import com.pixesoj.freefirelike.R
import com.pixesoj.freefirelike.config.AppConfig
import com.pixesoj.freefirelike.config.GlobalConfig
import com.pixesoj.freefirelike.manager.ApiManager
import com.pixesoj.freefirelike.manager.ApiManager.ApiCallback
import com.pixesoj.freefirelike.utils.AppUtils

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    var activity: Activity = this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        ApiManager.get(this, GlobalConfig.API_BASE_URL + "config", null, 15, 1, object : ApiCallback {
            override fun onSuccess(responseBody: String?, responseElement: JsonElement?) {
                responseElement?.asJsonObject?.let { json ->
                    val status = json.get("status")?.asString ?: "error"
                    if (status == "ok") {
                        val config = json.getAsJsonObject("config")
                        val version = config?.get("app_version")?.asString ?: "unknown"
                        val code = config?.get("app_code")?.asInt ?: AppUtils.getVersionCode(activity)
                        val downloadUrl = config?.get("app_download_url")?.asString ?: "null"

                        AppConfig.setUpdateAvailable(code > AppUtils.getVersionCode(activity))
                        AppConfig.setUpdateVersionName(version)
                        AppConfig.setUpdateUrl(downloadUrl)

                        goNext()
                    } else {
                        goNext()
                    }
                } ?: goNext()
            }

            override fun onError(e: Exception?) { goNext() }
            override fun onNoInternet(context: Context) { goNext() }
            override fun onTimeout(context: Context) { goNext() }
        })
    }


    private fun goNext(){
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}